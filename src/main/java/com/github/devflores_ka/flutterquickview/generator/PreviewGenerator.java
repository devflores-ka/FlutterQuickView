package com.github.devflores_ka.flutterquickview.generator;

import com.github.devflores_ka.flutterquickview.analyzer.models.WidgetNode;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Generador optimizado de archivos temporales para renderizado de previews
 * Crea proyectos Flutter mínimos con configuración optimizada para velocidad
 */
public class PreviewGenerator {
    private static final Logger LOG = Logger.getInstance(PreviewGenerator.class);

    // Templates embebidos para evitar dependencias externas
    private static final String PUBSPEC_TEMPLATE = """
        name: flutter_quick_preview_temp
        description: Temporary Flutter project for widget previews
        version: 1.0.0+1
        publish_to: 'none'
        
        environment:
          sdk: '>=3.0.0 <4.0.0'
          flutter: ">=3.10.0"
        
        dependencies:
          flutter:
            sdk: flutter
          cupertino_icons: ^1.0.6
        
        dev_dependencies:
          flutter_test:
            sdk: flutter
        
        flutter:
          uses-material-design: true
        """;

    private static final String ANALYSIS_OPTIONS_TEMPLATE = """
        include: package:flutter_lints/flutter.yaml
        
        analyzer:
          exclude:
            - "**/*.g.dart"
          language:
            strict-casts: false
            strict-inference: false
        
        linter:
          rules:
            avoid_print: false
            prefer_const_constructors: false
        """;

    private static final String TEST_CONFIG_TEMPLATE = """
        import 'dart:async';
        import 'package:flutter_test/flutter_test.dart';
       \s
        Future<void> testExecutable(FutureOr<void> Function() testMain) async {
          setUpAll(() {
            TestWidgetsFlutterBinding.ensureInitialized();
           \s
            if (goldenFileComparator is LocalFileComparator) {
              final testUrl = (goldenFileComparator as LocalFileComparator).basedir;
              goldenFileComparator = LocalFileComparator(testUrl.resolve('goldens/'));
            }
          });
         \s
          await testMain();
        }
       \s""";

    public PreviewGenerator(Project project) {
    }

    /**
     * Genera un proyecto temporal optimizado para un widget específico
     */
    public Path generatePreviewProject(WidgetNode widget) throws IOException {
        return generatePreviewProject(List.of(widget));
    }

    /**
     * Genera un proyecto temporal optimizado para múltiples widgets
     */
    public Path generatePreviewProject(List<WidgetNode> widgets) throws IOException {
        if (widgets.isEmpty()) {
            throw new IllegalArgumentException("No widgets provided");
        }

        LOG.info("Generando proyecto temporal para " + widgets.size() + " widgets");

        // Crear directorio temporal único
        Path tempProjectDir = createTempProjectDirectory();

        try {
            // 1. Crear estructura básica del proyecto
            createProjectStructure(tempProjectDir);

            // 2. Generar archivos de configuración optimizados
            generateConfigurationFiles(tempProjectDir);

            // 3. Extraer y procesar dependencias de los widgets
            List<String> dependencies = extractDependencies(widgets);
            updatePubspecWithDependencies(tempProjectDir, dependencies);

            // 4. Generar archivos de widget y tests
            generateWidgetFiles(tempProjectDir, widgets);
            generateTestFiles(tempProjectDir, widgets);

            // 5. Crear archivo main.dart si es necesario
            generateMainFile(tempProjectDir, widgets);

            LOG.info("Proyecto temporal generado en: " + tempProjectDir);
            return tempProjectDir;

        } catch (Exception e) {
            // Limpiar en caso de error
            cleanupTempProject(tempProjectDir);
            throw new IOException("Error generando proyecto temporal", e);
        }
    }

    /**
     * Crea directorio temporal único para el proyecto
     */
    private Path createTempProjectDirectory() throws IOException {
        Path tempBaseDir = Paths.get(System.getProperty("java.io.tmpdir"), "flutter_quick_view");
        Files.createDirectories(tempBaseDir);

        String projectName = "preview_" + System.currentTimeMillis();
        Path projectDir = tempBaseDir.resolve(projectName);

        return Files.createDirectories(projectDir);
    }

    /**
     * Crea la estructura básica del proyecto Flutter
     */
    private void createProjectStructure(Path projectDir) throws IOException {
        // Directorios principales
        Files.createDirectories(projectDir.resolve("lib"));
        Files.createDirectories(projectDir.resolve("test"));
        Files.createDirectories(projectDir.resolve("test/goldens"));
        Files.createDirectories(projectDir.resolve("integration_test"));

        LOG.debug("Estructura de proyecto creada en: " + projectDir);
    }

    /**
     * Genera archivos de configuración optimizados
     */
    private void generateConfigurationFiles(Path projectDir) throws IOException {
        // pubspec.yaml
        Files.writeString(projectDir.resolve("pubspec.yaml"), PUBSPEC_TEMPLATE);

        // analysis_options.yaml
        Files.writeString(projectDir.resolve("analysis_options.yaml"), ANALYSIS_OPTIONS_TEMPLATE);

        // flutter_test_config.dart
        Files.writeString(projectDir.resolve("test/flutter_test_config.dart"), TEST_CONFIG_TEMPLATE);

        LOG.debug("Archivos de configuración generados");
    }

    /**
     * Extrae dependencias necesarias de los widgets
     */
    private List<String> extractDependencies(List<WidgetNode> widgets) {
        // Por ahora retornamos dependencias básicas
        // TODO: Implementar análisis más sofisticado del código fuente
        return List.of(
                "cupertino_icons: ^1.0.6"
        );
    }

    /**
     * Actualiza pubspec.yaml con dependencias adicionales
     */
    private void updatePubspecWithDependencies(Path projectDir, List<String> dependencies) throws IOException {
        if (dependencies.isEmpty()) return;

        Path pubspecFile = projectDir.resolve("pubspec.yaml");
        String content = Files.readString(pubspecFile);

        // Insertar dependencias adicionales antes de dev_dependencies
        StringBuilder additionalDeps = new StringBuilder();
        for (String dep : dependencies) {
            if (!content.contains(dep.split(":")[0])) {
                additionalDeps.append("  ").append(dep).append("\n");
            }
        }

        if (!additionalDeps.isEmpty()) {
            content = content.replace("dev_dependencies:", additionalDeps + "\ndev_dependencies:");
            Files.writeString(pubspecFile, content);
            LOG.debug("Dependencias adicionales agregadas: " + dependencies);
        }
    }

    /**
     * Genera archivos de widget en el proyecto temporal
     */
    private void generateWidgetFiles(Path projectDir, List<WidgetNode> widgets) throws IOException {
        for (WidgetNode widget : widgets) {
            String fileName = widget.getClassName().toLowerCase() + ".dart";
            Path widgetFile = projectDir.resolve("lib").resolve(fileName);

            String widgetContent = generateWidgetFileContent(widget);
            Files.writeString(widgetFile, widgetContent);

            LOG.debug("Archivo de widget generado: " + fileName);
        }
    }

    /**
     * Genera el contenido optimizado para un archivo de widget
     */
    private String generateWidgetFileContent(WidgetNode widget) {
        StringBuilder content = new StringBuilder();

        // Imports básicos
        content.append("import 'package:flutter/material.dart';\n");
        content.append("import 'package:flutter/cupertino.dart';\n");

        // Extraer imports adicionales del código original
        String originalImports = extractImportsFromSourceCode(widget.getSourceCode());
        if (!originalImports.isEmpty()) {
            content.append(originalImports).append("\n");
        }

        // Código del widget (sin imports duplicados)
        String cleanWidgetCode = removeImportsFromCode(widget.getSourceCode());
        content.append("\n").append(cleanWidgetCode);

        return content.toString();
    }

    /**
     * Genera archivos de test para cada widget
     */
    private void generateTestFiles(Path projectDir, List<WidgetNode> widgets) throws IOException {
        for (WidgetNode widget : widgets) {
            String testFileName = widget.getClassName().toLowerCase() + "_test.dart";
            Path testFile = projectDir.resolve("test").resolve(testFileName);

            String testContent = generateTestFileContent(widget);
            Files.writeString(testFile, testContent);

            LOG.debug("Archivo de test generado: " + testFileName);
        }
    }

    /**
     * Genera el contenido optimizado de un archivo de test
     */
    private String generateTestFileContent(WidgetNode widget) {
        String className = widget.getClassName();
        String goldenFileName = className.toLowerCase() + "_preview.png";

        return String.format("""
            import 'package:flutter/material.dart';
            import 'package:flutter_test/flutter_test.dart';
            import '../lib/%s.dart';
           \s
            void main() {
              testWidgets('%s preview test', (WidgetTester tester) async {
                await tester.pumpWidget(
                  MaterialApp(
                    debugShowCheckedModeBanner: false,
                    theme: ThemeData.light(),
                    home: Scaffold(
                      body: Center(
                        child: SizedBox(
                          width: 375,
                          height: 667,
                          child: %s(),
                        ),
                      ),
                    ),
                  ),
                );
               \s
                // Estabilizar la UI
                await tester.pump(Duration(milliseconds: 500));
                await tester.pumpAndSettle(Duration(seconds: 2));
               \s
                // Generar golden file
                await expectLater(
                  find.byType(MaterialApp),
                  matchesGoldenFile('%s'),
                );
              });
            }
           \s""",
                className.toLowerCase(),
                className,
                className,
                goldenFileName
        );
    }

    /**
     * Genera archivo main.dart si es necesario
     */
    private void generateMainFile(Path projectDir, List<WidgetNode> widgets) throws IOException {
        // Solo generar main.dart si no existe o si es necesario para dependencias
        Path mainFile = projectDir.resolve("lib/main.dart");

        if (!Files.exists(mainFile)) {
            String mainContent = generateMainFileContent(widgets);
            Files.writeString(mainFile, mainContent);
            LOG.debug("Archivo main.dart generado");
        }
    }

    /**
     * Genera contenido básico de main.dart
     */
    private String generateMainFileContent(List<WidgetNode> widgets) {
        WidgetNode firstWidget = widgets.get(0);

        return String.format("""
            import 'package:flutter/material.dart';
            import '%s.dart';
            
            void main() {
              runApp(MyApp());
            }
            
            class MyApp extends StatelessWidget {
              @override
              Widget build(BuildContext context) {
                return MaterialApp(
                  title: 'Flutter Quick Preview',
                  theme: ThemeData(primarySwatch: Colors.blue),
                  home: %s(),
                );
              }
            }
            """,
                firstWidget.getClassName().toLowerCase(),
                firstWidget.getClassName()
        );
    }

    /**
     * Extrae imports del código fuente original
     */
    private String extractImportsFromSourceCode(String sourceCode) {
        Pattern importPattern = Pattern.compile("^import\\s+[^;]+;\\s*$", Pattern.MULTILINE);
        Matcher matcher = importPattern.matcher(sourceCode);

        StringBuilder imports = new StringBuilder();
        while (matcher.find()) {
            String importLine = matcher.group().trim();
            // Filtrar imports de 'package:flutter' ya incluidos
            if (!importLine.contains("package:flutter/material.dart") &&
                    !importLine.contains("package:flutter/cupertino.dart")) {
                imports.append(importLine).append("\n");
            }
        }

        return imports.toString();
    }

    /**
     * Remueve imports del código para evitar duplicados
     */
    private String removeImportsFromCode(String sourceCode) {
        Pattern importPattern = Pattern.compile("^import\\s+[^;]+;\\s*\n?", Pattern.MULTILINE);
        return importPattern.matcher(sourceCode).replaceAll("").trim();
    }

    /**
     * Limpia proyecto temporal en caso de error
     */
    private void cleanupTempProject(Path projectDir) {
        try {
            if (Files.exists(projectDir)) {
                Files.walk(projectDir)
                        .sorted(Comparator.reverseOrder()) // Borrar archivos antes que directorios
                        .forEach(path -> {
                            try {
                                Files.deleteIfExists(path);
                            } catch (IOException e) {
                                LOG.warn("Error eliminando: " + path, e);
                            }
                        });
            }
        } catch (Exception e) {
            LOG.warn("Error en cleanup de proyecto temporal", e);
        }
    }

    /**
     * Copia dependencias del proyecto original si existen
     */
    public void copyProjectDependencies(Path tempProjectDir, VirtualFile originalProjectRoot) {
        try {
            // Copiar pubspec.yaml del proyecto original para obtener dependencias
            VirtualFile originalPubspec = originalProjectRoot.findChild("pubspec.yaml");
            if (originalPubspec != null && originalPubspec.exists()) {
                Path originalPubspecPath = Paths.get(originalPubspec.getPath());
                Path tempPubspecPath = tempProjectDir.resolve("pubspec_original.yaml");

                Files.copy(originalPubspecPath, tempPubspecPath, StandardCopyOption.REPLACE_EXISTING);

                // Mergear dependencias del proyecto original
                mergeDependencies(tempProjectDir, tempPubspecPath);

                LOG.debug("Dependencias del proyecto original copiadas");
            }
        } catch (Exception e) {
            LOG.warn("Error copiando dependencias del proyecto original", e);
        }
    }

    /**
     * Mergea dependencias del proyecto original con él, témplate
     */
    private void mergeDependencies(Path tempProjectDir, Path originalPubspecPath) throws IOException {
        String originalContent = Files.readString(originalPubspecPath);
        String tempContent = Files.readString(tempProjectDir.resolve("pubspec.yaml"));

        // Extraer sección de dependencies del proyecto original
        Pattern depsPattern = Pattern.compile("dependencies:\\s*\n((?:\\s+[^\\n]+\n)*)", Pattern.MULTILINE);
        Matcher matcher = depsPattern.matcher(originalContent);

        if (matcher.find()) {
            String originalDeps = matcher.group(1);

            // Insertar en el pubspec temporal
            tempContent = tempContent.replace(
                    "dependencies:\n  flutter:\n    sdk: flutter",
                    "dependencies:\n  flutter:\n    sdk: flutter\n" + originalDeps
            );

            Files.writeString(tempProjectDir.resolve("pubspec.yaml"), tempContent);
            LOG.debug("Dependencias mergeadas exitosamente");
        }

        // Limpiar archivo temporal
        Files.deleteIfExists(originalPubspecPath);
    }

    /**
     * Obtiene el directorio de proyecto temporal base
     */
    public static Path getTempProjectsDirectory() {
        return Paths.get(System.getProperty("java.io.tmpdir"), "flutter_quick_view");
    }

    /**
     * Limpia todos los proyectos temporales antiguos
     */
    public static void cleanupOldTempProjects() {
        try {
            Path tempDir = getTempProjectsDirectory();
            if (!Files.exists(tempDir)) return;

            long cutoffTime = System.currentTimeMillis() - (24 * 60 * 60 * 1000); // 24 horas

            Files.list(tempDir)
                    .filter(Files::isDirectory)
                    .filter(path -> {
                        try {
                            long lastModified = Files.getLastModifiedTime(path).toMillis();
                            return lastModified < cutoffTime;
                        } catch (IOException e) {
                            return true; // Eliminar si no se puede leer
                        }
                    })
                    .forEach(path -> {
                        try {
                            Files.walk(path)
                                    .sorted(Comparator.reverseOrder())
                                    .forEach(file -> {
                                        try {
                                            Files.deleteIfExists(file);
                                        } catch (IOException e) {
                                            // Ignorar errores de eliminación
                                        }
                                    });
                        } catch (IOException e) {
                            // Ignorar errores
                        }
                    });

            LOG.info("Proyectos temporales antiguos limpiados");

        } catch (Exception e) {
            LOG.warn("Error limpiando proyectos temporales antiguos", e);
        }
    }
}