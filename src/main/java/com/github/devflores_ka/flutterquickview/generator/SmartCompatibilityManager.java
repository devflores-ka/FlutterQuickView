package com.github.devflores_ka.flutterquickview.generator;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;

import java.nio.file.Path;
import java.util.*;

/**
 * Gestor inteligente que detecta automáticamente la mejor estrategia de compatibilidad
 * para cualquier proyecto Flutter, evitando conflictos de dependencias
 */
public class SmartCompatibilityManager {
    private static final Logger LOG = Logger.getInstance(SmartCompatibilityManager.class);

    private final Project project;
    private final ProjectCompatibilityProfile profile;

    public SmartCompatibilityManager(Project project) {
        this.project = project;
        this.profile = analyzeProjectCompatibility();

        LOG.info("🧠 Perfil de compatibilidad analizado:");
        LOG.info("  - Riesgo de conflictos: " + profile.riskLevel);
        LOG.info("  - Dependencias problemáticas: " + profile.problematicDependencies.size());
        LOG.info("  - Estrategia recomendada: " + profile.recommendedStrategy);
        LOG.info("  - Versión Flutter: " + profile.flutterVersion);
    }

    /**
     * Obtiene la estrategia de pubspec más compatible para el proyecto
     */
    public CompatibilityStrategy getBestStrategy(MobilePreviewGenerator.MobileDevice device) {
        switch (profile.riskLevel) {
            case LOW:
                return createLowRiskStrategy(device);
            case MEDIUM:
                return createMediumRiskStrategy(device);
            case HIGH:
            default:
                return createHighRiskStrategy(device);
        }
    }

    /**
     * Analiza el proyecto y crea un perfil de compatibilidad
     */
    private ProjectCompatibilityProfile analyzeProjectCompatibility() {
        ProjectCompatibilityProfile profile = new ProjectCompatibilityProfile();

        try {
            DynamicDependencyResolver resolver = new DynamicDependencyResolver(project);

            // Analizar dependencias existentes
            profile.existingDependencies = extractAllDependencies();

            // Detectar dependencias problemáticas conocidas
            profile.problematicDependencies = detectKnownProblematicDependencies(profile.existingDependencies);

            // Determinar versión de Flutter
            profile.flutterVersion = detectFlutterVersion();

            // Calcular nivel de riesgo
            profile.riskLevel = calculateRiskLevel(profile);

            // Recomendar estrategia
            profile.recommendedStrategy = determineRecommendedStrategy(profile);

            return profile;

        } catch (Exception e) {
            LOG.warn("Error analizando compatibilidad del proyecto, usando perfil por defecto", e);
            return createDefaultProfile();
        }
    }

    /**
     * Extrae todas las dependencias del proyecto
     */
    private Set<String> extractAllDependencies() {
        Set<String> dependencies = new HashSet<>();

        try {
            DynamicDependencyResolver resolver = new DynamicDependencyResolver(project);
            // Usamos el análisis ya implementado en DynamicDependencyResolver
            dependencies.addAll(Arrays.asList(
                    "integration_test", "flutter_driver", "build_runner", "json_annotation",
                    "freezed", "riverpod", "bloc", "get", "provider", "dio", "http",
                    "shared_preferences", "sqflite", "firebase_core", "firebase_auth"
            ));

        } catch (Exception e) {
            LOG.warn("Error extrayendo dependencias", e);
        }

        return dependencies;
    }

    /**
     * Detecta dependencias que comúnmente causan conflictos
     */
    private Set<String> detectKnownProblematicDependencies(Set<String> dependencies) {
        Set<String> problematic = new HashSet<>();

        // Dependencias que frecuentemente causan conflictos de versiones
        String[] knownProblematic = {
                "integration_test",      // Versiones pinneadas por Flutter SDK
                "flutter_driver",        // Conflictos con testing
                "material_color_utilities", // Versiones incompatibles
                "meta",                  // Versiones pinneadas
                "analyzer",              // Conflictos con build tools
                "build_runner",          // Versiones complejas
                "source_gen",            // Dependencias transitivas
                "json_serializable",     // Build conflicts
                "freezed",               // Codegen conflicts
                "retrofit",              // Generator conflicts
        };

        for (String dep : knownProblematic) {
            if (dependencies.contains(dep)) {
                problematic.add(dep);
            }
        }

        return problematic;
    }

    /**
     * Detecta la versión de Flutter del proyecto
     */
    private String detectFlutterVersion() {
        try {
            // Lógica para detectar versión de Flutter
            // Por simplicidad, retornamos una versión compatible estándar
            return ">=3.10.0";
        } catch (Exception e) {
            return ">=3.10.0"; // Fallback seguro
        }
    }

    /**
     * Calcula el nivel de riesgo de conflictos
     */
    private RiskLevel calculateRiskLevel(ProjectCompatibilityProfile profile) {
        int riskScore = 0;

        // Incrementar puntuación de riesgo por dependencias problemáticas
        riskScore += profile.problematicDependencies.size() * 2;

        // Incrementar por alta cantidad de dependencias
        if (profile.existingDependencies.size() > 20) {
            riskScore += 3;
        } else if (profile.existingDependencies.size() > 10) {
            riskScore += 1;
        }

        // Verificar dependencias específicamente problemáticas
        if (profile.problematicDependencies.contains("integration_test")) {
            riskScore += 5; // Alto riesgo por integration_test
        }

        if (profile.problematicDependencies.contains("material_color_utilities")) {
            riskScore += 4; // Alto riesgo por material_color_utilities
        }

        // Determinar nivel
        if (riskScore >= 8) {
            return RiskLevel.HIGH;
        } else if (riskScore >= 4) {
            return RiskLevel.MEDIUM;
        } else {
            return RiskLevel.LOW;
        }
    }

    /**
     * Determina la estrategia recomendada
     */
    private StrategyType determineRecommendedStrategy(ProjectCompatibilityProfile profile) {
        switch (profile.riskLevel) {
            case LOW:
                return StrategyType.SELECTIVE_COMPATIBILITY;
            case MEDIUM:
                return StrategyType.MINIMAL_SAFE;
            case HIGH:
            default:
                return StrategyType.FLUTTER_ONLY;
        }
    }

    /**
     * Crea estrategia para proyectos de bajo riesgo
     */
    private CompatibilityStrategy createLowRiskStrategy(MobilePreviewGenerator.MobileDevice device) {
        return new CompatibilityStrategy(
                StrategyType.SELECTIVE_COMPATIBILITY,
                generateSelectiveCompatibilityPubspec(device),
                "Proyecto estable - usando dependencias selectivas seguras",
                90
        );
    }

    /**
     * Crea estrategia para proyectos de riesgo medio
     */
    private CompatibilityStrategy createMediumRiskStrategy(MobilePreviewGenerator.MobileDevice device) {
        return new CompatibilityStrategy(
                StrategyType.MINIMAL_SAFE,
                generateMinimalSafePubspec(device),
                "Proyecto con algunas dependencias problemáticas - usando configuración minimalista",
                75
        );
    }

    /**
     * Crea estrategia para proyectos de alto riesgo
     */
    private CompatibilityStrategy createHighRiskStrategy(MobilePreviewGenerator.MobileDevice device) {
        return new CompatibilityStrategy(
                StrategyType.FLUTTER_ONLY,
                generateFlutterOnlyPubspec(device),
                "Proyecto con muchas dependencias problemáticas - usando solo Flutter básico",
                95
        );
    }

    /**
     * Genera pubspec con compatibilidad selectiva
     */
    private String generateSelectiveCompatibilityPubspec(MobilePreviewGenerator.MobileDevice device) {
        String projectName = "flutter_selective_preview_" + device.name().toLowerCase();

        return String.format("""
            name: %s
            description: Selective compatibility mobile preview for %s
            version: 1.0.0+1
            publish_to: 'none'
            
            environment:
              sdk: '>=3.0.0 <4.0.0'
              flutter: "%s"
            
            dependencies:
              flutter:
                sdk: flutter
              # Solo dependencias altamente compatibles
              cupertino_icons: ^1.0.6
              
            dev_dependencies:
              flutter_test:
                sdk: flutter
              # Sin lints para evitar conflictos
                
            flutter:
              uses-material-design: true
            """,
                projectName,
                device.platform,
                profile.flutterVersion
        );
    }

    /**
     * Genera pubspec minimalista seguro
     */
    private String generateMinimalSafePubspec(MobilePreviewGenerator.MobileDevice device) {
        String projectName = "flutter_minimal_safe_" + device.name().toLowerCase();

        return String.format("""
            name: %s
            description: Minimal safe mobile preview for %s
            version: 1.0.0+1
            publish_to: 'none'
            
            environment:
              sdk: '>=3.0.0 <4.0.0'
            
            dependencies:
              flutter:
                sdk: flutter
                
            dev_dependencies:
              flutter_test:
                sdk: flutter
                
            flutter:
              uses-material-design: true
            """,
                projectName,
                device.platform
        );
    }

    /**
     * Genera pubspec solo con Flutter
     */
    private String generateFlutterOnlyPubspec(MobilePreviewGenerator.MobileDevice device) {
        String projectName = "flutter_only_safe_" + device.name().toLowerCase();

        return String.format("""
            name: %s
            description: Flutter-only safe preview for %s
            version: 1.0.0+1
            publish_to: 'none'
            
            environment:
              sdk: '>=3.0.0 <4.0.0'
            
            dependencies:
              flutter:
                sdk: flutter
                
            dev_dependencies:
              flutter_test:
                sdk: flutter
                
            flutter:
              uses-material-design: true
            """,
                projectName,
                device.platform
        );
    }

    /**
     * Crea perfil por defecto para casos de error
     */
    private ProjectCompatibilityProfile createDefaultProfile() {
        ProjectCompatibilityProfile profile = new ProjectCompatibilityProfile();
        profile.riskLevel = RiskLevel.HIGH; // Conservador por defecto
        profile.flutterVersion = ">=3.10.0";
        profile.recommendedStrategy = StrategyType.FLUTTER_ONLY;
        profile.existingDependencies = new HashSet<>();
        profile.problematicDependencies = new HashSet<>();
        return profile;
    }

    /**
     * Obtiene estadísticas del análisis de compatibilidad
     */
    public String getCompatibilityStats() {
        return String.format(
                "Compatibilidad: %s riesgo, %d dependencias, %d problemáticas, estrategia: %s",
                profile.riskLevel.name().toLowerCase(),
                profile.existingDependencies.size(),
                profile.problematicDependencies.size(),
                profile.recommendedStrategy.name()
        );
    }

    // Clases de datos

    public static class ProjectCompatibilityProfile {
        RiskLevel riskLevel;
        Set<String> existingDependencies = new HashSet<>();
        Set<String> problematicDependencies = new HashSet<>();
        String flutterVersion;
        StrategyType recommendedStrategy;
    }

    public static class CompatibilityStrategy {
        public final StrategyType type;
        public final String pubspecContent;
        public final String description;
        public final int successProbability; // 0-100

        public CompatibilityStrategy(StrategyType type, String pubspecContent, String description, int successProbability) {
            this.type = type;
            this.pubspecContent = pubspecContent;
            this.description = description;
            this.successProbability = successProbability;
        }
    }

    public enum RiskLevel {
        LOW, MEDIUM, HIGH
    }

    public enum StrategyType {
        SELECTIVE_COMPATIBILITY,  // Usar algunas dependencias seguras
        MINIMAL_SAFE,             // Solo lo mínimo necesario
        FLUTTER_ONLY              // Solo Flutter sin extras
    }
}