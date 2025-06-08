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
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Acción con debugging mejorado para detectar widgets Preview
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
            LOG.info("=== PREVIEW ACTION INICIADA ===");
            LOG.info("Archivo: " + file.getName());
            LOG.info("Ruta: " + file.getPath());

            // 1. Leer y analizar el contenido del archivo ANTES de llamar al analizador
            String fileContent = FileUtil.loadTextAndClose(file.getInputStream());
            LOG.info("Contenido leído: " + fileContent.length() + " caracteres");

            // Debug: Imprimir estadísticas del contenido
            FlutterCodeAnalyzer.printAnalysisStats(fileContent, file.getName());

            // 2. Obtener el servicio de comunicación con la Tool Window
            PreviewToolWindowService toolWindowService = PreviewToolWindowService.getInstance(project);

            // 3. Abrir la Tool Window
            ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow("FlutterQuickView");
            if (toolWindow != null) {
                toolWindow.activate(null);
                LOG.info("Tool Window activada");
            } else {
                LOG.warn("Tool Window 'FlutterQuickView' no encontrada");
            }

            // 4. Notificar inicio del análisis
            toolWindowService.notifyStatusUpdate("Analizando " + file.getName() + "...");

            // 5. Ejecutar análisis con logging detallado
            LOG.info("=== INICIANDO ANÁLISIS DETALLADO ===");

            // Usar el método mejorado directamente
            List<WidgetNode> previewWidgets = FlutterCodeAnalyzer.analyzeTextAdvanced(fileContent, file.getName());

            LOG.info("=== ANÁLISIS COMPLETADO ===");
            LOG.info("Widgets encontrados por análisis directo: " + previewWidgets.size());

            // 6. También probar el método original para comparar
            LOG.info("=== COMPARANDO CON MÉTODO ORIGINAL ===");
            List<WidgetNode> originalResults = FlutterCodeAnalyzer.analyzeFile(file, project);
            LOG.info("Widgets encontrados por método original: " + originalResults.size());

            // Usar los resultados del análisis directo (más confiable)
            List<WidgetNode> finalResults = previewWidgets;

            // 7. Notificar resultados a la Tool Window
            toolWindowService.notifyFileAnalyzed(file, finalResults);

            // 8. Mostrar mensaje detallado al usuario
            showDetailedResults(project, finalResults, file.getName(), fileContent);

            LOG.info("=== PREVIEW ACTION COMPLETADA ===");
            LOG.info("Resultado final: " + finalResults.size() + " widgets");

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

    /**
     * Muestra resultados detallados al usuario
     */
    private void showDetailedResults(Project project, List<WidgetNode> widgets, String fileName, String fileContent) {
        if (widgets.isEmpty()) {
            // Análisis detallado de por qué no se encontraron widgets
            StringBuilder debugInfo = new StringBuilder();
            debugInfo.append("No se encontraron widgets Preview en: ").append(fileName).append("\n\n");

            // Verificar contenido específico
            boolean hasClassKeyword = fileContent.contains("class ");
            boolean hasPreviewInName = fileContent.contains("Preview");
            boolean hasStatelessWidget = fileContent.contains("StatelessWidget");
            boolean hasStatefulWidget = fileContent.contains("StatefulWidget");
            boolean hasExtends = fileContent.contains("extends");

            debugInfo.append("Análisis del contenido:\n");
            debugInfo.append("• Contiene 'class ': ").append(hasClassKeyword ? "✅" : "❌").append("\n");
            debugInfo.append("• Contiene 'Preview': ").append(hasPreviewInName ? "✅" : "❌").append("\n");
            debugInfo.append("• Contiene 'StatelessWidget': ").append(hasStatelessWidget ? "✅" : "❌").append("\n");
            debugInfo.append("• Contiene 'StatefulWidget': ").append(hasStatefulWidget ? "✅" : "❌").append("\n");
            debugInfo.append("• Contiene 'extends': ").append(hasExtends ? "✅" : "❌").append("\n\n");

            debugInfo.append("Para que un widget sea detectado debe:\n");
            debugInfo.append("• Nombre de clase termine en 'Preview'\n");
            debugInfo.append("• Extender StatelessWidget o StatefulWidget\n");
            debugInfo.append("• Estar en una sola línea la declaración: class NombrePreview extends StatelessWidget\n\n");

            // Mostrar líneas relevantes del archivo
            String[] lines = fileContent.split("\n");
            debugInfo.append("Líneas que contienen 'class':\n");
            for (int i = 0; i < lines.length; i++) {
                if (lines[i].contains("class")) {
                    debugInfo.append("  Línea ").append(i + 1).append(": ").append(lines[i].trim()).append("\n");
                }
            }

            Messages.showInfoMessage(project, debugInfo.toString(), "FlutterQuickView - Debug Info");

        } else {
            // Mostrar widgets encontrados
            StringBuilder message = new StringBuilder();
            message.append("Se encontraron ").append(widgets.size()).append(" widgets Preview.\n\n");
            message.append("Widgets encontrados:\n");

            for (WidgetNode widget : widgets) {
                message.append("• ").append(widget.getClassName()).append(" (línea ").append(widget.getLineNumber()).append(")\n");
            }

            message.append("\nRevisa la Tool Window 'FlutterQuickView' para renderizar los widgets.");

            Messages.showInfoMessage(project, message.toString(), "FlutterQuickView - Widgets Encontrados");
        }
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        // Habilitar la acción solo cuando hay un archivo Dart seleccionado
        VirtualFile file = e.getData(CommonDataKeys.VIRTUAL_FILE);
        boolean enabled = FlutterCodeAnalyzer.isDartFile(file);
        e.getPresentation().setEnabled(enabled);

        // Debug info
        if (file != null) {
            LOG.debug("Update - Archivo: " + file.getName() + ", Dart: " + enabled);
        }
    }
}