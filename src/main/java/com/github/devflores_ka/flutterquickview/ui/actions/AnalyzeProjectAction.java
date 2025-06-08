package com.github.devflores_ka.flutterquickview.ui.actions;

import com.github.devflores_ka.flutterquickview.analyzer.FlutterCodeAnalyzer;
import com.github.devflores_ka.flutterquickview.analyzer.models.WidgetNode;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Acción para analizar todos los archivos Dart del proyecto buscando widgets Preview
 */
public class AnalyzeProjectAction extends AnAction {
    private static final Logger LOG = Logger.getInstance(AnalyzeProjectAction.class);

    public AnalyzeProjectAction() {
        super("Analyze Entire Project",
                "Analyze all Dart files in project for Preview widgets",
                null);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) return;

        // Confirmar la acción
        int result = Messages.showYesNoDialog(project,
                "Esto analizará todos los archivos Dart del proyecto.\n" +
                        "En proyectos grandes puede tomar varios minutos.\n\n" +
                        "¿Desea continuar?",
                "Analizar Proyecto Flutter",
                Messages.getQuestionIcon());

        if (result != Messages.YES) return;

        // Ejecutar análisis en background
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Analizando proyecto Flutter...", true) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                analyzeProject(project, indicator);
            }
        });
    }

    private void analyzeProject(Project project, ProgressIndicator indicator) {
        try {
            indicator.setText("Buscando archivos Dart...");

            // Buscar todos los archivos Dart en el proyecto
            // Nota: Necesitarías registrar DartFileType para que esto funcione
            // Por ahora usamos un enfoque alternativo
            List<VirtualFile> dartFiles = findDartFiles(project);

            if (dartFiles.isEmpty()) {
                showNoFilesFoundMessage(project);
                return;
            }

            indicator.setText("Analizando " + dartFiles.size() + " archivos...");

            List<WidgetNode> allWidgets = new ArrayList<>();
            int processedFiles = 0;

            for (VirtualFile file : dartFiles) {
                if (indicator.isCanceled()) return;

                indicator.setText2("Procesando: " + file.getName());
                indicator.setFraction((double) processedFiles / dartFiles.size());

                try {
                    List<WidgetNode> widgets = FlutterCodeAnalyzer.analyzeFile(file, project);
                    allWidgets.addAll(widgets);

                } catch (Exception ex) {
                    LOG.warn("Error analizando " + file.getPath(), ex);
                }

                processedFiles++;
            }

            // Mostrar resultados en el UI thread
            showResults(project, allWidgets, dartFiles.size());

        } catch (Exception ex) {
            LOG.error("Error durante análisis del proyecto", ex);
            showErrorMessage(project, ex.getMessage());
        }
    }

    private List<VirtualFile> findDartFiles(Project project) {
        List<VirtualFile> dartFiles = new ArrayList<>();

        try {
            // Buscar archivos con extensión .dart
            VirtualFile baseDir = project.getBaseDir();
            if (baseDir != null) {
                collectDartFiles(baseDir, dartFiles);
            }

        } catch (Exception e) {
            LOG.warn("Error buscando archivos Dart", e);
        }

        return dartFiles;
    }

    private void collectDartFiles(VirtualFile directory, List<VirtualFile> dartFiles) {
        if (directory.isDirectory()) {
            for (VirtualFile child : directory.getChildren()) {
                if (child.isDirectory()) {
                    // Evitar carpetas de build y cache
                    String name = child.getName();
                    if (!name.equals("build") && !name.equals(".dart_tool") && !name.equals(".pub-cache")) {
                        collectDartFiles(child, dartFiles);
                    }
                } else if (FlutterCodeAnalyzer.isDartFile(child)) {
                    dartFiles.add(child);
                }
            }
        }
    }

    private void showNoFilesFoundMessage(Project project) {
        Messages.showInfoMessage(project,
                "No se encontraron archivos Dart en el proyecto.\n\n" +
                        "Asegúrese de que:\n" +
                        "• El proyecto contiene archivos .dart\n" +
                        "• Los archivos están en carpetas accesibles\n" +
                        "• No están en carpetas de build o cache",
                "Sin Archivos Dart");
    }

    private void showResults(Project project, List<WidgetNode> widgets, int totalFiles) {
        StringBuilder message = new StringBuilder();
        message.append("Análisis del proyecto completado:\n\n");
        message.append("• Archivos analizados: ").append(totalFiles).append("\n");
        message.append("• Widgets Preview encontrados: ").append(widgets.size()).append("\n\n");

        if (widgets.isEmpty()) {
            message.append("No se encontraron widgets Preview.\n\n");
            message.append("Recuerde que los widgets Preview deben:\n");
            message.append("• Terminar con 'Preview' en el nombre\n");
            message.append("• Extender StatelessWidget o StatefulWidget");
        } else {
            message.append("Widgets encontrados por archivo:\n");

            // Agrupar por archivo
            widgets.stream()
                    .collect(java.util.stream.Collectors.groupingBy(WidgetNode::getFileName))
                    .forEach((fileName, fileWidgets) -> {
                        message.append("\n").append(fileName).append(":\n");
                        fileWidgets.forEach(widget ->
                                message.append("  • ").append(widget.getClassName())
                                        .append(" (línea ").append(widget.getLineNumber()).append(")\n"));
                    });
        }

        Messages.showInfoMessage(project, message.toString(), "Análisis del Proyecto");
    }

    private void showErrorMessage(Project project, String error) {
        Messages.showErrorDialog(project,
                "Error durante el análisis del proyecto:\n\n" + error,
                "Error de Análisis");
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        // Habilitar solo cuando hay un proyecto abierto
        Project project = e.getProject();
        e.getPresentation().setEnabled(project != null);
    }
}