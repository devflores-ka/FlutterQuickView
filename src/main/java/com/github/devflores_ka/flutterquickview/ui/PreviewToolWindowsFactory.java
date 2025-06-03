package com.github.devflores_ka.flutterquickview.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * Factory para crear la Tool Window de FlutterQuickView
 */
public class PreviewToolWindowsFactory implements ToolWindowFactory {

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        // Crear el contenido principal de la tool window
        PreviewToolWindowContent toolWindowContent = new PreviewToolWindowContent(project);

        // Crear el content y añadirlo a la tool window
        Content content = ContentFactory.getInstance().createContent(
                toolWindowContent.getContent(),
                "",
                false
        );

        toolWindow.getContentManager().addContent(content);
    }

    /**
     * Clase interna para manejar el contenido de la tool window
     */
    private static class PreviewToolWindowContent {
        private final Project project;
        private final JPanel mainPanel;

        public PreviewToolWindowContent(Project project) {
            this.project = project;
            this.mainPanel = createMainPanel();
        }

        private JPanel createMainPanel() {
            JPanel panel = new JPanel();
            panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

            // Título
            JLabel titleLabel = new JLabel("Flutter Quick Preview");
            titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
            panel.add(titleLabel);

            // Mensaje inicial
            JLabel messageLabel = new JLabel("<html><center>" +
                    "No hay previsualizaciones disponibles.<br><br>" +
                    "Para generar previsualizaciones:<br>" +
                    "1. Abre un archivo .dart<br>" +
                    "2. Usa el menú contextual 'Flutter Quick Preview'<br>" +
                    "3. O presiona Ctrl+Shift+P" +
                    "</center></html>");
            messageLabel.setHorizontalAlignment(SwingConstants.CENTER);
            panel.add(messageLabel);

            // Botón de refresh (para futuras funcionalidades)
            JButton refreshButton = new JButton("Actualizar");
            refreshButton.addActionListener(e -> refreshPreviews());
            panel.add(refreshButton);

            return panel;
        }

        private void refreshPreviews() {
            // TODO: Implementar actualización de previsualizaciones
            JOptionPane.showMessageDialog(mainPanel,
                    "Funcionalidad de actualización próximamente disponible",
                    "FlutterQuickView",
                    JOptionPane.INFORMATION_MESSAGE);
        }

        public JComponent getContent() {
            return mainPanel;
        }
    }
}