package com.github.devflores_ka.flutterquickview.renderer;

import com.github.devflores_ka.flutterquickview.analyzer.models.WidgetNode;
import com.github.devflores_ka.flutterquickview.generator.MobilePreviewGenerator;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Servicio optimizado para renderizar widgets Flutter usando Golden Tests
 * VERSIÓN CORREGIDA para encontrar archivos golden
 */
@Service(Service.Level.PROJECT)
public final class FlutterRendererService {
    private static final Logger LOG = Logger.getInstance(FlutterRendererService.class);

    // Cache de imágenes renderizadas por hash del código
    private static final ConcurrentMap<String, BufferedImage> PREVIEW_CACHE = new ConcurrentHashMap<>();

    // Cache de archivos temporales para evitar regeneración innecesaria
    private static final ConcurrentMap<String, Path> TEMP_FILE_CACHE = new ConcurrentHashMap<>();

    private final Project project;
    private final FlutterProcessManager processManager;

    // Configuración optimizada
    private static final int PREVIEW_WIDTH = 375;
    private static final int PREVIEW_HEIGHT = 667;
    private static final int TIMEOUT_SECONDS = 180;

    public FlutterRendererService(Project project, FlutterMobileRenderer mobileRenderer) {
        this.project = project;
        this.processManager = new FlutterProcessManager(project);
        this.mobileRenderer = mobileRenderer;
    }

    public static FlutterRendererService getInstance(Project project) {
        return project.getService(FlutterRendererService.class);
    }

    /**
     * Renderiza un widget con progreso visible en la UI
     */
    public void renderWidgetWithProgress(WidgetNode widget, RenderCallback callback) {
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Renderizando " + widget.getClassName(), true) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setText("Preparando renderizado...");
                indicator.setFraction(0.1);

                try {
                    // Verificar cache
                    String cacheKey = generateCacheKey(widget);
                    BufferedImage cached = PREVIEW_CACHE.get(cacheKey);
                    if (cached != null) {
                        LOG.info("Imagen encontrada en cache para: " + widget.getClassName());
                        callback.onSuccess(cached);
                        return;
                    }

                    indicator.setText("Generando archivo de test...");
                    indicator.setFraction(0.3);

                    // Crear directorio base para el proyecto
                    Path projectDir = createTempFlutterProject(cacheKey);
                    LOG.info("Proyecto temporal creado en: " + projectDir);

                    indicator.setText("Configurando proyecto Flutter...");
                    indicator.setFraction(0.4);

                    // Generar archivo de test
                    Path testFile = generateTestInProject(widget, projectDir);
                    LOG.info("Archivo de test generado: " + testFile);

                    indicator.setText("Ejecutando pub get...");
                    indicator.setFraction(0.5);

                    // Ejecutar pub get en el proyecto
                    executePubGet(projectDir);

                    indicator.setText("Ejecutando Flutter test...");
                    indicator.setFraction(0.7);

                    // Ejecutar test desde el directorio del proyecto
                    Path outputImage = executeFlutterTestInProject(projectDir, widget.getClassName());
                    LOG.info("Test completado, buscando imagen en: " + outputImage);

                    indicator.setText("Cargando imagen...");
                    indicator.setFraction(0.9);

                    // Cargar y cachear
                    BufferedImage result = ImageIO.read(outputImage.toFile());
                    PREVIEW_CACHE.put(cacheKey, result);

                    LOG.info("Renderizado exitoso para: " + widget.getClassName() +
                            " - Tamaño: " + result.getWidth() + "x" + result.getHeight());

                    indicator.setFraction(1.0);
                    callback.onSuccess(result);

                } catch (Exception e) {
                    LOG.error("Error en renderizado con progreso para " + widget.getClassName(), e);
                    callback.onError(e);
                }
            }
        });
    }

    /**
     * Crea un proyecto Flutter temporal completo
     */
    private Path createTempFlutterProject(String cacheKey) throws Exception {
        Path tempDir = Paths.get(project.getBasePath(), ".flutter_quick_view", "projects", "project_" + cacheKey);

        // Si ya existe, eliminarlo para empezar limpio
        if (Files.exists(tempDir)) {
            deleteDirectory(tempDir);
        }

        Files.createDirectories(tempDir);
        Files.createDirectories(tempDir.resolve("lib"));
        Files.createDirectories(tempDir.resolve("test"));
        Files.createDirectories(tempDir.resolve("test/goldens"));

        // Crear pubspec.yaml
        String pubspecContent = """
            name: flutter_quick_preview
            description: Temporary Flutter project for quick previews
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
              flutter_lints: ^3.0.0
            
            flutter:
              uses-material-design: true
            """;

        Files.write(tempDir.resolve("pubspec.yaml"), pubspecContent.getBytes());

        // Crear analysis_options.yaml
        String analysisOptions = """
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

        Files.write(tempDir.resolve("analysis_options.yaml"), analysisOptions.getBytes());

        return tempDir;
    }

    /**
     * Ejecuta pub get en el proyecto usando FlutterProcessManager
     */
    private void executePubGet(Path projectDir) throws Exception {
        LOG.info("Ejecutando pub get en: " + projectDir);

        // Cambiar temporalmente el directorio base del proyecto para ejecutar pub get
        String originalBasePath = project.getBasePath();

        try {
            // Crear un ProcessBuilder que use el directorio correcto
            String[] command = {"flutter", "pub", "get"};

            // Usar FlutterProcessManager pero configurar el directorio de trabajo
            FlutterProcessManager.ProcessResult result = executeFlutterCommandInDirectory(command, projectDir, 60);

            if (!result.isSuccess()) {
                throw new RuntimeException("pub get failed: " + result.error());
            }

            LOG.info("pub get completado exitosamente");
            LOG.info("pub get output: " + result.output());

        } catch (Exception e) {
            LOG.error("Error en pub get", e);
            throw e;
        }
    }

    /**
     * Genera el archivo de test dentro del proyecto Flutter
     */
    private Path generateTestInProject(WidgetNode widget, Path projectDir) throws IOException {
        Path testFile = projectDir.resolve("test").resolve(widget.getClassName().toLowerCase() + "_test.dart");

        String testContent = generateOptimizedTestContent(widget);
        Files.write(testFile, testContent.getBytes());

        return testFile;
    }

    /**
     * Ejecuta el test Flutter dentro del proyecto usando FlutterProcessManager
     */
    private Path executeFlutterTestInProject(Path projectDir, String widgetName) throws Exception {
        String testFileName = widgetName.toLowerCase() + "_test.dart";

        LOG.info("Ejecutando test: " + testFileName + " en proyecto: " + projectDir);

        // Ejecutar el test específico con update-goldens
        String[] command = {
                "flutter", "test",
                "test/" + testFileName,
                "--update-goldens"
        };

        // Usar FlutterProcessManager con directorio personalizado
        FlutterProcessManager.ProcessResult result = executeFlutterCommandInDirectory(command, projectDir, 180);

        LOG.info("Flutter test terminado con código: " + result.exitCode());
        LOG.info("Flutter test output: " + result.output());

        if (!result.isSuccess()) {
            LOG.error("Flutter test stderr: " + result.error());
        }

        // Listar todos los archivos generados para debugging
        LOG.info("=== LISTADO DE ARCHIVOS GENERADOS ===");
        listDirectoryContents(projectDir, "");

        // Buscar el golden file generado
        Path goldenFile = findGoldenFileInProject(projectDir, widgetName);

        if (!Files.exists(goldenFile)) {
            throw new RuntimeException("Golden file no generado: " + goldenFile +
                    "\nExitCode: " + result.exitCode() +
                    "\nSTDOUT: " + result.output() +
                    "\nSTDERR: " + result.error());
        }

        LOG.info("Golden file encontrado: " + goldenFile);
        return goldenFile;
    }

    /**
     * Ejecuta un comando Flutter en un directorio específico usando FlutterProcessManager
     */
    private FlutterProcessManager.ProcessResult executeFlutterCommandInDirectory(
            String[] command, Path workingDirectory, int timeoutSeconds) throws Exception {

        // Verificar que Flutter esté disponible
        if (!processManager.isFlutterAvailable()) {
            throw new RuntimeException("Flutter no está disponible. SDK Path: " + processManager.getFlutterSdkPath());
        }

        LOG.info("Ejecutando comando: " + String.join(" ", command) + " en directorio: " + workingDirectory);
        LOG.info("Flutter SDK detectado en: " + processManager.getFlutterSdkPath());

        // Crear ProcessBuilder personalizado que use la ruta completa de Flutter
        ProcessBuilder pb = new ProcessBuilder();

        // Si tenemos la ruta del SDK, usar el ejecutable completo
        String flutterExecutable = processManager.getFlutterSdkPath() != null
                ? Paths.get(processManager.getFlutterSdkPath(), "bin",
                System.getProperty("os.name").toLowerCase().contains("windows") ? "flutter.bat" : "flutter").toString()
                : "flutter";

        // Reemplazar el primer elemento (flutter) con la ruta completa
        String[] fullCommand = new String[command.length];
        fullCommand[0] = flutterExecutable;
        System.arraycopy(command, 1, fullCommand, 1, command.length - 1);

        pb.command(fullCommand);
        pb.directory(workingDirectory.toFile());

        LOG.info("Comando completo: " + String.join(" ", fullCommand));

        long startTime = System.currentTimeMillis();
        Process process = pb.start();

        // Capturar salida y errores
        StringBuilder output = new StringBuilder();
        StringBuilder errors = new StringBuilder();

        // Leer stdout
        Thread outputThread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                    LOG.info("Flutter OUT: " + line);
                }
            } catch (IOException e) {
                LOG.warn("Error leyendo stdout", e);
            }
        });

        // Leer stderr
        Thread errorThread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    errors.append(line).append("\n");
                    LOG.warn("Flutter ERR: " + line);
                }
            } catch (IOException e) {
                LOG.warn("Error leyendo stderr", e);
            }
        });

        outputThread.start();
        errorThread.start();

        // Esperar con timeout
        boolean finished = process.waitFor(timeoutSeconds, java.util.concurrent.TimeUnit.SECONDS);

        if (!finished) {
            process.destroyForcibly();
            throw new RuntimeException("Comando Flutter excedió timeout de " + timeoutSeconds + " segundos");
        }

        // Esperar a que terminen los threads de lectura
        outputThread.join(2000);
        errorThread.join(2000);

        long executionTime = System.currentTimeMillis() - startTime;
        int exitCode = process.exitValue();

        return new FlutterProcessManager.ProcessResult(exitCode, output.toString(), errors.toString(), executionTime);
    }

    /**
     * Lista el contenido de directorios para debugging
     */
    private void listDirectoryContents(Path dir, String indent) {
        try {
            if (Files.exists(dir) && Files.isDirectory(dir)) {
                LOG.info(indent + "📁 " + dir.getFileName() + "/");
                Files.list(dir).forEach(file -> {
                    if (Files.isDirectory(file)) {
                        listDirectoryContents(file, indent + "  ");
                    } else {
                        LOG.info(indent + "  📄 " + file.getFileName() + " (" + getFileSize(file) + ")");
                    }
                });
            }
        } catch (Exception e) {
            LOG.warn("Error listando directorio: " + dir, e);
        }
    }

    private String getFileSize(Path file) {
        try {
            long size = Files.size(file);
            if (size < 1024) return size + " B";
            if (size < 1024 * 1024) return (size / 1024) + " KB";
            return (size / (1024 * 1024)) + " MB";
        } catch (Exception e) {
            return "? B";
        }
    }

    /**
     * Busca el archivo golden en todas las ubicaciones posibles
     */
    private Path findGoldenFileInProject(Path projectDir, String widgetName) {
        String goldenFileName = widgetName.toLowerCase() + "_preview.png";

        // Ubicaciones posibles donde Flutter genera los golden files
        Path[] possibleLocations = {
                projectDir.resolve("test/goldens").resolve(goldenFileName),
                projectDir.resolve("test").resolve(goldenFileName),
                projectDir.resolve("goldens").resolve(goldenFileName),
                projectDir.resolve(goldenFileName)
        };

        for (Path location : possibleLocations) {
            LOG.info("Buscando golden file en: " + location);
            if (Files.exists(location)) {
                LOG.info("✅ Archivo golden encontrado en: " + location);
                return location;
            }
        }

        // Si no encuentra nada, buscar cualquier archivo PNG en test/
        try {
            Path testDir = projectDir.resolve("test");
            if (Files.exists(testDir)) {
                LOG.info("Buscando archivos PNG en directorio test/...");
                Files.walk(testDir)
                        .filter(Files::isRegularFile)
                        .filter(f -> f.toString().toLowerCase().endsWith(".png"))
                        .forEach(f -> LOG.info("🖼️ PNG encontrado: " + f));

                // Retornar el primer PNG encontrado
                return Files.walk(testDir)
                        .filter(Files::isRegularFile)
                        .filter(f -> f.toString().toLowerCase().endsWith(".png"))
                        .findFirst()
                        .orElse(possibleLocations[0]); // fallback
            }
        } catch (Exception e) {
            LOG.warn("Error buscando archivos PNG", e);
        }

        // Fallback a la primera ubicación
        return possibleLocations[0];
    }

    /**
     * Elimina un directorio recursivamente
     */
    private void deleteDirectory(Path dir) throws IOException {
        if (Files.exists(dir)) {
            Files.walk(dir)
                    .sorted(java.util.Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        }
    }

    /**
     * Genera una clave de caché basada en el contenido del widget
     */
    private String generateCacheKey(WidgetNode widget) {
        try {
            String content = widget.getSourceCode() + "_" + PREVIEW_WIDTH + "x" + PREVIEW_HEIGHT;
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(content.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString().substring(0, 8); // Solo los primeros 8 caracteres
        } catch (Exception e) {
            return String.valueOf(Math.abs(widget.getSourceCode().hashCode()));
        }
    }

    /**
     * Genera el contenido optimizado del archivo de test
     */
    private String generateOptimizedTestContent(WidgetNode widget) {
        StringBuilder content = new StringBuilder();
        LOG.info("🔥 GENERANDO TEST PARA: " + widget.getClassName() + " - Es StatefulWidget: " + widget.getSourceCode().contains("StatefulWidget"));

        // Imports básicos optimizados
        content.append("import 'package:flutter/material.dart';\n");
        content.append("import 'package:flutter_test/flutter_test.dart';\n\n");

        // Incluir el código del widget CON todas las clases relacionadas
        String widgetCode = extractWidgetAndRelatedClasses(widget);
        content.append(widgetCode).append("\n\n");

        // Test optimizado
        content.append("void main() {\n");
        content.append("  testWidgets('").append(widget.getClassName()).append(" preview', (WidgetTester tester) async {\n");

        // Si el widget ya contiene MaterialApp, usarlo directamente
        boolean hasOwnMaterialApp = widgetCode.contains("MaterialApp");

        if (hasOwnMaterialApp) {
            content.append("    await tester.pumpWidget(\n");
            content.append("      Center(\n");
            content.append("        child: SizedBox(\n");
            content.append("          width: ").append(PREVIEW_WIDTH).append(",\n");
            content.append("          height: ").append(PREVIEW_HEIGHT).append(",\n");
            content.append("          child: ").append(widget.getClassName()).append("(),\n");
            content.append("        ),\n");
            content.append("      ),\n");
            content.append("    );\n");
        } else {
            content.append("    await tester.pumpWidget(\n");
            content.append("      MaterialApp(\n");
            content.append("        debugShowCheckedModeBanner: false,\n");
            content.append("        theme: ThemeData.light(),\n");
            content.append("        home: Scaffold(\n");
            content.append("          body: Center(\n");
            content.append("            child: SizedBox(\n");
            content.append("              width: ").append(PREVIEW_WIDTH).append(",\n");
            content.append("              height: ").append(PREVIEW_HEIGHT).append(",\n");
            content.append("              child: ").append(widget.getClassName()).append("(),\n");
            content.append("            ),\n");
            content.append("          ),\n");
            content.append("        ),\n");
            content.append("      ),\n");
            content.append("    );\n");
        }

        content.append("    \n");
        content.append("    // Esperar a que se estabilice la UI\n");
        content.append("    await tester.pumpAndSettle(Duration(milliseconds: 500));\n");
        content.append("    \n");
        content.append("    // Capturar como golden file\n");
        content.append("    await expectLater(\n");

        if (hasOwnMaterialApp) {
            content.append("      find.byType(").append(widget.getClassName()).append("),\n");
        } else {
            content.append("      find.byType(MaterialApp),\n");
        }

        content.append("      matchesGoldenFile('").append(widget.getClassName().toLowerCase()).append("_preview.png'),\n");
        content.append("    );\n");
        content.append("  });\n");
        content.append("}\n");

        return content.toString();
    }

    /**
     * Extrae el widget y todas las clases relacionadas (incluyendo clases privadas de State)
     */
    private String extractWidgetAndRelatedClasses(WidgetNode widget) {
        String sourceCode = widget.getSourceCode();
        LOG.info("🔥 EXTRAYENDO CLASES PARA: " + widget.getClassName());

        // LOG DE DEBUG - AGREGAR ESTOS
        LOG.info("=== DEBUG extractWidgetAndRelatedClasses ===");
        LOG.info("Widget: " + widget.getClassName());
        LOG.info("Source code length: " + sourceCode.length());
        LOG.info("Contains StatefulWidget: " + sourceCode.contains("StatefulWidget"));
        LOG.info("Source code preview: " + sourceCode.substring(0, Math.min(200, sourceCode.length())));

        // Si es StatefulWidget, necesitamos incluir también la clase State
        if (sourceCode.contains("StatefulWidget")) {
            LOG.info("Detectado StatefulWidget, buscando clase State...");

            // Buscar la clase State asociada
            String stateClassName = findStateClassName(sourceCode, widget.getClassName());
            LOG.info("State class encontrada: " + stateClassName);

            if (stateClassName != null) {
                // Extraer tanto el widget como su clase State
                String fullCode = extractWidgetWithStateClass(sourceCode, widget.getClassName(), stateClassName);
                LOG.info("Full code extracted length: " + fullCode.length());
                LOG.info("Full code preview: " + fullCode.substring(0, Math.min(300, fullCode.length())));

                String result = removeOnlyImports(fullCode);
                LOG.info("Final result length: " + result.length());
                return result;
            } else {
                LOG.warn("No se encontró clase State para: " + widget.getClassName());
            }
        }

        // Para StatelessWidget o si no encontramos la clase State, solo el widget
        LOG.info("Usando solo el widget (StatelessWidget o State no encontrado)");
        return removeOnlyImports(sourceCode);
    }

    /**
     * Encuentra el nombre de la clase State para un StatefulWidget
     */
    private String findStateClassName(String sourceCode, String widgetClassName) {
        LOG.info("=== DEBUG findStateClassName ===");
        LOG.info("Buscando State para widget: " + widgetClassName);

        // Buscar el patrón: State<WidgetName> createState() => _WidgetNameState();
        Pattern statePattern = Pattern.compile(
                "State<" + widgetClassName + ">\\s+createState\\(\\)\\s*=>\\s*([\\w]+)\\(\\);",
                Pattern.MULTILINE
        );

        java.util.regex.Matcher matcher = statePattern.matcher(sourceCode);
        if (matcher.find()) {
            String found = matcher.group(1);
            LOG.info("Patrón 1 encontrado: " + found);
            return found;
        } else {
            LOG.info("Patrón 1 NO encontrado");
        }

        // Patrón alternativo: buscar clase que extiende State<WidgetName>
        Pattern altPattern = Pattern.compile(
                "class\\s+([\\w]+)\\s+extends\\s+State<" + widgetClassName + ">",
                Pattern.MULTILINE
        );

        java.util.regex.Matcher altMatcher = altPattern.matcher(sourceCode);
        if (altMatcher.find()) {
            String found = altMatcher.group(1);
            LOG.info("Patrón 2 encontrado: " + found);
            return found;
        } else {
            LOG.info("Patrón 2 NO encontrado");
        }

        LOG.info("No se encontró ningún patrón State");
        return null;
    }

    /**
     * Extrae tanto el widget como su clase State completa
     */
    private String extractWidgetWithStateClass(String sourceCode, String widgetClassName, String stateClassName) {
        StringBuilder result = new StringBuilder();

        // Extraer la clase del widget
        String widgetClass = extractSingleClass(sourceCode, widgetClassName);
        if (widgetClass != null) {
            result.append(widgetClass).append("\n\n");
        }

        // Extraer la clase State
        String stateClass = extractSingleClass(sourceCode, stateClassName);
        if (stateClass != null) {
            result.append(stateClass).append("\n\n");
        }

        // Si no pudimos extraer las clases por separado, usar todo el código
        if (result.length() == 0) {
            result.append(sourceCode);
        }

        return result.toString();
    }

    /**
     * Extrae una clase específica del código fuente
     */
    private String extractSingleClass(String sourceCode, String className) {
        // Buscar el inicio de la clase
        Pattern classPattern = Pattern.compile(
                "class\\s+" + className + "\\s+extends\\s+[\\w<>]+\\s*\\{",
                Pattern.MULTILINE
        );

        java.util.regex.Matcher matcher = classPattern.matcher(sourceCode);
        if (!matcher.find()) {
            return null;
        }

        int classStart = matcher.start();
        int braceStart = matcher.end() - 1; // Posición de la '{'

        // Contar llaves para encontrar el final de la clase
        int braceCount = 1;
        int i = braceStart + 1;

        while (i < sourceCode.length() && braceCount > 0) {
            char c = sourceCode.charAt(i);
            if (c == '{') braceCount++;
            else if (c == '}') braceCount--;
            i++;
        }

        if (braceCount == 0) {
            return sourceCode.substring(classStart, i);
        }

        return null;
    }

    /**
     * Remueve SOLO los imports, preservando todas las clases
     */
    private String removeOnlyImports(String sourceCode) {
        // Patrón más específico que solo remueve líneas de import
        Pattern importPattern = Pattern.compile("^import\\s+[^;]+;\\s*\n?", Pattern.MULTILINE);
        return importPattern.matcher(sourceCode).replaceAll("").trim();
    }

    /**
     * Limpia el caché y archivos temporales
     */
    public void clearCache() {
        PREVIEW_CACHE.clear();
        TEMP_FILE_CACHE.clear();

        // Opcionalmente limpiar directorios temporales
        try {
            Path tempDir = Paths.get(project.getBasePath(), ".flutter_quick_view");
            if (Files.exists(tempDir)) {
                deleteDirectory(tempDir);
            }
        } catch (Exception e) {
            LOG.warn("Error limpiando directorios temporales", e);
        }

        LOG.info("Cache limpiado");
    }

    /**
     * Interface para callbacks de renderizado
     */
    public interface RenderCallback {
        void onSuccess(BufferedImage image);
        void onError(Exception error);
    }

    /**
     * Obtiene estadísticas del caché
     */
    public CacheStats getCacheStats() {
        return new CacheStats(PREVIEW_CACHE.size(), TEMP_FILE_CACHE.size());
    }

    public static class CacheStats {
        public final int cachedImages;
        public final int tempFiles;

        public CacheStats(int cachedImages, int tempFiles) {
            this.cachedImages = cachedImages;
            this.tempFiles = tempFiles;
        }

        @Override
        public String toString() {
            return String.format("Cache: %d images, %d temp files", cachedImages, tempFiles);
        }
    }

    // AGREGAR AL FINAL DE FlutterRendererService.java, después de la clase CacheStats:

    // ========== NUEVAS FUNCIONALIDADES MÓVILES ==========

    private final FlutterMobileRenderer mobileRenderer;

    // Constructor actualizado - REEMPLAZAR el constructor existente
    public FlutterRendererService(Project project) {
        this.project = project;
        this.processManager = new FlutterProcessManager(project);
        this.mobileRenderer = new FlutterMobileRenderer(project); // NUEVO
    }

    /**
     * NUEVO: Renderiza widget con apariencia móvil realista
     */
    public void renderWidgetMobileWithProgress(WidgetNode widget, MobilePreviewGenerator.MobileDevice device, RenderCallback callback) {
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Renderizando móvil " + widget.getClassName(), true) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                try {
                    indicator.setText("Configurando dispositivo móvil...");
                    indicator.setFraction(0.1);

                    // Usar el renderizador móvil específico
                    BufferedImage result = mobileRenderer.renderWidgetMobile(widget, device);

                    indicator.setFraction(1.0);
                    callback.onSuccess(result);

                } catch (Exception e) {
                    LOG.error("Error en renderizado móvil", e);
                    callback.onError(e);
                }
            }
        });
    }

    /**
     * NUEVO: Renderiza con dispositivo móvil por defecto (Pixel 7)
     */
    public void renderWidgetMobileWithProgress(WidgetNode widget, RenderCallback callback) {
        renderWidgetMobileWithProgress(widget, MobilePreviewGenerator.MobileDevice.PIXEL_7, callback);
    }

    /**
     * NUEVO: Obtiene el renderizador móvil
     */
    public FlutterMobileRenderer getMobileRenderer() {
        return mobileRenderer;
    }

    /**
     * NUEVO: Limpia cache móvil
     */
    public void clearMobileCache() {
        if (mobileRenderer != null) {
            mobileRenderer.clearMobileCache();
        }
    }

    /**
     * NUEVO: Obtiene estadísticas del cache móvil
     */
    public String getMobileCacheStats() {
        if (mobileRenderer != null) {
            return mobileRenderer.getMobileCacheStats();
        }
        return "Mobile cache: 0 images";
    }

}