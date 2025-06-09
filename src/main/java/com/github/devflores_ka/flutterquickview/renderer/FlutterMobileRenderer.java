package com.github.devflores_ka.flutterquickview.renderer;

import com.github.devflores_ka.flutterquickview.analyzer.models.WidgetNode;
import com.github.devflores_ka.flutterquickview.generator.MobilePreviewGenerator;
import com.github.devflores_ka.flutterquickview.generator.DynamicDependencyResolver;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Renderizador móvil mejorado con resolución adaptativa de dependencias
 * VERSIÓN CORREGIDA que evita conflictos de dependencias automáticamente
 */
public class FlutterMobileRenderer {
    private static final Logger LOG = Logger.getInstance(FlutterMobileRenderer.class);

    // Cache específico para imágenes móviles
    private static final ConcurrentMap<String, BufferedImage> MOBILE_CACHE = new ConcurrentHashMap<>();

    private final Project project;
    private final FlutterProcessManager processManager;
    private final DynamicDependencyResolver dependencyResolver;

    public FlutterMobileRenderer(Project project) {
        this.project = project;
        this.processManager = new FlutterProcessManager(project);
        this.dependencyResolver = new DynamicDependencyResolver(project);

        LOG.info("🤖 FlutterMobileRenderer inicializado con resolución adaptativa");
        LOG.info("📊 " + dependencyResolver.getAnalysisStats());
    }

    /**
     * Renderiza widget con apariencia móvil usando estrategia adaptativa
     */
    public BufferedImage renderWidgetMobile(WidgetNode widget, MobilePreviewGenerator.MobileDevice device) throws Exception {
        LOG.info("🚀 Renderizando widget móvil: " + widget.getClassName() + " en " + device.name());

        // Verificar caché primero
        String cacheKey = generateMobileCacheKey(widget, device);
        BufferedImage cached = MOBILE_CACHE.get(cacheKey);
        if (cached != null) {
            LOG.info("✅ Imagen móvil encontrada en caché");
            return cached;
        }

        try {
            // ESTRATEGIA ADAPTATIVA: Crear proyecto con múltiples fallbacks
            Path projectDir = createAdaptiveMobileProject(widget, device, cacheKey);

            // Generar test móvil optimizado
            generateAdaptiveMobileTest(projectDir, widget, device);

            // Ejecutar test con manejo de errores mejorado
            Path outputImage = executeAdaptiveFlutterTest(projectDir, widget, device);

            // Cargar imagen y aplicar post-procesamiento móvil
            BufferedImage rawImage = javax.imageio.ImageIO.read(outputImage.toFile());
            BufferedImage processedImage = postProcessMobileImage(rawImage, device);

            // Cachear resultado
            MOBILE_CACHE.put(cacheKey, processedImage);

            LOG.info("✅ Renderizado móvil completado: " + processedImage.getWidth() + "x" + processedImage.getHeight());
            return processedImage;

        } catch (Exception e) {
            LOG.error("❌ Error en renderizado móvil para " + widget.getClassName(), e);
            throw e;
        }
    }

    /**
     * NUEVA: Crea proyecto móvil con estrategia adaptativa anti-conflictos
     */
    private Path createAdaptiveMobileProject(WidgetNode widget, MobilePreviewGenerator.MobileDevice device, String cacheKey) throws Exception {
        Path projectDir = Paths.get(project.getBasePath(), ".flutter_quick_view", "mobile_projects",
                device.name().toLowerCase(), "project_" + cacheKey);

        if (Files.exists(projectDir)) {
            deleteDirectory(projectDir);
        }

        Files.createDirectories(projectDir);
        Files.createDirectories(projectDir.resolve("lib"));
        Files.createDirectories(projectDir.resolve("test"));
        Files.createDirectories(projectDir.resolve("test/goldens"));

        // ESTRATEGIA 1: Pubspec compatible con el proyecto actual
        try {
            LOG.info("🔄 Estrategia 1: Pubspec compatible con proyecto actual");
            String compatiblePubspec = dependencyResolver.generateCompatiblePubspec(
                    "flutter_mobile_preview_" + device.name().toLowerCase(), device);

            Files.write(projectDir.resolve("pubspec.yaml"), compatiblePubspec.getBytes());

            // Probar pub get rápidamente
            if (testPubGetQuick(projectDir)) {
                LOG.info("✅ Estrategia 1 exitosa - usando pubspec compatible");
                createMobileProjectFiles(projectDir, device);
                return projectDir;
            }

        } catch (Exception e) {
            LOG.warn("⚠️ Estrategia 1 falló: " + e.getMessage());
        }

        // ESTRATEGIA 2: Pubspec minimalista
        try {
            LOG.info("🔄 Estrategia 2: Pubspec minimalista ultra-compatible");
            String minimalPubspec = generateUltraMinimalPubspec(device);

            Files.write(projectDir.resolve("pubspec.yaml"), minimalPubspec.getBytes());

            if (testPubGetQuick(projectDir)) {
                LOG.info("✅ Estrategia 2 exitosa - usando pubspec minimalista");
                createMobileProjectFiles(projectDir, device);
                return projectDir;
            }

        } catch (Exception e) {
            LOG.warn("⚠️ Estrategia 2 falló: " + e.getMessage());
        }

        // ESTRATEGIA 3: Pubspec solo-Flutter (última opción)
        try {
            LOG.info("🔄 Estrategia 3: Pubspec solo-Flutter");
            String basicPubspec = generateFlutterOnlyPubspec(device);

            Files.write(projectDir.resolve("pubspec.yaml"), basicPubspec.getBytes());

            if (testPubGetQuick(projectDir)) {
                LOG.info("✅ Estrategia 3 exitosa - usando pubspec básico");
                createMobileProjectFiles(projectDir, device);
                return projectDir;
            }

        } catch (Exception e) {
            LOG.error("❌ Todas las estrategias fallaron", e);
            throw new RuntimeException("No se pudo crear proyecto móvil compatible: " + e.getMessage());
        }

        throw new RuntimeException("Error crítico: ninguna estrategia de pubspec funcionó");
    }

    /**
     * Genera pubspec ultra-minimalista para máxima compatibilidad
     */
    private String generateUltraMinimalPubspec(MobilePreviewGenerator.MobileDevice device) {
        String deviceName = "flutter_minimal_mobile_" + device.name().toLowerCase();

        return String.format("""
            name: %s
            description: Ultra minimal mobile preview for %s
            version: 1.0.0+1
            publish_to: 'none'
            
            environment:
              sdk: '>=3.0.0 <4.0.0'
              flutter: ">=3.10.0"
            
            dependencies:
              flutter:
                sdk: flutter
              # Solo dependencias esenciales, sin conflictos
              
            dev_dependencies:
              flutter_test:
                sdk: flutter
              # Sin flutter_lints para evitar problemas
                
            flutter:
              uses-material-design: true
            """,
                deviceName,
                device.platform
        );
    }

    /**
     * Genera pubspec con solo Flutter (sin dependencias extra)
     */
    private String generateFlutterOnlyPubspec(MobilePreviewGenerator.MobileDevice device) {
        String deviceName = "flutter_only_mobile_" + device.name().toLowerCase();

        return String.format("""
            name: %s
            description: Flutter-only mobile preview for %s
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
                deviceName,
                device.platform
        );
    }

    /**
     * Prueba pub get rápidamente (timeout reducido)
     */
    private boolean testPubGetQuick(Path projectDir) {
        try {
            LOG.info("🧪 Probando pub get rápido en: " + projectDir.getFileName());

            String flutterExecutable = getFlutterExecutablePath();
            String[] command = {flutterExecutable, "pub", "get"};

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.directory(projectDir.toFile());

            // Timeout reducido para prueba rápida
            Process process = pb.start();
            boolean finished = process.waitFor(30, java.util.concurrent.TimeUnit.SECONDS);

            if (!finished) {
                process.destroyForcibly();
                LOG.warn("⏰ pub get timeout (30s)");
                return false;
            }

            boolean success = process.exitValue() == 0;
            LOG.info("Resultado pub get rápido: " + (success ? "✅ ÉXITO" : "❌ FALLO"));

            if (!success) {
                // Leer error para debugging
                try (java.io.BufferedReader reader = new java.io.BufferedReader(
                        new java.io.InputStreamReader(process.getErrorStream()))) {
                    String errorLine = reader.readLine();
                    if (errorLine != null) {
                        LOG.warn("Error pub get: " + errorLine);
                    }
                }
            }

            return success;

        } catch (Exception e) {
            LOG.warn("⚠️ Error probando pub get: " + e.getMessage());
            return false;
        }
    }

    /**
     * Crea archivos del proyecto móvil optimizados
     */
    private void createMobileProjectFiles(Path projectDir, MobilePreviewGenerator.MobileDevice device) throws IOException {
        // Crear analysis_options.yaml súper permisivo
        String analysisContent = """
            # Configuración súper permisiva para evitar errores de análisis
            analyzer:
              exclude:
                - "**/*.g.dart"
                - "**/*.freezed.dart"
              errors:
                # Ignorar todos los warnings/errores que puedan aparecer
                missing_required_param: ignore
                missing_return: ignore
                unused_import: ignore
                unnecessary_import: ignore
                prefer_const_constructors: ignore
                use_key_in_widget_constructors: ignore
                library_private_types_in_public_api: ignore
              language:
                strict-casts: false
                strict-inference: false
                strict-raw-types: false
            
            linter:
              rules: {} # Sin reglas de linting para máxima compatibilidad
            """;

        Files.write(projectDir.resolve("analysis_options.yaml"), analysisContent.getBytes());

        // Crear configuración de dispositivo simplificada
        String deviceConfig = String.format("""
            // Configuración simplificada para %s
            const double DEVICE_WIDTH = %d;
            const double DEVICE_HEIGHT = %d;
            const double DEVICE_PIXEL_RATIO = %f;
            const String DEVICE_PLATFORM = '%s';
            """,
                device.name(),
                device.width,
                device.height,
                device.pixelRatio,
                device.platform
        );

        Files.write(projectDir.resolve("lib/device_config.dart"), deviceConfig.getBytes());

        LOG.debug("📁 Archivos de proyecto móvil creados");
    }

    /**
     * Genera test móvil adaptativo sin dependencias problemáticas
     */
    private void generateAdaptiveMobileTest(Path projectDir, WidgetNode widget, MobilePreviewGenerator.MobileDevice device) throws IOException {
        String testContent = generateSimplifiedMobileTest(widget, device);
        String testFileName = widget.getClassName().toLowerCase() + "_mobile_test.dart";
        Files.write(projectDir.resolve("test").resolve(testFileName), testContent.getBytes());

        LOG.debug("📝 Test móvil adaptativo generado: " + testFileName);
    }

    /**
     * Genera test móvil simplificado sin dependencias complejas
     */
    private String generateSimplifiedMobileTest(WidgetNode widget, MobilePreviewGenerator.MobileDevice device) {
        String pixelRatioStr = String.format(java.util.Locale.US, "%.1f", device.pixelRatio);
        String widgetCode = removeImportsFromCode(widget.getSourceCode());

        // DETECCIÓN INTELIGENTE: ¿El widget ya contiene MaterialApp?
        boolean hasOwnMaterialApp = widgetCode.contains("MaterialApp");

        if (hasOwnMaterialApp) {
            // CASO 1: Widget ya tiene MaterialApp - usarlo directamente
            return String.format(java.util.Locale.US, """
            import 'package:flutter/material.dart';
            import 'package:flutter_test/flutter_test.dart';
            
            %s
            
            void main() {
              testWidgets('%s mobile preview', (WidgetTester tester) async {
                // Configurar tamaño de dispositivo móvil
                await tester.binding.setSurfaceSize(Size(%d, %d));
                tester.binding.window.devicePixelRatioTestValue = %s;
                
                // Widget ya tiene MaterialApp - usar directamente
                await tester.pumpWidget(%s());
                
                // Esperar estabilización
                await tester.pumpAndSettle(Duration(seconds: 1));
                
                // Capturar el único MaterialApp
                await expectLater(
                  find.byType(MaterialApp),
                  matchesGoldenFile('%s_mobile_%s.png'),
                );
                
                // Limpiar configuración
                tester.binding.window.clearDevicePixelRatioTestValue();
              });
            }
            """,
                    widgetCode,
                    widget.getClassName(),
                    device.width,
                    device.height,
                    pixelRatioStr,
                    widget.getClassName(),
                    widget.getClassName().toLowerCase(),
                    device.name().toLowerCase()
            );
        } else {
            // CASO 2: Widget NO tiene MaterialApp - crear uno
            return String.format(java.util.Locale.US, """
            import 'package:flutter/material.dart';
            import 'package:flutter_test/flutter_test.dart';
            
            %s
            
            void main() {
              testWidgets('%s mobile preview', (WidgetTester tester) async {
                // Configurar tamaño de dispositivo móvil
                await tester.binding.setSurfaceSize(Size(%d, %d));
                tester.binding.window.devicePixelRatioTestValue = %s;
                
                // Widget NO tiene MaterialApp - crear uno
                await tester.pumpWidget(
                  MaterialApp(
                    debugShowCheckedModeBanner: false,
                    theme: %s,
                    home: Material(
                      child: SafeArea(
                        child: Container(
                          width: %d,
                          height: %d,
                          decoration: BoxDecoration(
                            color: Colors.white,
                            borderRadius: BorderRadius.circular(%d),
                          ),
                          child: ClipRRect(
                            borderRadius: BorderRadius.circular(%d),
                            child: %s(),
                          ),
                        ),
                      ),
                    ),
                  ),
                );
                
                // Esperar estabilización
                await tester.pumpAndSettle(Duration(seconds: 1));
                
                // Capturar el MaterialApp creado
                await expectLater(
                  find.byType(MaterialApp),
                  matchesGoldenFile('%s_mobile_%s.png'),
                );
                
                // Limpiar configuración
                tester.binding.window.clearDevicePixelRatioTestValue();
              });
            }
            """,
                    widgetCode,
                    widget.getClassName(),
                    device.width,
                    device.height,
                    pixelRatioStr,
                    generateSimplifiedTheme(device),
                    device.width,
                    device.height,
                    device.platform.equals("iOS") ? 25 : 12,
                    device.platform.equals("iOS") ? 25 : 12,
                    widget.getClassName(),
                    widget.getClassName().toLowerCase(),
                    device.name().toLowerCase()
            );
        }
    }

    /**
     * Genera test ultra-simple - CORREGIDO
     */
    private String generateUltraSimpleTest(WidgetNode widget, MobilePreviewGenerator.MobileDevice device) {
        // CORREGIR: Formato decimal con Locale.US
        String pixelRatioStr = String.format(java.util.Locale.US, "%.1f", device.pixelRatio);

        return String.format(java.util.Locale.US, """
        import 'package:flutter/material.dart';
        import 'package:flutter_test/flutter_test.dart';
        
        %s
        
        void main() {
          testWidgets('ultra simple test', (tester) async {
            await tester.binding.setSurfaceSize(Size(%d, %d));
            tester.binding.window.devicePixelRatioTestValue = %s;
            
            await tester.pumpWidget(
              MaterialApp(
                debugShowCheckedModeBanner: false,
                home: Material(child: %s()),
              ),
            );
            
            await tester.pumpAndSettle();
            
            await expectLater(
              find.byType(MaterialApp),
              matchesGoldenFile('%s_ultra.png'),
            );
            
            // Limpiar
            tester.binding.window.clearDevicePixelRatioTestValue();
          });
        }
        """,
                removeImportsFromCode(widget.getSourceCode()),
                device.width,
                device.height,
                pixelRatioStr,  // CORREGIDO
                widget.getClassName(),
                widget.getClassName().toLowerCase()
        );
    }

    /**
     * Genera tema simplificado sin dependencias externas
     */
    private String generateSimplifiedTheme(MobilePreviewGenerator.MobileDevice device) {
        if (device.platform.equals("iOS")) {
            return """
                ThemeData(
                  brightness: Brightness.light,
                  primarySwatch: Colors.blue,
                  visualDensity: VisualDensity.compact,
                  appBarTheme: AppBarTheme(
                    backgroundColor: Colors.white,
                    elevation: 0,
                    foregroundColor: Colors.black,
                  ),
                )""";
        } else {
            return """
                ThemeData(
                  brightness: Brightness.light,
                  primarySwatch: Colors.blue,
                  visualDensity: VisualDensity.standard,
                  appBarTheme: AppBarTheme(
                    backgroundColor: Colors.white,
                    elevation: 4,
                    foregroundColor: Colors.black,
                  ),
                )""";
        }
    }

    /**
     * Ejecuta test Flutter con manejo adaptativo de errores
     */
    private Path executeAdaptiveFlutterTest(Path projectDir, WidgetNode widget, MobilePreviewGenerator.MobileDevice device) throws Exception {
        String testFileName = widget.getClassName().toLowerCase() + "_mobile_test.dart";

        LOG.info("🚀 Ejecutando test móvil adaptativo: " + testFileName);

        if (!processManager.isFlutterAvailable()) {
            throw new RuntimeException("Flutter no está disponible. Ruta detectada: " + processManager.getFlutterSdkPath());
        }

        String flutterExecutable = getFlutterExecutablePath();

        // Comando simplificado sin flags problemáticos
        String[] command = {
                flutterExecutable, "test",
                "test/" + testFileName,
                "--update-goldens"
        };

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(projectDir.toFile());

        // Variables de entorno simplificadas
        pb.environment().put("FLUTTER_TEST", "true");
        pb.environment().put("FLUTTER_WEB_USE_SKIA", "false");

        LOG.info("📋 Ejecutando comando: " + String.join(" ", command));
        LOG.info("📁 Directorio de trabajo: " + projectDir);

        Process process = pb.start();

        // Capturar salida para debugging
        StringBuilder output = new StringBuilder();
        StringBuilder errors = new StringBuilder();

        Thread outputThread = new Thread(() -> {
            try (java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                    LOG.info("📤 Flutter OUT: " + line);
                }
            } catch (Exception e) {
                LOG.warn("Error leyendo stdout", e);
            }
        });

        Thread errorThread = new Thread(() -> {
            try (java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(process.getErrorStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    errors.append(line).append("\n");
                    LOG.warn("📥 Flutter ERR: " + line);
                }
            } catch (Exception e) {
                LOG.warn("Error leyendo stderr", e);
            }
        });

        outputThread.start();
        errorThread.start();

        boolean finished = process.waitFor(90, java.util.concurrent.TimeUnit.SECONDS);

        if (!finished) {
            process.destroyForcibly();
            throw new RuntimeException("Test móvil excedió timeout de 90 segundos");
        }

        // Esperar threads
        outputThread.join(2000);
        errorThread.join(2000);

        int exitCode = process.exitValue();
        LOG.info("🏁 Test terminado con código: " + exitCode);

        if (exitCode != 0) {
            String errorMsg = String.format("Test móvil falló con código %d:\n📤 STDOUT: %s\n📥 STDERR: %s",
                    exitCode, output.toString(), errors.toString());
            throw new RuntimeException(errorMsg);
        }

        return findMobileGoldenFile(projectDir, widget, device);
    }

    /**
     * Busca archivo golden móvil generado
     */
    private Path findMobileGoldenFile(Path projectDir, WidgetNode widget, MobilePreviewGenerator.MobileDevice device) {
        String goldenFileName = widget.getClassName().toLowerCase() + "_mobile_" + device.name().toLowerCase() + ".png";

        Path[] possibleLocations = {
                projectDir.resolve("test/goldens").resolve(goldenFileName),
                projectDir.resolve("test").resolve(goldenFileName),
                projectDir.resolve("goldens").resolve(goldenFileName),
        };

        for (Path location : possibleLocations) {
            if (Files.exists(location)) {
                LOG.info("✅ Golden móvil encontrado: " + location);
                return location;
            }
        }

        // Buscar cualquier PNG como fallback
        try {
            return Files.walk(projectDir.resolve("test"))
                    .filter(Files::isRegularFile)
                    .filter(f -> f.toString().toLowerCase().endsWith(".png"))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("No se encontró golden móvil: " + goldenFileName));
        } catch (Exception e) {
            throw new RuntimeException("No se encontró golden móvil: " + goldenFileName);
        }
    }

    // ... (resto de métodos como postProcessMobileImage, etc. - mantener los existentes)

    /**
     * Post-procesa la imagen para mejorar apariencia móvil
     */
    private BufferedImage postProcessMobileImage(BufferedImage rawImage, MobilePreviewGenerator.MobileDevice device) {
        try {
            int targetWidth = device.width;
            int targetHeight = device.height;

            BufferedImage scaledImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = scaledImage.createGraphics();

            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            if (device.platform.equals("iOS")) {
                g2d.setColor(new Color(0xF2, 0xF2, 0xF7));
            } else {
                g2d.setColor(new Color(0xFF, 0xFF, 0xFF));
            }
            g2d.fillRect(0, 0, targetWidth, targetHeight);

            g2d.drawImage(rawImage, 0, 0, targetWidth, targetHeight, null);

            if (device.platform.equals("iOS")) {
                g2d.setColor(new Color(0, 0, 0, 20));
                g2d.setStroke(new BasicStroke(1.0f));
                g2d.drawRoundRect(2, 2, targetWidth-4, targetHeight-4, 25, 25);
            } else {
                g2d.setColor(new Color(0, 0, 0, 10));
                g2d.setStroke(new BasicStroke(0.5f));
                g2d.drawRoundRect(1, 1, targetWidth-2, targetHeight-2, 12, 12);
            }

            g2d.dispose();
            return scaledImage;

        } catch (Exception e) {
            LOG.warn("Error en post-procesamiento móvil, retornando imagen original", e);
            return rawImage;
        }
    }

    private String removeImportsFromCode(String sourceCode) {
        return sourceCode.replaceAll("^import\\s+[^;]+;\\s*\n?", "").trim();
    }

    private String generateMobileCacheKey(WidgetNode widget, MobilePreviewGenerator.MobileDevice device) {
        String content = widget.getSourceCode() + "_" + device.name() + "_" + device.width + "x" + device.height;
        return String.valueOf(Math.abs(content.hashCode())).substring(0, 10);
    }

    private void deleteDirectory(Path dir) throws Exception {
        if (Files.exists(dir)) {
            Files.walk(dir)
                    .sorted(java.util.Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(java.io.File::delete);
        }
    }

    private String getFlutterExecutablePath() {
        String sdkPath = processManager.getFlutterSdkPath();
        if (sdkPath == null) {
            throw new RuntimeException("Flutter SDK no detectado");
        }

        if (System.getProperty("os.name").toLowerCase().contains("windows")) {
            return Paths.get(sdkPath, "bin", "flutter.bat").toString();
        } else {
            return Paths.get(sdkPath, "bin", "flutter").toString();
        }
    }

    public void renderMobileWidgetWithProgress(WidgetNode widget, MobileRenderCallback callback) {
        renderMobileWidgetWithProgress(widget, MobilePreviewGenerator.MobileDevice.PIXEL_7, callback);
    }

    public void renderMobileWidgetWithProgress(WidgetNode widget, MobilePreviewGenerator.MobileDevice device, MobileRenderCallback callback) {
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Renderizando móvil " + widget.getClassName(), true) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                try {
                    indicator.setText("Configurando dispositivo móvil...");
                    indicator.setFraction(0.1);

                    BufferedImage image = renderWidgetMobile(widget, device);

                    indicator.setFraction(1.0);
                    callback.onSuccess(image);

                } catch (Exception e) {
                    LOG.error("Error en renderizado móvil", e);
                    callback.onError(e);
                }
            }
        });
    }

    public void clearMobileCache() {
        MOBILE_CACHE.clear();
        LOG.info("Caché móvil limpiado");
    }

    public String getMobileCacheStats() {
        return String.format("Mobile cache: %d images", MOBILE_CACHE.size());
    }

    public interface MobileRenderCallback {
        void onSuccess(BufferedImage image);
        void onError(Exception error);
    }
}