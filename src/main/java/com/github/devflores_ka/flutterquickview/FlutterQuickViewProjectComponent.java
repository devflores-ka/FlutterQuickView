package com.github.devflores_ka.flutterquickview;

import com.github.devflores_ka.flutterquickview.analyzer.FlutterCodeAnalyzer;
import com.github.devflores_ka.flutterquickview.analyzer.models.WidgetNode;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import com.intellij.util.messages.MessageBusConnection;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Servicio a nivel de proyecto para FlutterQuickView
 * Se inicializa para cada proyecto y mantiene el estado específico del proyecto
 */
@Service(Service.Level.PROJECT)
public final class FlutterQuickViewProjectComponent {
    private static final Logger LOG = Logger.getInstance(FlutterQuickViewProjectComponent.class);
    private static final String COMPONENT_NAME = "FlutterQuickView.ProjectComponent";

    private final Project project;
    private MessageBusConnection messageBusConnection;

    // Cache de widgets Preview por archivo
    private final ConcurrentMap<String, List<WidgetNode>> previewWidgetsCache = new ConcurrentHashMap<>();

    // Estado del proyecto
    private boolean isFlutterProject = false;
    private boolean isInitialized = false;

    /**
     * Obtiene la instancia del servicio para un proyecto específico
     */
    public static FlutterQuickViewProjectComponent getInstance(Project project) {
        return project.getService(FlutterQuickViewProjectComponent.class);
    }

    /**
     * Inicializa el servicio cuando se crea la instancia
     */
    public FlutterQuickViewProjectComponent(Project project) {
        this.project = project;

        // Inicializar el servicio
        initialize();
    }

    /**
     * Método de inicialización del servicio
     */
    private void initialize() {
        LOG.info("Inicializando FlutterQuickView Service para: " + project.getName());

        // Verificar si es un proyecto Flutter
        detectFlutterProject();

        if (isFlutterProject) {
            // Inicializar componentes específicos de Flutter
            initializeFlutterComponents();
        }

        // Registrar listeners del proyecto
        registerProjectListeners();

        isInitialized = true;
        LOG.info("FlutterQuickView Service inicializado para: " + project.getName());
    }

    /**
     * Detecta si el proyecto actual es un proyecto Flutter
     */
    private void detectFlutterProject() {
        try {
            VirtualFile baseDir = project.getBaseDir();
            if (baseDir == null) return;

            // Buscar pubspec.yaml
            VirtualFile pubspecFile = baseDir.findChild("pubspec.yaml");
            if (pubspecFile != null && pubspecFile.exists()) {
                LOG.debug("Encontrado pubspec.yaml en " + project.getName());

                // Verificar si contiene dependencias de Flutter
                // Por simplicidad, asumimos que si tiene pubspec.yaml es Flutter
                isFlutterProject = true;
                LOG.info("Proyecto Flutter detectado: " + project.getName());
            } else {
                LOG.debug("No se encontró pubspec.yaml, no es un proyecto Flutter");
            }

        } catch (Exception e) {
            LOG.warn("Error detectando proyecto Flutter", e);
        }
    }

    /**
     * Inicializa componentes específicos de Flutter
     */
    private void initializeFlutterComponents() {
        try {
            LOG.debug("Inicializando componentes Flutter para " + project.getName());

            // Escanear archivos Dart existentes para poblar el caché inicial
            scanExistingDartFiles();

        } catch (Exception e) {
            LOG.error("Error inicializando componentes Flutter", e);
        }
    }

    /**
     * Registra listeners para el proyecto
     */
    private void registerProjectListeners() {
        try {
            // Conectar al message bus del proyecto
            messageBusConnection = project.getMessageBus().connect();

            // Escuchar cambios en archivos
            messageBusConnection.subscribe(VirtualFileManager.VFS_CHANGES, new BulkFileListener() {
                @Override
                public void after(@NotNull List<? extends VFileEvent> events) {
                    for (VFileEvent event : events) {
                        handleFileChange(event);
                    }
                }
            });

            LOG.debug("Listeners del proyecto registrados");

        } catch (Exception e) {
            LOG.error("Error registrando listeners del proyecto", e);
        }
    }

    /**
     * Maneja cambios en archivos
     */
    private void handleFileChange(VFileEvent event) {
        VirtualFile file = event.getFile();
        if (file != null && FlutterCodeAnalyzer.isDartFile(file)) {
            String filePath = file.getPath();

            // Invalidar cache para este archivo
            previewWidgetsCache.remove(filePath);

            LOG.debug("Cache invalidado para archivo modificado: " + filePath);
        }
    }

    /**
     * Escanea archivos Dart existentes para poblar el caché inicial
     */
    private void scanExistingDartFiles() {
        if (!isFlutterProject) return;

        // Por ahora no hacemos escaneo completo para evitar impacto en performance
        // El caché se poblará bajo demanda cuando se analicen archivos individuales
        LOG.debug("Cache de widgets Preview inicializado (bajo demanda)");
    }

    /**
     * Limpio recurso del proyecto
     */
    private void cleanupProjectResources() {
        try {
            // Desconectar message bus
            if (messageBusConnection != null) {
                messageBusConnection.disconnect();
                messageBusConnection = null;
            }

            // Limpiar caché
            previewWidgetsCache.clear();

            LOG.debug("Recursos del proyecto limpiados");

        } catch (Exception e) {
            LOG.error("Error limpiando recursos del proyecto", e);
        }
    }

    // Métodos públicos para usar el componente

    /**
     * Verifica si el proyecto es un proyecto Flutter
     */
    public boolean isFlutterProject() {
        return isFlutterProject;
    }

    /**
     * Verifica si el componente está inicializado
     */
    public boolean isInitialized() {
        return isInitialized;
    }

    /**
     * Obtiene widgets Preview cacheados para un archivo
     */
    public List<WidgetNode> getCachedPreviewWidgets(String filePath) {
        return previewWidgetsCache.get(filePath);
    }

    /**
     * Actualiza el cache de widgets Preview para un archivo
     */
    public void updatePreviewWidgetsCache(String filePath, List<WidgetNode> widgets) {
        if (widgets != null && !widgets.isEmpty()) {
            previewWidgetsCache.put(filePath, widgets);
            LOG.debug("Cache actualizado para " + filePath + " con " + widgets.size() + " widgets");
        } else {
            previewWidgetsCache.remove(filePath);
        }
    }

    /**
     * Obtiene el número total de widgets Preview cacheados
     */
    public int getCachedWidgetCount() {
        return previewWidgetsCache.values().stream()
                .mapToInt(List::size)
                .sum();
    }
}