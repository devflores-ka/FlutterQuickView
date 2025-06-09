package com.github.devflores_ka.flutterquickview.renderer;

import com.github.devflores_ka.flutterquickview.analyzer.models.WidgetNode;
import com.github.devflores_ka.flutterquickview.generator.MobilePreviewGenerator;
import com.github.devflores_ka.flutterquickview.generator.SmartCompatibilityManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Renderizador ultra-adaptativo que combina todos los sistemas inteligentes
 * para máxima compatibilidad y auto-reparación automática
 */
public class UltraAdaptiveFlutterRenderer {
    private static final Logger LOG = Logger.getInstance(UltraAdaptiveFlutterRenderer.class);

    // Cache global inteligente
    private static final ConcurrentMap<String, BufferedImage> ULTRA_CACHE = new ConcurrentHashMap<>();
    private static final ConcurrentMap<String, SuccessfulStrategy> STRATEGY_CACHE = new ConcurrentHashMap<>();

    private final Project project;
    private final FlutterProcessManager processManager;
    private final SmartCompatibilityManager compatibilityManager;
    private final AdaptiveErrorHandler errorHandler;

    public UltraAdaptiveFlutterRenderer(Project project) {
        this.project = project;
        this.processManager = new FlutterProcessManager(project);
        this.compatibilityManager = new SmartCompatibilityManager(project);
        this.errorHandler = new AdaptiveErrorHandler(project);

        LOG.info("🚀 UltraAdaptiveFlutterRenderer inicializado");
        LOG.info("📊 " + compatibilityManager.getCompatibilityStats());
    }

    /**
     * Renderiza widget con máxima adaptabilidad y auto-reparación
     */
    public void renderWidgetUltraAdaptive(WidgetNode widget, MobilePreviewGenerator.MobileDevice device,
                                          UltraAdaptiveCallback callback) {

        ProgressManager.getInstance().run(new Task.Backgroundable(project,
                "🧠 Renderizado Ultra-Adaptativo: " + widget.getClassName(), true) {

            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setText("🔍 Analizando compatibilidad del proyecto...");
                indicator.setFraction(0.05);

                try {
                    String cacheKey = generateUltraCacheKey(widget, device);

                    // Verificar cache primero
                    BufferedImage cached = ULTRA_CACHE.get(cacheKey);
                    if (cached != null) {
                        LOG.info("✅ Imagen encontrada en cache ultra-adaptativo");
                        callback.onSuccess(cached, "Cache hit - renderizado previo exitoso");
                        return;
                    }

                    // Verificar estrategia exitosa previa
                    SuccessfulStrategy previousStrategy = STRATEGY_CACHE.get(cacheKey);
                    if (previousStrategy != null) {
                        LOG.info("🎯 Usando estrategia exitosa previa: " + previousStrategy.description);
                        indicator.setText("🎯 Aplicando estrategia aprendida...");
                        indicator.setFraction(0.15);

                        if (applyLearnedStrategy(widget, device, previousStrategy, indicator, callback)) {
                            return; // Éxito con estrategia aprendida
                        }
                    }

                    // Ejecutar renderizado ultra-adaptativo completo
                    executeUltraAdaptiveRendering(widget, device, indicator, callback, cacheKey);

                } catch (Exception e) {
                    LOG.error("❌ Error crítico en renderizado ultra-adaptativo", e);
                    callback.onError(e, "Error crítico en el sistema adaptativo");
                }
            }
        });
    }

    /**
     * Ejecuta el proceso completo de renderizado ultra-adaptativo
     */
    private void executeUltraAdaptiveRendering(WidgetNode widget, MobilePreviewGenerator.MobileDevice device,
                                               ProgressIndicator indicator, UltraAdaptiveCallback callback,
                                               String cacheKey) {

        indicator.setText("🧠 Obteniendo estrategia de compatibilidad óptima...");
        indicator.setFraction(0.25);

        // Obtener la mejor estrategia según el análisis del proyecto
        SmartCompatibilityManager.CompatibilityStrategy strategy = compatibilityManager.getBestStrategy(device);

        LOG.info("📋 Estrategia seleccionada: " + strategy.description +
                " (probabilidad de éxito: " + strategy.successProbability + "%)");

        indicator.setText("📋 " + strategy.description);
        indicator.setFraction(0.35);

        // Intentar con la estrategia principal
        RenderAttemptResult result = attemptRenderingWithStrategy(widget, device, strategy, indicator);

        if (result.success) {
            handleSuccessfulRendering(result, widget, device, strategy, callback, cacheKey);
            return;
        }

        // Si falla, activar auto-reparación en cascada
        indicator.setText("🔧 Activando sistema de auto-reparación...");
        indicator.setFraction(0.50);

        executeAutoRepairCascade(widget, device, result.error, strategy.pubspecContent,
                indicator, callback, cacheKey);
    }

    /**
     * Intenta renderizado con una estrategia específica
     */
    private RenderAttemptResult attemptRenderingWithStrategy(WidgetNode widget,
                                                             MobilePreviewGenerator.MobileDevice device,
                                                             SmartCompatibilityManager.CompatibilityStrategy strategy,
                                                             ProgressIndicator indicator) {
        try {
            LOG.info("🎯 Intentando renderizado con estrategia: " + strategy.type);

            // Crear proyecto temporal con la estrategia
            Path projectDir = createProjectWithStrategy(widget, device, strategy);

            // Ejecutar test
            Path outputImage = executeTestWithStrategy(projectDir, widget, device, strategy);

            // Cargar imagen
            BufferedImage image = javax.imageio.ImageIO.read(outputImage.toFile());

            return new RenderAttemptResult(true, image, null, strategy);

        } catch (Exception e) {
            LOG.warn("⚠️ Estrategia " + strategy.type + " falló: " + e.getMessage());
            return new RenderAttemptResult(false, null, e, strategy);
        }
    }

    /**
     * Ejecuta cascada de auto-reparación cuando la estrategia principal falla
     */
    private void executeAutoRepairCascade(WidgetNode widget, MobilePreviewGenerator.MobileDevice device,
                                          Exception originalError, String originalPubspec,
                                          ProgressIndicator indicator, UltraAdaptiveCallback callback,
                                          String cacheKey) {

        LOG.info("🔧 Iniciando cascada de auto-reparación...");

        // Generar estrategias de reparación en cascada
        List<AdaptiveErrorHandler.AutoRepairSuggestion> repairStrategies =
                errorHandler.generateCascadeRepairStrategy(originalError.getMessage(), originalPubspec);

        LOG.info("🎯 Generadas " + repairStrategies.size() + " estrategias de auto-reparación");

        for (int i = 0; i < repairStrategies.size(); i++) {
            AdaptiveErrorHandler.AutoRepairSuggestion repair = repairStrategies.get(i);

            indicator.setText("🔧 Auto-reparación " + (i + 1) + "/" + repairStrategies.size() + ": " + repair.description);
            indicator.setFraction(0.60 + (i * 0.10));

            LOG.info("🔧 Intentando auto-reparación: " + repair.description +
                    " (probabilidad: " + repair.successProbability + "%)");

            try {
                RenderAttemptResult repairResult = attemptRenderingWithRepair(widget, device, repair);

                if (repairResult.success) {
                    LOG.info("✅ Auto-reparación exitosa con: " + repair.description);

                    // Guardar estrategia exitosa para futuros usos
                    SuccessfulStrategy successStrategy = new SuccessfulStrategy(
                            repair.type.name(),
                            repair.description,
                            repair.repairedPubspec,
                            System.currentTimeMillis()
                    );
                    STRATEGY_CACHE.put(cacheKey, successStrategy);

                    handleSuccessfulRendering(repairResult, widget, device, null, callback, cacheKey);
                    return;
                }

            } catch (Exception e) {
                LOG.warn("⚠️ Auto-reparación " + repair.type + " falló: " + e.getMessage());
            }
        }

        // Si todas las reparaciones fallan, reportar error final
        String errorMessage = "Todas las estrategias de auto-reparación fallaron. " +
                "Errores encontrados: " + originalError.getMessage();

        LOG.error("❌ Cascada de auto-reparación agotada");
        callback.onError(new RuntimeException(errorMessage),
                "Sistema de auto-reparación agotado - revisa las dependencias del proyecto");
    }

    /**
     * Intenta renderizado con una sugerencia de auto-reparación
     */
    private RenderAttemptResult attemptRenderingWithRepair(WidgetNode widget,
                                                           MobilePreviewGenerator.MobileDevice device,
                                                           AdaptiveErrorHandler.AutoRepairSuggestion repair) throws Exception {

        // Crear estrategia temporal con el pubspec reparado
        SmartCompatibilityManager.CompatibilityStrategy repairStrategy =
                new SmartCompatibilityManager.CompatibilityStrategy(
                        SmartCompatibilityManager.StrategyType.MINIMAL_SAFE,
                        repair.repairedPubspec,
                        repair.description,
                        repair.successProbability
                );

        return attemptRenderingWithStrategy(widget, device, repairStrategy, null);
    }

    /**
     * Maneja renderizado exitoso
     */
    private void handleSuccessfulRendering(RenderAttemptResult result, WidgetNode widget,
                                           MobilePreviewGenerator.MobileDevice device,
                                           SmartCompatibilityManager.CompatibilityStrategy strategy,
                                           UltraAdaptiveCallback callback, String cacheKey) {

        // Cachear imagen exitosa
        ULTRA_CACHE.put(cacheKey, result.image);

        // Post-procesar imagen si es necesario
        BufferedImage finalImage = postProcessUltraImage(result.image, device);

        String successMessage = strategy != null ?
                "Renderizado exitoso con estrategia: " + strategy.description :
                "Renderizado exitoso con auto-reparación";

        LOG.info("✅ " + successMessage);
        callback.onSuccess(finalImage, successMessage);
    }

    /**
     * Aplica estrategia aprendida de intentos previos exitosos
     */
    private boolean applyLearnedStrategy(WidgetNode widget, MobilePreviewGenerator.MobileDevice device,
                                         SuccessfulStrategy strategy, ProgressIndicator indicator,
                                         UltraAdaptiveCallback callback) {
        try {
            indicator.setText("🎯 Aplicando estrategia aprendida: " + strategy.description);

            // Crear estrategia temporal con configuración aprendida
            SmartCompatibilityManager.CompatibilityStrategy learnedStrategy =
                    new SmartCompatibilityManager.CompatibilityStrategy(
                            SmartCompatibilityManager.StrategyType.SELECTIVE_COMPATIBILITY,
                            strategy.pubspecContent,
                            strategy.description,
                            95 // Alta probabilidad para estrategias aprendidas
                    );

            RenderAttemptResult result = attemptRenderingWithStrategy(widget, device, learnedStrategy, indicator);

            if (result.success) {
                String cacheKey = generateUltraCacheKey(widget, device);
                handleSuccessfulRendering(result, widget, device, learnedStrategy, callback, cacheKey);
                return true;
            }

        } catch (Exception e) {
            LOG.warn("⚠️ Estrategia aprendida falló, continuando con análisis completo: " + e.getMessage());
        }

        return false;
    }

    // Métodos auxiliares

    private Path createProjectWithStrategy(WidgetNode widget, MobilePreviewGenerator.MobileDevice device,
                                           SmartCompatibilityManager.CompatibilityStrategy strategy) throws Exception {
        String cacheKey = generateUltraCacheKey(widget, device);
        Path projectDir = Paths.get(project.getBasePath(), ".flutter_quick_view", "ultra_adaptive",
                device.name().toLowerCase(), "project_" + cacheKey);

        if (Files.exists(projectDir)) {
            deleteDirectory(projectDir);
        }

        Files.createDirectories(projectDir);
        Files.createDirectories(projectDir.resolve("lib"));
        Files.createDirectories(projectDir.resolve("test"));
        Files.createDirectories(projectDir.resolve("test/goldens"));

        // Escribir pubspec de la estrategia
        Files.write(projectDir.resolve("pubspec.yaml"), strategy.pubspecContent.getBytes());

        // Crear analysis_options ultra-permisivo
        createUltraPermissiveAnalysisOptions(projectDir);

        return projectDir;
    }

    private void createUltraPermissiveAnalysisOptions(Path projectDir) throws Exception {
        String analysisContent = """
            # Ultra-permissive analysis options for maximum compatibility
            analyzer:
              exclude:
                - "**/*.g.dart"
                - "**/*.freezed.dart"
                - "**/*.mocks.dart"
              errors:
                # Ignore all possible warnings/errors
                missing_required_param: ignore
                missing_return: ignore
                unused_import: ignore
                unnecessary_import: ignore
                unused_field: ignore
                unused_local_variable: ignore
                prefer_const_constructors: ignore
                use_key_in_widget_constructors: ignore
                library_private_types_in_public_api: ignore
                invalid_annotation_target: ignore
                deprecated_member_use: ignore
              language:
                strict-casts: false
                strict-inference: false
                strict-raw-types: false
            
            linter:
              rules: {} # No linting rules for ultra compatibility
            """;

        Files.write(projectDir.resolve("analysis_options.yaml"), analysisContent.getBytes());
    }

    private Path executeTestWithStrategy(Path projectDir, WidgetNode widget,
                                         MobilePreviewGenerator.MobileDevice device,
                                         SmartCompatibilityManager.CompatibilityStrategy strategy) throws Exception {

        // Generar test ultra-simple
        String testContent = generateUltraSimpleTest(widget, device);
        String testFileName = widget.getClassName().toLowerCase() + "_ultra_test.dart";
        Files.write(projectDir.resolve("test").resolve(testFileName), testContent.getBytes());

        // Ejecutar pub get primero
        executeUltraPubGet(projectDir);

        // Ejecutar test
        return executeUltraTest(projectDir, testFileName, widget, device);
    }

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

    private void executeUltraPubGet(Path projectDir) throws Exception {
        String flutterExecutable = getFlutterExecutablePath();
        ProcessBuilder pb = new ProcessBuilder(flutterExecutable, "pub", "get");
        pb.directory(projectDir.toFile());

        Process process = pb.start();
        boolean finished = process.waitFor(45, java.util.concurrent.TimeUnit.SECONDS);

        if (!finished) {
            process.destroyForcibly();
            throw new RuntimeException("Ultra pub get timeout");
        }

        if (process.exitValue() != 0) {
            throw new RuntimeException("Ultra pub get failed with code: " + process.exitValue());
        }
    }

    private Path executeUltraTest(Path projectDir, String testFileName, WidgetNode widget,
                                  MobilePreviewGenerator.MobileDevice device) throws Exception {

        String flutterExecutable = getFlutterExecutablePath();
        ProcessBuilder pb = new ProcessBuilder(
                flutterExecutable, "test", "test/" + testFileName, "--update-goldens"
        );
        pb.directory(projectDir.toFile());

        Process process = pb.start();
        boolean finished = process.waitFor(60, java.util.concurrent.TimeUnit.SECONDS);

        if (!finished) {
            process.destroyForcibly();
            throw new RuntimeException("Ultra test timeout");
        }

        if (process.exitValue() != 0) {
            throw new RuntimeException("Ultra test failed with code: " + process.exitValue());
        }

        // Buscar golden file
        String goldenFileName = widget.getClassName().toLowerCase() + "_ultra.png";
        Path goldenFile = projectDir.resolve("test/goldens").resolve(goldenFileName);

        if (!Files.exists(goldenFile)) {
            goldenFile = projectDir.resolve("test").resolve(goldenFileName);
        }

        if (!Files.exists(goldenFile)) {
            throw new RuntimeException("Ultra golden file not found: " + goldenFileName);
        }

        return goldenFile;
    }

    private BufferedImage postProcessUltraImage(BufferedImage image, MobilePreviewGenerator.MobileDevice device) {
        // Post-procesamiento mejorado aquí si es necesario
        return image;
    }

    private String generateUltraCacheKey(WidgetNode widget, MobilePreviewGenerator.MobileDevice device) {
        String content = widget.getSourceCode() + "_ultra_" + device.name() + "_" + device.width + "x" + device.height;
        return String.valueOf(Math.abs(content.hashCode())).substring(0, 12);
    }

    private String removeImportsFromCode(String sourceCode) {
        return sourceCode.replaceAll("^import\\s+[^;]+;\\s*\n?", "").trim();
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

    /**
     * Limpia todos los caches ultra-adaptativos
     */
    public void clearUltraCache() {
        ULTRA_CACHE.clear();
        STRATEGY_CACHE.clear();
        LOG.info("🧹 Cache ultra-adaptativo limpiado");
    }

    /**
     * Obtiene estadísticas del sistema ultra-adaptativo
     */
    public String getUltraStats() {
        return String.format(
                "Ultra Cache: %d images, %d estrategias aprendidas, %s",
                ULTRA_CACHE.size(),
                STRATEGY_CACHE.size(),
                compatibilityManager.getCompatibilityStats()
        );
    }

    /**
     * Versión simplificada para renderizado rápido
     */
    public void renderWidgetQuickAdaptive(WidgetNode widget, UltraAdaptiveCallback callback) {
        renderWidgetUltraAdaptive(widget, MobilePreviewGenerator.MobileDevice.PIXEL_7, callback);
    }

    // Clases de datos para resultados

    private static class RenderAttemptResult {
        final boolean success;
        final BufferedImage image;
        final Exception error;
        final SmartCompatibilityManager.CompatibilityStrategy strategy;

        RenderAttemptResult(boolean success, BufferedImage image, Exception error,
                            SmartCompatibilityManager.CompatibilityStrategy strategy) {
            this.success = success;
            this.image = image;
            this.error = error;
            this.strategy = strategy;
        }
    }

    private static class SuccessfulStrategy {
        final String strategyType;
        final String description;
        final String pubspecContent;
        final long timestamp;

        SuccessfulStrategy(String strategyType, String description, String pubspecContent, long timestamp) {
            this.strategyType = strategyType;
            this.description = description;
            this.pubspecContent = pubspecContent;
            this.timestamp = timestamp;
        }
    }

    /**
     * Interface para callbacks del renderizado ultra-adaptativo
     */
    public interface UltraAdaptiveCallback {
        void onSuccess(BufferedImage image, String strategyDescription);
        void onError(Exception error, String context);
    }
}