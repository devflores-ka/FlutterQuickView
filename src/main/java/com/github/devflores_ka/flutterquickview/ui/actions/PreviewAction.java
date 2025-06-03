package com.github.devflores_ka.flutterquickview.ui.actions;

import com.github.devflores_ka.flutterquickview.analyzer.FlutterCodeAnalyzer;
import com.github.devflores_ka.flutterquickview.analyzer.models.WidgetNode;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.github.devflores_ka.flutterquickview.FlutterQuickViewBundle;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Acción que detecta y muestra widgets Preview en el archivo actual
 */
public class PreviewAction extends AnAction {
    private static final Logger LOG = Logger.getInstance(PreviewAction.class);

    public PreviewAction() {
        super("Detect Flutter Preview Widgets",
                "Detect and show Preview widgets in current file",
                null);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) return;

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
            // Analizar el archivo
            List<WidgetNode> previewWidgets = FlutterCodeAnalyzer.analyzeFile(file, project);

            if (previewWidgets.isEmpty()) {
                Messages.showInfoMessage(project,
                        """
                                No se encontraron widgets Preview en este archivo.
                                
                                Los widgets Preview deben:
                                • Terminar con 'Preview' en el nombre de la clase
                                • Extender de StatelessWidget o StatefulWidget""",
                        "FlutterQuickView - Sin Resultados");
            } else {
                // Mostrar resultados
                showPreviewResults(project, previewWidgets, file.getName());
            }

        } catch (Exception ex) {
            LOG.error("Error analizando archivo " + file.getPath(), ex);
            Messages.showErrorDialog(project,
                    "Error analizando el archivo: " + ex.getMessage(),
                    "FlutterQuickView - Error");
        }
    }

    private void showPreviewResults(Project project, List<WidgetNode> widgets, String fileName) {
        StringBuilder message = new StringBuilder();
        message.append("Widgets Preview encontrados en ").append(fileName).append(":\n\n");

        for (int i = 0; i < widgets.size(); i++) {
            WidgetNode widget = widgets.get(i);
            message.append(String.format("%d. %s (línea %d)\n",
                    i + 1,
                    widget.getClassName(),
                    widget.getLineNumber()));
        }

        message.append("\n¿Desea generar previsualizaciones para estos widgets?");

        int result = Messages.showYesNoDialog(project,
                message.toString(),
                "FlutterQuickView - Widgets Encontrados",
                Messages.getQuestionIcon());

        if (result == Messages.YES) {
            // TODO: Aquí llamarías al servicio de renderizado
            generatePreviews(project, widgets);
        }
    }

    private void generatePreviews(Project project, List<WidgetNode> widgets) {
        // Por ahora solo mostramos un mensaje
        Messages.showInfoMessage(project,
                "Generación de previsualizaciones próximamente disponible.\n\n" +
                        "Widgets a procesar: " + widgets.size(),
                "FlutterQuickView - En Desarrollo");

        // TODO: Implementar la llamada al FlutterRenderService
        // FlutterRenderService.getInstance().generatePreviews(widgets);
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        // Habilitar la acción solo cuando hay un archivo Dart seleccionado
        VirtualFile file = e.getData(CommonDataKeys.VIRTUAL_FILE);
        boolean enabled = FlutterCodeAnalyzer.isDartFile(file);
        e.getPresentation().setEnabled(enabled);
    }
}