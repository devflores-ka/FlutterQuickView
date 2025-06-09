package com.github.devflores_ka.flutterquickview.generator;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resuelve dinámicamente las dependencias compatibles con el proyecto actual
 * Evita conflictos de versiones analizando el pubspec.yaml original
 */
public class DynamicDependencyResolver {
    private static final Logger LOG = Logger.getInstance(DynamicDependencyResolver.class);

    private final Project project;
    private final Map<String, String> projectDependencies;
    private final Map<String, String> projectDevDependencies;
    private final String flutterVersion;
    private final String dartSdkConstraint;

    public DynamicDependencyResolver(Project project) {
        this.project = project;
        this.projectDependencies = new HashMap<>();
        this.projectDevDependencies = new HashMap<>();

        // Analizar el proyecto actual
        PubspecAnalysis analysis = analyzePubspec();
        this.flutterVersion = analysis.flutterVersion;
        this.dartSdkConstraint = analysis.dartSdkConstraint;

        LOG.info("🔍 Dependencias del proyecto analizadas:");
        LOG.info("  - Dependencies: " + projectDependencies.size());
        LOG.info("  - Dev Dependencies: " + projectDevDependencies.size());
        LOG.info("  - Flutter version: " + flutterVersion);
        LOG.info("  - Dart SDK: " + dartSdkConstraint);
    }

    /**
     * Genera un pubspec.yaml compatible con el proyecto actual
     */
    public String generateCompatiblePubspec(String tempProjectName, MobilePreviewGenerator.MobileDevice device) {
        StringBuilder pubspec = new StringBuilder();

        // Header básico
        pubspec.append("name: ").append(tempProjectName).append("\n");
        pubspec.append("description: Compatible mobile preview for ").append(device.platform).append("\n");
        pubspec.append("version: 1.0.0+1\n");
        pubspec.append("publish_to: 'none'\n\n");

        // Environment compatible
        pubspec.append("environment:\n");
        pubspec.append("  sdk: '").append(dartSdkConstraint).append("'\n");
        if (!flutterVersion.isEmpty()) {
            pubspec.append("  flutter: \"").append(flutterVersion).append("\"\n");
        }
        pubspec.append("\n");

        // Dependencies compatibles
        pubspec.append("dependencies:\n");
        pubspec.append("  flutter:\n");
        pubspec.append("    sdk: flutter\n");

        // Agregar dependencias esenciales del proyecto original
        addCompatibleDependencies(pubspec, projectDependencies);

        // Agregar dependencias específicas del dispositivo (solo si no hay conflicto)
        addDeviceSpecificDependencies(pubspec, device);

        pubspec.append("\n");

        // Dev dependencies compatibles
        pubspec.append("dev_dependencies:\n");
        pubspec.append("  flutter_test:\n");
        pubspec.append("    sdk: flutter\n");

        // Agregar dev dependencies del proyecto sin conflictos
        addCompatibleDevDependencies(pubspec, projectDevDependencies);

        pubspec.append("\n");

        // Flutter config básico
        pubspec.append("flutter:\n");
        pubspec.append("  uses-material-design: true\n");

        return pubspec.toString();
    }

    /**
     * Agrega dependencias compatibles evitando conflictos de versiones
     */
    private void addCompatibleDependencies(StringBuilder pubspec, Map<String, String> dependencies) {
        // Lista de dependencias seguras que raramente causan conflictos
        Set<String> safeDependencies = Set.of(
                "cupertino_icons",
                "provider",
                "http",
                "shared_preferences",
                "path_provider",
                "url_launcher",
                "image_picker",
                "cached_network_image"
        );

        dependencies.forEach((name, version) -> {
            if (safeDependencies.contains(name)) {
                pubspec.append("  ").append(name).append(": ").append(version).append("\n");
                LOG.debug("✅ Agregada dependencia segura: " + name + ": " + version);
            } else {
                LOG.debug("⚠️ Dependencia omitida (potencial conflicto): " + name);
            }
        });
    }

    /**
     * Agrega dependencias específicas del dispositivo si no hay conflictos
     */
    private void addDeviceSpecificDependencies(StringBuilder pubspec, MobilePreviewGenerator.MobileDevice device) {
        // En lugar de agregar material_color_utilities específico, usar lo que ya existe
        if (device.platform.equals("iOS")) {
            // Para iOS, solo agregar si no existe
            if (!projectDependencies.containsKey("cupertino_icons")) {
                pubspec.append("  cupertino_icons: ^1.0.6\n");
            }
        } else {
            // Para Android, no forzar material_color_utilities
            // Dejar que Flutter maneje las dependencias Material automáticamente
            LOG.info("📱 Android device - usando dependencias Material por defecto");
        }
    }

    /**
     * Agrega dev dependencies compatibles
     */
    private void addCompatibleDevDependencies(StringBuilder pubspec, Map<String, String> devDependencies) {
        // Lista blanca de dev dependencies que raramente causan problemas
        Set<String> safeDevDependencies = Set.of(
                "flutter_lints",
                "build_runner",
                "json_annotation",
                "mockito",
                "test"
        );

        devDependencies.forEach((name, version) -> {
            if (safeDevDependencies.contains(name)) {
                pubspec.append("  ").append(name).append(": ").append(version).append("\n");
                LOG.debug("✅ Agregada dev dependency segura: " + name + ": " + version);
            }
        });

        // Agregar flutter_lints por defecto si no existe (es estándar)
        if (!devDependencies.containsKey("flutter_lints")) {
            pubspec.append("  flutter_lints: ^3.0.0\n");
        }
    }

    /**
     * Analiza el pubspec.yaml del proyecto actual
     */
    private PubspecAnalysis analyzePubspec() {
        try {
            VirtualFile baseDir = project.getBaseDir();
            if (baseDir == null) {
                LOG.warn("No se pudo obtener directorio base del proyecto");
                return new PubspecAnalysis();
            }

            VirtualFile pubspecFile = baseDir.findChild("pubspec.yaml");
            if (pubspecFile == null || !pubspecFile.exists()) {
                LOG.warn("No se encontró pubspec.yaml en el proyecto");
                return new PubspecAnalysis();
            }

            String content = new String(pubspecFile.contentsToByteArray());
            LOG.info("📄 Analizando pubspec.yaml del proyecto (" + content.length() + " caracteres)");

            return parsePubspecContent(content);

        } catch (Exception e) {
            LOG.error("Error analizando pubspec.yaml del proyecto", e);
            return new PubspecAnalysis();
        }
    }

    /**
     * Parsea el contenido del pubspec.yaml
     */
    private PubspecAnalysis parsePubspecContent(String content) {
        PubspecAnalysis analysis = new PubspecAnalysis();

        try {
            // Extraer environment
            Pattern envPattern = Pattern.compile("environment:\\s*\\n((?:\\s+[^\\n]+\\n)*)", Pattern.MULTILINE);
            Matcher envMatcher = envPattern.matcher(content);
            if (envMatcher.find()) {
                String envSection = envMatcher.group(1);

                // Extraer SDK constraint
                Pattern sdkPattern = Pattern.compile("sdk:\\s*['\"]([^'\"]+)['\"]");
                Matcher sdkMatcher = sdkPattern.matcher(envSection);
                if (sdkMatcher.find()) {
                    analysis.dartSdkConstraint = sdkMatcher.group(1);
                }

                // Extraer Flutter version
                Pattern flutterPattern = Pattern.compile("flutter:\\s*['\"]([^'\"]+)['\"]");
                Matcher flutterMatcher = flutterPattern.matcher(envSection);
                if (flutterMatcher.find()) {
                    analysis.flutterVersion = flutterMatcher.group(1);
                }
            }

            // Extraer dependencies
            Pattern depsPattern = Pattern.compile("dependencies:\\s*\\n((?:\\s+[^\\n]+\\n)*?)(?=\\n\\w|\\z)", Pattern.MULTILINE | Pattern.DOTALL);
            Matcher depsMatcher = depsPattern.matcher(content);
            if (depsMatcher.find()) {
                String depsSection = depsMatcher.group(1);
                parseDependenciesSection(depsSection, projectDependencies);
            }

            // Extraer dev_dependencies
            Pattern devDepsPattern = Pattern.compile("dev_dependencies:\\s*\\n((?:\\s+[^\\n]+\\n)*?)(?=\\n\\w|\\z)", Pattern.MULTILINE | Pattern.DOTALL);
            Matcher devDepsMatcher = devDepsPattern.matcher(content);
            if (devDepsMatcher.find()) {
                String devDepsSection = devDepsMatcher.group(1);
                parseDependenciesSection(devDepsSection, projectDevDependencies);
            }

            LOG.info("✅ Pubspec parseado exitosamente");

        } catch (Exception e) {
            LOG.error("Error parseando pubspec content", e);
        }

        return analysis;
    }

    /**
     * Parsea una sección de dependencias
     */
    private void parseDependenciesSection(String section, Map<String, String> targetMap) {
        String[] lines = section.split("\n");

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;

            // Detectar dependencias simples: "nombre: version"
            Pattern simplePattern = Pattern.compile("([\\w_]+):\\s*([^\\n]+)");
            Matcher simpleMatcher = simplePattern.matcher(line);

            if (simpleMatcher.find()) {
                String name = simpleMatcher.group(1);
                String version = simpleMatcher.group(2).trim();

                // Saltar dependencias de SDK
                if (!name.equals("flutter") && !name.equals("flutter_test")) {
                    targetMap.put(name, version);
                    LOG.debug("📦 Dependencia encontrada: " + name + " -> " + version);
                }
            }
        }
    }

    /**
     * Verifica si una dependencia específica causaría conflictos
     */
    public boolean wouldCauseConflict(String dependencyName, String desiredVersion) {
        if (!projectDependencies.containsKey(dependencyName) && !projectDevDependencies.containsKey(dependencyName)) {
            return false; // No hay conflicto si no existe en el proyecto
        }

        String existingVersion = projectDependencies.getOrDefault(dependencyName,
                projectDevDependencies.get(dependencyName));

        // Verificación simple de compatibilidad de versiones
        return !areVersionsCompatible(existingVersion, desiredVersion);
    }

    /**
     * Verifica compatibilidad básica entre versiones
     */
    private boolean areVersionsCompatible(String existing, String desired) {
        // Lógica simplificada - en un caso real usarías una librería de versionado
        if (existing.equals(desired)) return true;

        // Si ambas usan caret (^), verificar compatibilidad mayor
        if (existing.startsWith("^") && desired.startsWith("^")) {
            String existingMajor = extractMajorVersion(existing.substring(1));
            String desiredMajor = extractMajorVersion(desired.substring(1));
            return existingMajor.equals(desiredMajor);
        }

        return false;
    }

    private String extractMajorVersion(String version) {
        return version.split("\\.")[0];
    }

    /**
     * Genera un pubspec.yaml minimalista para máxima compatibilidad
     */
    public String generateMinimalPubspec(String tempProjectName) {
        return String.format("""
            name: %s
            description: Minimal compatible Flutter project for previews
            version: 1.0.0+1
            publish_to: 'none'
            
            environment:
              sdk: '%s'
              %s
              
            dependencies:
              flutter:
                sdk: flutter
              cupertino_icons: ^1.0.6
              
            dev_dependencies:
              flutter_test:
                sdk: flutter
              flutter_lints: ^3.0.0
                
            flutter:
              uses-material-design: true
            """,
                tempProjectName,
                dartSdkConstraint.isEmpty() ? ">=3.0.0 <4.0.0" : dartSdkConstraint,
                flutterVersion.isEmpty() ? "" : "flutter: \"" + flutterVersion + "\""
        );
    }

    /**
     * Clase para encapsular el análisis del pubspec
     */
    private static class PubspecAnalysis {
        String dartSdkConstraint = ">=3.0.0 <4.0.0";
        String flutterVersion = "";
    }

    /**
     * Obtiene estadísticas del análisis
     */
    public String getAnalysisStats() {
        return String.format(
                "Dependencies: %d, Dev Dependencies: %d, Flutter: %s, Dart: %s",
                projectDependencies.size(),
                projectDevDependencies.size(),
                flutterVersion.isEmpty() ? "default" : flutterVersion,
                dartSdkConstraint
        );
    }
}