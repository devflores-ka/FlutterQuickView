package com.github.devflores_ka.flutterquickview.ui.actions;

import com.github.devflores_ka.flutterquickview.analyzer.FlutterCodeAnalyzer;
import com.github.devflores_ka.flutterquickview.analyzer.models.WidgetNode;
import com.github.devflores_ka.flutterquickview.ui.PreviewToolWindowService;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.github.devflores_ka.flutterquickview.FlutterQuickViewBundle;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Acción que detecta widgets Preview y los envía a la Tool Window vía servicio
 */
public class PreviewAction extends AnAction {
    private static final Logger LOG = Logger.getInstance(PreviewAction.class);

    public PreviewAction() {
        super("Detect Flutter Preview Widgets",
                "Detect and show Preview widgets in current file",
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
                    FlutterQuickViewBundle.errorNoFileSelected(),
                    "FlutterQuickView");
            return;
        }

        if (!FlutterCodeAnalyzer.isDartFile(file)) {
            Messages.showWarningDialog(project,
                    FlutterQuickViewBundle.errorNotDartFile(),
                    "FlutterQuickView");
            return;
        }

        try {
            // 1. Obtener el servicio de comunicación con la Tool Window
            PreviewToolWindowService toolWindowService = PreviewToolWindowService.getInstance(project);

            // 2. Abrir la Tool Window
            ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow("FlutterQuickView");
            if (toolWindow != null) {
                toolWindow.activate(null);
                LOG.info("Tool Window activada");
            } else {
                LOG.warn("Tool Window 'FlutterQuickView' no encontrada");
            }

            // 3. Analizar el archivo
            LOG.info("Analizando archivo: " + file.getName());
            toolWindowService.notifyStatusUpdate("Analizando " + file.getName() + "...");

            List<WidgetNode> previewWidgets = FlutterCodeAnalyzer.analyzeFile(file, project);

            // 4. Notificar resultados a la Tool Window
            toolWindowService.notifyFileAnalyzed(file, previewWidgets);

            // 5. Mostrar mensaje al usuario
            if (previewWidgets.isEmpty()) {
                Messages.showInfoMessage(project,
                        """
                                No se encontraron widgets Preview en este archivo.
                                
                                Los widgets Preview deben:
                                • Terminar con 'Preview' en el nombre de la clase
                                • Extender de StatelessWidget o StatefulWidget
                                
                                Revisa la Tool Window 'FlutterQuickView' para más detalles.""",
                        "FlutterQuickView - Sin Resultados");
            } else {
                Messages.showInfoMessage(project,
                        "Se encontraron " + previewWidgets.size() + " widgets Preview.\n\n" +
                                "Widgets encontrados:\n" +
                                previewWidgets.stream()
                                        .map(w -> "• " + w.getClassName())
                                        .reduce((a, b) -> a + "\n" + b)
                                        .orElse("") +
                                "\n\nRevisa la Tool Window 'FlutterQuickView' para renderizar los widgets.",
                        "FlutterQuickView - Widgets Encontrados");
            }

            LOG.info("Análisis completado. Widgets encontrados: " + previewWidgets.size());

        } catch (Exception ex) {
            LOG.error("Error analizando archivo " + file.getPath(), ex);

            // Notificar error a la Tool Window
            PreviewToolWindowService toolWindowService = PreviewToolWindowService.getInstance(project);
            toolWindowService.notifyAnalysisError(file, ex);

            Messages.showErrorDialog(project,
                    "Error analizando el archivo: " + ex.getMessage() +
                            "\n\nRevisa la Tool Window 'FlutterQuickView' para más detalles.",
                    "FlutterQuickView - Error");
        }
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        // Habilitar la acción solo cuando hay un archivo Dart seleccionado
        VirtualFile file = e.getData(CommonDataKeys.VIRTUAL_FILE);
        boolean enabled = FlutterCodeAnalyzer.isDartFile(file);
        e.getPresentation().setEnabled(enabled);
    }
}