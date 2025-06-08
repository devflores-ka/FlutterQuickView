package com.github.devflores_ka.flutterquickview.renderer;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.SystemInfo;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Gestiona la ejecución optimizada de procesos Flutter
 * Incluye detección automática de SDK y configuración de entorno
 */
public class FlutterProcessManager {
    private static final Logger LOG = Logger.getInstance(FlutterProcessManager.class);

    private final Project project;
    private String flutterSdkPath;
    private String flutterExecutable;

    public FlutterProcessManager(Project project) {
        this.project = project;
        initializeFlutterEnvironment();
    }

    /**
     * Inicializa el entorno Flutter detectando automáticamente el SDK
     */
    private void initializeFlutterEnvironment() {
        try {
            // Intentar detectar Flutter en el PATH
            flutterSdkPath = detectFlutterSdk();
            if (flutterSdkPath != null) {
                flutterExecutable = getFlutterExecutablePath(flutterSdkPath);
                LOG.info("Flutter SDK detectado en: " + flutterSdkPath);
                LOG.info("Ejecutable Flutter: " + flutterExecutable);
            } else {
                LOG.warn("No se pudo detectar Flutter SDK automáticamente");
            }
        } catch (Exception e) {
            LOG.error("Error inicializando entorno Flutter", e);
        }
    }

    /**
     * Detecta automáticamente la ubicación del Flutter SDK
     */
    private String detectFlutterSdk() {
        try {
            // Método 1: Usar 'flutter --version' para detectar instalación
            ProcessBuilder pb = new ProcessBuilder();
            if (SystemInfo.isWindows) {
                pb.command("where", "flutter");
            } else {
                pb.command("which", "flutter");
            }

            Process process = pb.start();
            process.waitFor(5, TimeUnit.SECONDS);

            if (process.exitValue() == 0) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String flutterPath = reader.readLine();
                    if (flutterPath != null && !flutterPath.trim().isEmpty()) {
                        // Extraer directorio SDK del path del ejecutable
                        Path sdkPath = Paths.get(flutterPath).getParent();
                        if (sdkPath != null && Files.exists(sdkPath.resolve("bin").resolve("flutter"))) {
                            return sdkPath.toString();
                        }
                    }
                }
            }

            // Método 2: Buscar en ubicaciones comunes
            String[] commonPaths = getCommonFlutterPaths();
            for (String path : commonPaths) {
                Path flutterDir = Paths.get(path);
                if (Files.exists(flutterDir.resolve("bin").resolve(getFlutterExecutableName()))) {
                    return flutterDir.toString();
                }
            }

            // Método 3: Variable de entorno FLUTTER_ROOT
            String flutterRoot = System.getenv("FLUTTER_ROOT");
            if (flutterRoot != null && Files.exists(Paths.get(flutterRoot, "bin", getFlutterExecutableName()))) {
                return flutterRoot;
            }

        } catch (Exception e) {
            LOG.warn("Error detectando Flutter SDK", e);
        }

        return null;
    }

    /**
     * Obtiene rutas comunes donde suele estar instalado Flutter
     */
    private String[] getCommonFlutterPaths() {
        if (SystemInfo.isWindows) {
            return new String[] {
                    System.getProperty("user.home") + "\\flutter",
                    "C:\\flutter",
                    "C:\\tools\\flutter",
                    System.getProperty("user.home") + "\\AppData\\Local\\flutter"
            };
        } else if (SystemInfo.isMac) {
            return new String[] {
                    System.getProperty("user.home") + "/flutter",
                    "/usr/local/flutter",
                    "/opt/flutter",
                    System.getProperty("user.home") + "/Development/flutter"
            };
        } else { // Linux
            return new String[] {
                    System.getProperty("user.home") + "/flutter",
                    "/usr/local/flutter",
                    "/opt/flutter",
                    "/snap/flutter/common/flutter"
            };
        }
    }

    /**
     * Obtiene el nombre del ejecutable Flutter según el SO
     */
    private String getFlutterExecutableName() {
        return SystemInfo.isWindows ? "flutter.bat" : "flutter";
    }

    /**
     * Obtiene la ruta completa al ejecutable Flutter
     */
    private String getFlutterExecutablePath(String sdkPath) {
        return Paths.get(sdkPath, "bin", getFlutterExecutableName()).toString();
    }

    /**
     * Ejecuta un comando Flutter con timeout y manejo de errores optimizado
     */
    public ProcessResult executeCommand(String[] command, int timeoutSeconds) throws Exception {
        if (flutterExecutable == null) {
            throw new IllegalStateException("Flutter SDK no encontrado. Verifique la instalación de Flutter.");
        }

        // Reemplazar "flutter" con la ruta completa del ejecutable
        String[] fullCommand = new String[command.length];
        fullCommand[0] = flutterExecutable;
        System.arraycopy(command, 1, fullCommand, 1, command.length - 1);

        LOG.info("Ejecutando comando: " + String.join(" ", fullCommand));

        ProcessBuilder pb = new ProcessBuilder(fullCommand);
        pb.directory(Paths.get(Objects.requireNonNull(project.getBasePath())).toFile());

        // Configurar variables de entorno optimizadas
        configureEnvironment(pb);

        long startTime = System.currentTimeMillis();
        Process process = pb.start();

        // Leer salida y errores de manera asíncrona
        StringBuilder output = new StringBuilder();
        StringBuilder error = new StringBuilder();

        Thread outputReader = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                    LOG.debug("Flutter stdout: " + line);
                }
            } catch (IOException e) {
                LOG.warn("Error leyendo stdout", e);
            }
        });

        Thread errorReader = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    error.append(line).append("\n");
                    LOG.debug("Flutter stderr: " + line);
                }
            } catch (IOException e) {
                LOG.warn("Error leyendo stderr", e);
            }
        });

        outputReader.start();
        errorReader.start();

        // Esperar con timeout
        boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);

        if (!finished) {
            process.destroyForcibly();
            throw new RuntimeException("Comando Flutter excedió el timeout de " + timeoutSeconds + " segundos");
        }

        // Esperar a que terminen los lectores
        outputReader.join(1000);
        errorReader.join(1000);

        long executionTime = System.currentTimeMillis() - startTime;
        int exitCode = process.exitValue();

        LOG.info("Comando Flutter completado en " + executionTime + "ms con código: " + exitCode);

        return new ProcessResult(exitCode, output.toString(), error.toString(), executionTime);
    }

    /**
     * Configura variables de entorno optimizadas para Flutter
     */
    private void configureEnvironment(ProcessBuilder pb) {
        // Variables de entorno para optimizar Flutter
        pb.environment().put("FLUTTER_WEB_USE_SKIA", "false");
        pb.environment().put("FLUTTER_WEB_AUTO_DETECT", "false");
        pb.environment().put("CHROME_EXECUTABLE", findChromeExecutable());

        // Configurar Dart VM para mejor performance
        pb.environment().put("DART_VM_OPTIONS", "--optimization-counter-threshold=5");

        // Deshabilitar analytics para mayor velocidad
        pb.environment().put("FLUTTER_ANALYTICS", "false");

        // Configurar cache de pub
        if (flutterSdkPath != null) {
            pb.environment().put("PUB_CACHE", Paths.get(flutterSdkPath, ".pub-cache").toString());
        }

        LOG.debug("Variables de entorno configuradas para optimización");
    }

    /**
     * Encuentra el ejecutable de Chrome para tests headless
     */
    private String findChromeExecutable() {
        String[] chromePaths;

        if (SystemInfo.isWindows) {
            chromePaths = new String[] {
                    "C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe",
                    "C:\\Program Files (x86)\\Google\\Chrome\\Application\\chrome.exe",
                    System.getProperty("user.home") + "\\AppData\\Local\\Google\\Chrome\\Application\\chrome.exe"
            };
        } else if (SystemInfo.isMac) {
            chromePaths = new String[] {
                    "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome",
                    "/Applications/Chromium.app/Contents/MacOS/Chromium"
            };
        } else { // Linux
            chromePaths = new String[] {
                    "/usr/bin/google-chrome",
                    "/usr/bin/chromium-browser",
                    "/usr/bin/chromium",
                    "/snap/bin/chromium"
            };
        }

        for (String path : chromePaths) {
            if (Files.exists(Paths.get(path))) {
                LOG.debug("Chrome encontrado en: " + path);
                return path;
            }
        }

        LOG.warn("No se encontró Chrome, usando configuración por defecto");
        return "";
    }

    /**
     * Ejecuta Flutter doctor para verificar la configuración
     */
    public ProcessResult checkFlutterInstallation() throws Exception {
        String[] command = {"flutter", "doctor", "--machine"};
        return executeCommand(command, 30);
    }

    /**
     * Obtiene la versión de Flutter instalada
     */
    public String getFlutterVersion() {
        try {
            ProcessResult result = executeCommand(new String[]{"flutter", "--version"}, 10);
            if (result.exitCode() == 0) {
                String output = result.output();
                // Extraer versión de la primera línea
                String[] lines = output.split("\n");
                if (lines.length > 0) {
                    return lines[0].trim();
                }
            }
        } catch (Exception e) {
            LOG.warn("Error obteniendo versión de Flutter", e);
        }
        return "Desconocida";
    }

    /**
     * Verifica si el proyecto actual es un proyecto Flutter válido
     */
    public boolean isFlutterProject() {
        try {
            Path projectPath = Paths.get(Objects.requireNonNull(project.getBasePath()));
            return Files.exists(projectPath.resolve("pubspec.yaml")) &&
                    Files.exists(projectPath.resolve("lib"));
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Ejecuta 'flutter pub get' si es necesario
     */
    public ProcessResult ensureDependencies() throws Exception {
        if (!isFlutterProject()) {
            throw new IllegalStateException("No es un proyecto Flutter válido");
        }

        // Verificar si necesita pub get
        Path pubspecLock = Paths.get(Objects.requireNonNull(project.getBasePath()), "pubspec.lock");
        Path pubspec = Paths.get(project.getBasePath(), "pubspec.yaml");

        if (!Files.exists(pubspecLock) ||
                Files.getLastModifiedTime(pubspec).compareTo(Files.getLastModifiedTime(pubspecLock)) > 0) {

            LOG.info("Ejecutando flutter pub get...");
            return executeCommand(new String[]{"flutter", "pub", "get"}, 60);
        }

        return new ProcessResult(0, "Dependencies up to date", "", 0);
    }

    /**
     * Crea un proyecto Flutter temporal para testing
     */
    public Path createTempFlutterProject(String projectName) throws Exception {
        Path tempDir = Files.createTempDirectory("flutter_quick_view_");
        Path projectPath = tempDir.resolve(projectName);

        ProcessResult result = executeCommand(new String[]{
                "flutter", "create", "--no-pub", projectPath.toString()
        }, 30);

        if (result.exitCode() != 0) {
            throw new RuntimeException("Error creando proyecto temporal: " + result.error());
        }

        return projectPath;
    }

    /**
     * Limpia archivos temporales y cache de Flutter
     */
    public void cleanFlutterCache() {
        try {
            executeCommand(new String[]{"flutter", "clean"}, 30);
            LOG.info("Cache de Flutter limpiado");
        } catch (Exception e) {
            LOG.warn("Error limpiando cache de Flutter", e);
        }
    }

    /**
         * Clase para encapsular resultados de procesos
         */
        public record ProcessResult(
                int exitCode,
                String output,
                String error,
                long executionTimeMs
        ) {

        public boolean isSuccess() {
                return exitCode == 0;
            }

            @Override
            public @NotNull String toString() {
                return String.format("ProcessResult{exitCode=%d, executionTime=%dms, hasOutput=%s, hasError=%s}",
                        exitCode, executionTimeMs, !output.isEmpty(), !error.isEmpty());
            }
        }

    /**
     * Obtiene la ruta del SDK de Flutter detectada
     */
    public String getFlutterSdkPath() {
        return flutterSdkPath;
    }

    /**
     * Establece manualmente la ruta del SDK de Flutter
     */
    public void setFlutterSdkPath(String sdkPath) {
        if (sdkPath != null && Files.exists(Paths.get(sdkPath, "bin", getFlutterExecutableName()))) {
            this.flutterSdkPath = sdkPath;
            this.flutterExecutable = getFlutterExecutablePath(sdkPath);
            LOG.info("Flutter SDK configurado manualmente: " + sdkPath);
        } else {
            throw new IllegalArgumentException("Ruta de Flutter SDK inválida: " + sdkPath);
        }
    }

    /**
     * Verifica si Flutter está correctamente configurado
     */
    public boolean isFlutterAvailable() {
        return flutterExecutable != null && Files.exists(Paths.get(flutterExecutable));
    }
}