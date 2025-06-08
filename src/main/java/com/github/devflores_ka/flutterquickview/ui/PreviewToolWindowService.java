package com.github.devflores_ka.flutterquickview.ui;

import com.github.devflores_ka.flutterquickview.analyzer.models.WidgetNode;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Servicio para comunicación entre acciones y la Tool Window
 */
@Service(Service.Level.PROJECT)
public final class PreviewToolWindowService {
    private static final Logger LOG = Logger.getInstance(PreviewToolWindowService.class);

    private final List<ToolWindowListener> listeners = new CopyOnWriteArrayList<>();

    public static PreviewToolWindowService getInstance(Project project) {
        return project.getService(PreviewToolWindowService.class);
    }

    /**
     * Interface para listeners de eventos de la Tool Window
     */
    public interface ToolWindowListener {
        void onFileAnalyzed(VirtualFile file, List<WidgetNode> widgets);
        void onAnalysisError(VirtualFile file, Exception error);
        void onStatusUpdate(String message);
    }

    /**
     * Registra un listener
     */
    public void addListener(ToolWindowListener listener) {
        listeners.add(listener);
        LOG.debug("Listener registrado: " + listener.getClass().getSimpleName());
    }

    /**
     * Remueve un listener
     */
    public void removeListener(ToolWindowListener listener) {
        listeners.remove(listener);
        LOG.debug("Listener removido: " + listener.getClass().getSimpleName());
    }

    /**
     * Notifica que un archivo fue analizado
     */
    public void notifyFileAnalyzed(@NotNull VirtualFile file, @NotNull List<WidgetNode> widgets) {
        LOG.info("Notificando análisis de archivo: " + file.getName() + " con " + widgets.size() + " widgets");

        for (ToolWindowListener listener : listeners) {
            try {
                listener.onFileAnalyzed(file, widgets);
            } catch (Exception e) {
                LOG.error("Error notificando listener: " + listener.getClass().getSimpleName(), e);
            }
        }
    }

    /**
     * Notifica error en análisis
     */
    public void notifyAnalysisError(@NotNull VirtualFile file, @NotNull Exception error) {
        LOG.warn("Notificando error de análisis para: " + file.getName());

        for (ToolWindowListener listener : listeners) {
            try {
                listener.onAnalysisError(file, error);
            } catch (Exception e) {
                LOG.error("Error notificando error de listener: " + listener.getClass().getSimpleName(), e);
            }
        }
    }

    /**
     * Notifica actualización de estado
     */
    public void notifyStatusUpdate(@NotNull String message) {
        LOG.debug("Notificando actualización de estado: " + message);

        for (ToolWindowListener listener : listeners) {
            try {
                listener.onStatusUpdate(message);
            } catch (Exception e) {
                LOG.error("Error notificando estado a listener: " + listener.getClass().getSimpleName(), e);
            }
        }
    }

    /**
     * Verifica si hay listeners registrados
     */
    public boolean hasListeners() {
        return !listeners.isEmpty();
    }

    /**
     * Obtiene el número de listeners registrados
     */
    public int getListenerCount() {
        return listeners.size();
    }
}