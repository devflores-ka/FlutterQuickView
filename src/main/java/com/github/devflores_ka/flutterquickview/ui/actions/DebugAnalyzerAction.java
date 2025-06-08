package com.github.devflores_ka.flutterquickview.ui.actions;

import com.github.devflores_ka.flutterquickview.analyzer.AnalyzerTestUtility;
import com.github.devflores_ka.flutterquickview.analyzer.FlutterCodeAnalyzer;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

/**
 * Acción para debugging del analizador
 * Ejecuta tests y análisis detallado del archivo actual
 */
public class DebugAnalyzerAction extends AnAction {
    private static final Logger LOG = Logger.getInstance(DebugAnalyzerAction.class);

    public DebugAnalyzerAction() {
        super("Debug Analyzer",
                "Run debug tests on the analyzer and current file",
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

        VirtualFile file = e.getData(CommonDataKeys.VIRTUAL_FILE);

        // Opciones de debugging
        String[] options = {
                "Test Analyzer con ejemplos predefinidos",
                "Analizar archivo actual",
                "Test específico ButtonPreview",
                "Cancelar"
        };

        int choice = Messages.showChooseDialog(
                        project,
                        "Selecciona el tipo de debugging:",
                        "FlutterQuickView - Debug Analyzer",
                Messages.getQuestionIcon(),
                options,
                options[0]
                );

        switch (choice) {
            case 0:
                runPredefinedTests(project);
                break;
            case 1:
                analyzeCurrentFile(project, file);
                break;
            case 2:
                runButtonPreviewTest(project);
                break;
            default:
                return;
        }
    }

    /**
     * Ejecuta tests predefinidos del analizador
     */
    private void runPredefinedTests(Project project) {
        try {
            LOG.info("Ejecutando tests predefinidos del analizador...");
            AnalyzerTestUtility.runAnalyzerTests();

            Messages.showInfoMessage(project,
                    "Tests del analizador ejecutados.\n\n" +
                            "Revisa los logs de IntelliJ (Help > Show Log) para ver los resultados detallados.",
                    "Debug Analyzer - Tests Completados");

        } catch (Exception ex) {
            LOG.error("Error ejecutando tests predefinidos", ex);
            Messages.showErrorDialog(project,
                    "Error ejecutando tests: " + ex.getMessage(),
                    "Debug Analyzer - Error");
        }
    }

    /**
     * Analiza el archivo actual con debugging detallado
     */
    private void analyzeCurrentFile(Project project, VirtualFile file) {
        if (file == null) {
            Messages.showWarningDialog(project,
                    "No hay archivo seleccionado.",
                    "Debug Analyzer");
            return;
        }

        if (!FlutterCodeAnalyzer.isDartFile(file)) {
            Messages.showWarningDialog(project,
                    "El archivo seleccionado no es un archivo Dart (.dart).",
                    "Debug Analyzer");
            return;
        }

        try {
            LOG.info("=== DEBUG ANALYZER - ARCHIVO ACTUAL ===");
            LOG.info("Archivo: " + file.getName());

            // Leer contenido
            String content = FileUtil.loadTextAndClose(file.getInputStream());
            LOG.info("Contenido leído: " + content.length() + " caracteres");

            // Ejecutar test rápido
            AnalyzerTestUtility.quickTest(content, file.getName());

            Messages.showInfoMessage(project,
                    "Análisis de debugging completado para: " + file.getName() + "\n\n" +
                            "Revisa los logs de IntelliJ (Help > Show Log) para ver los resultados detallados.",
                    "Debug Analyzer - Análisis Completado");

        } catch (Exception ex) {
            LOG.error("Error analizando archivo actual", ex);
            Messages.showErrorDialog(project,
                    "Error analizando archivo: " + ex.getMessage(),
                    "Debug Analyzer - Error");
        }
    }

    /**
     * Ejecuta el test específico para ButtonPreview
     */
    private void runButtonPreviewTest(Project project) {
        try {
            LOG.info("Ejecutando test específico ButtonPreview...");
            AnalyzerTestUtility.testButtonPreviewSpecific();

            Messages.showInfoMessage(project,
                    "Test específico ButtonPreview ejecutado.\n\n" +
                            "Revisa los logs de IntelliJ (Help > Show Log) para ver los resultados detallados.",
                    "Debug Analyzer - Test ButtonPreview Completado");

        } catch (Exception ex) {
            LOG.error("Error ejecutando test ButtonPreview", ex);
            Messages.showErrorDialog(project,
                    "Error ejecutando test ButtonPreview: " + ex.getMessage(),
                    "Debug Analyzer - Error");
        }
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        // Siempre habilitado en proyectos
        Project project = e.getProject();
        e.getPresentation().setEnabled(project != null);
    }
}