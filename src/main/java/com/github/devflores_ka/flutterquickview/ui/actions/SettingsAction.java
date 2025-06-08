package com.github.devflores_ka.flutterquickview.ui.actions;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

/**
 * Acción para abrir la configuración de FlutterQuickView
 */
public class SettingsAction extends AnAction {

    public SettingsAction() {
        super("FlutterQuickView Settings",
                "Configure FlutterQuickView settings",
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

        // Abrir diálogo de configuración
        SettingsDialog dialog = new SettingsDialog(project);
        dialog.show();
    }

    /**
     * Diálogo de configuración para FlutterQuickView
     */
    private static class SettingsDialog extends DialogWrapper {
        private final Project project;
        private JPanel mainPanel;
        private JComboBox<String> previewSizeCombo;
        private JComboBox<String> themeCombo;
        private JCheckBox hotReloadCheckBox;
        private JCheckBox autoRefreshCheckBox;
        private JTextField flutterSdkField;

        public SettingsDialog(Project project) {
            super(project);
            this.project = project;
            setTitle("Configuración FlutterQuickView");
            init();
        }

        @Override
        protected @Nullable JComponent createCenterPanel() {
            mainPanel = new JPanel(new BorderLayout());

            // Panel principal con configuraciones
            JPanel settingsPanel = createSettingsPanel();
            mainPanel.add(settingsPanel, BorderLayout.CENTER);

            // Panel de información
            JPanel infoPanel = createInfoPanel();
            mainPanel.add(infoPanel, BorderLayout.SOUTH);

            return mainPanel;
        }

        private JPanel createSettingsPanel() {
            JPanel panel = new JPanel(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(5, 5, 5, 5);
            gbc.anchor = GridBagConstraints.WEST;

            // Tamaño de previsualización
            gbc.gridx = 0; gbc.gridy = 0;
            panel.add(new JLabel("Tamaño de Previsualización:"), gbc);

            gbc.gridx = 1;
            previewSizeCombo = new JComboBox<>(new String[]{
                    "Pequeño (320x480)",
                    "Mediano (375x667)",
                    "Grande (414x896)"
            });
            previewSizeCombo.setSelectedIndex(1); // Mediano por defecto
            panel.add(previewSizeCombo, gbc);

            // Tema
            gbc.gridx = 0; gbc.gridy = 1;
            panel.add(new JLabel("Tema:"), gbc);

            gbc.gridx = 1;
            themeCombo = new JComboBox<>(new String[]{
                    "Claro",
                    "Oscuro",
                    "Seguir sistema"
            });
            themeCombo.setSelectedIndex(2); // Seguir sistema por defecto
            panel.add(themeCombo, gbc);

            // Hot Reload
            gbc.gridx = 0; gbc.gridy = 2;
            gbc.gridwidth = 2;
            hotReloadCheckBox = new JCheckBox("Habilitar Hot Reload", true);
            panel.add(hotReloadCheckBox, gbc);

            // Auto Refresh
            gbc.gridy = 3;
            autoRefreshCheckBox = new JCheckBox("Actualización Automática", true);
            panel.add(autoRefreshCheckBox, gbc);

            // Flutter SDK Path
            gbc.gridwidth = 1;
            gbc.gridx = 0; gbc.gridy = 4;
            panel.add(new JLabel("Ruta Flutter SDK:"), gbc);

            gbc.gridx = 1;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 1.0;
            flutterSdkField = new JTextField();
            flutterSdkField.setText(""); // TODO: Cargar de configuración
            panel.add(flutterSdkField, gbc);

            // Botón para explorar SDK
            gbc.gridx = 2;
            gbc.fill = GridBagConstraints.NONE;
            gbc.weightx = 0;
            JButton browseButton = new JButton("Explorar...");
            browseButton.addActionListener(e -> browseForFlutterSdk());
            panel.add(browseButton, gbc);

            return panel;
        }

        private JPanel createInfoPanel() {
            JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));

            JLabel infoLabel = new JLabel("<html><small>" +
                    "Nota: Algunas configuraciones requieren reiniciar el IDE para aplicarse completamente." +
                    "</small></html>");
            panel.add(infoLabel);

            return panel;
        }

        private void browseForFlutterSdk() {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            fileChooser.setDialogTitle("Seleccionar Flutter SDK");

            if (fileChooser.showOpenDialog(mainPanel) == JFileChooser.APPROVE_OPTION) {
                flutterSdkField.setText(fileChooser.getSelectedFile().getAbsolutePath());
            }
        }

        @Override
        protected void doOKAction() {
            // TODO: Guardar configuraciones
            saveSettings();
            super.doOKAction();
        }

        private void saveSettings() {
            // Por ahora solo mostramos un mensaje
            Messages.showInfoMessage(project,
                    "Configuraciones guardadas:\n\n" +
                            "• Tamaño: " + previewSizeCombo.getSelectedItem() + "\n" +
                            "• Tema: " + themeCombo.getSelectedItem() + "\n" +
                            "• Hot Reload: " + (hotReloadCheckBox.isSelected() ? "Habilitado" : "Deshabilitado") + "\n" +
                            "• Auto Refresh: " + (autoRefreshCheckBox.isSelected() ? "Habilitado" : "Deshabilitado") + "\n" +
                            "• Flutter SDK: " + flutterSdkField.getText() + "\n\n" +
                            "Nota: El sistema de persistencia se implementará próximamente.",
                    "Configuraciones Guardadas");
        }
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        // Siempre habilitado cuando hay un proyecto
        Project project = e.getProject();
        e.getPresentation().setEnabled(project != null);
    }
}