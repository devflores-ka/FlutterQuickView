package com.github.devflores_ka.flutterquickview.ui.actions;

import com.github.devflores_ka.flutterquickview.analyzer.FlutterCodeAnalyzer;
import com.github.devflores_ka.flutterquickview.analyzer.models.WidgetNode;
import com.github.devflores_ka.flutterquickview.generator.MobilePreviewGenerator;
import com.github.devflores_ka.flutterquickview.renderer.FlutterRendererService;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.awt.image.BufferedImage;
import java.util.List;

/**
 * Acción rápida para generar preview móvil con dispositivo por defecto
 */
public class QuickMobilePreviewAction extends AnAction {
    private static final Logger LOG = Logger.getInstance(QuickMobilePreviewAction.class);

    public QuickMobilePreviewAction() {
        super("📱 Quick Mobile Preview",
                "Generate mobile preview with default device (Pixel 7)",
                null);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            LOG.warn("No project available");
            return;
        }

        VirtualFile file = e.getData(CommonDataKeys.VIRTUAL_FILE);
        if (file == null) {
            Messages.showWarningDialog(project,
                    "No hay archivo seleccionado",
                    "Quick Mobile Preview");
            return;
        }

        if (!FlutterCodeAnalyzer.isDartFile(file)) {
            Messages.showWarningDialog(project,
                    "El archivo seleccionado no es un archivo Dart (.dart)",
                    "Quick Mobile Preview");
            return;
        }

        try {
            LOG.info("=== QUICK MOBILE PREVIEW INICIADO ===");
            LOG.info("Archivo: " + file.getName());

            // Analizar archivo para widgets Preview
            List<WidgetNode> widgets = FlutterCodeAnalyzer.analyzeFile(file, project);

            if (widgets.isEmpty()) {
                Messages.showInfoMessage(project,
                        "No se encontraron widgets Preview en este archivo.\n\n" +
                                "Los widgets Preview deben:\n" +
                                "• Terminar con 'Preview' en el nombre de la clase\n" +
                                "• Extender StatelessWidget o StatefulWidget",
                        "Quick Mobile Preview - Sin Widgets");
                return;
            }

            // Usar el primer widget encontrado
            WidgetNode widget = widgets.get(0);
            LOG.info("Renderizando widget: " + widget.getClassName());

            // Mostrar diálogo de progreso
            String message = String.format("Generando preview móvil para: %s\nDispositivo: Google Pixel 7 (Android)\nTamaño: 393x851",
                    widget.getClassName());

            Messages.showInfoMessage(project, message, "Quick Mobile Preview - Iniciando");

            // Obtener servicio de renderizado
            FlutterRendererService rendererService = FlutterRendererService.getInstance(project);

            // Renderizar con dispositivo por defecto
            rendererService.renderWidgetMobileWithProgress(widget,
                    MobilePreviewGenerator.MobileDevice.PIXEL_7,
                    new FlutterRendererService.RenderCallback() {
                        @Override
                        public void onSuccess(BufferedImage image) {
                            LOG.info("Quick mobile preview completado exitosamente");

                            // Mostrar resultado
                            Messages.showInfoMessage(project,
                                    String.format("✅ Preview móvil generado exitosamente!\n\n" +
                                                    "Widget: %s\n" +
                                                    "Dispositivo: Google Pixel 7\n" +
                                                    "Resolución: %dx%d\n\n" +
                                                    "Revisa la Tool Window 'FlutterQuickView' para ver el resultado.",
                                            widget.getClassName(),
                                            image.getWidth(),
                                            image.getHeight()),
                                    "Quick Mobile Preview - Completado");
                        }

                        @Override
                        public void onError(Exception error) {
                            LOG.error("Quick mobile preview falló", error);

                            Messages.showErrorDialog(project,
                                    "Error generando preview móvil:\n\n" + error.getMessage() +
                                            "\n\nIntenta usar la acción 'Flutter Quick Preview' para más opciones.",
                                    "Quick Mobile Preview - Error");
                        }
                    });

        } catch (Exception ex) {
            LOG.error("Error en Quick Mobile Preview", ex);
            Messages.showErrorDialog(project,
                    "Error inesperado: " + ex.getMessage(),
                    "Quick Mobile Preview - Error");
        }
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        // Habilitar solo cuando hay un archivo Dart seleccionado
        VirtualFile file = e.getData(CommonDataKeys.VIRTUAL_FILE);
        boolean enabled = FlutterCodeAnalyzer.isDartFile(file);
        e.getPresentation().setEnabled(enabled);

        if (file != null) {
            LOG.debug("Update - Archivo: " + file.getName() + ", Dart: " + enabled);
        }
    }
}