package com.github.devflores_ka.flutterquickview.ui.components;

import com.github.devflores_ka.flutterquickview.analyzer.models.WidgetNode;
import com.github.devflores_ka.flutterquickview.generator.MobilePreviewGenerator;
import com.github.devflores_ka.flutterquickview.renderer.UltraAdaptiveFlutterRenderer;
import com.github.devflores_ka.flutterquickview.ui.PreviewToolWindowsFactory;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Componente inteligente que muestra el progreso del sistema ultra-adaptativo
 * con feedback en tiempo real y estadísticas de compatibilidad
 */
public class SmartPreviewComponent {
    private final WidgetNode widget;
    private final UltraAdaptiveFlutterRenderer ultraRenderer;
    private final PreviewToolWindowsFactory.PreviewToolWindowContent parent;

    // UI Components
    private final JPanel panel;
    private final JLabel imageLabel;
    private final JProgressBar intelligentProgressBar;
    private final JLabel statusLabel;
    private final JLabel strategyLabel;
    private final JButton smartRenderButton;
    private final JComboBox<DeviceOption> deviceSelector;
    private final JPanel statsPanel;
    private final JTextArea adaptiveLogArea;

    // State
    private final AtomicBoolean isRendering = new AtomicBoolean(false);
    private String currentStrategy = "Analizando proyecto...";

    private static class DeviceOption {
        final MobilePreviewGenerator.MobileDevice device;
        final String displayName;
        final String emoji;

        DeviceOption(MobilePreviewGenerator.MobileDevice device, String displayName, String emoji) {
            this.device = device;
            this.displayName = displayName;
            this.emoji = emoji;
        }

        @Override
        public String toString() {
            return emoji + " " + displayName;
        }
    }

    private static final DeviceOption[] SMART_DEVICE_OPTIONS = {
            new DeviceOption(MobilePreviewGenerator.MobileDevice.PIXEL_7, "Google Pixel 7 (Android)", "🤖"),
            new DeviceOption(MobilePreviewGenerator.MobileDevice.IPHONE_14, "iPhone 14 (iOS)", "📱"),
            new DeviceOption(MobilePreviewGenerator.MobileDevice.SAMSUNG_GALAXY_S23, "Galaxy S23 (Android)", "🌟"),
            new DeviceOption(MobilePreviewGenerator.MobileDevice.IPHONE_14_PRO_MAX, "iPhone 14 Pro Max (iOS)", "📱")
    };

    public SmartPreviewComponent(WidgetNode widget, UltraAdaptiveFlutterRenderer ultraRenderer,
                                 PreviewToolWindowsFactory.PreviewToolWindowContent parent) {
        this.widget = widget;
        this.ultraRenderer = ultraRenderer;
        this.parent = parent;
        this.panel = new JPanel(new BorderLayout());
        this.imageLabel = new JLabel();
        this.intelligentProgressBar = new JProgressBar(0, 100);
        this.statusLabel = new JLabel("🧠 Sistema inteligente listo");
        this.strategyLabel = new JLabel("Estrategia: Pendiente de análisis");
        this.smartRenderButton = new JButton("🚀 Renderizado Inteligente");
        this.deviceSelector = new JComboBox<>(SMART_DEVICE_OPTIONS);
        this.statsPanel = new JPanel();
        this.adaptiveLogArea = new JTextArea(5, 30);

        setupSmartComponent();
    }

    private void setupSmartComponent() {
        // Configurar panel principal
        panel.setBorder(BorderFactory.createTitledBorder(
                "🧠 " + widget.getClassName() + " - Renderizado Inteligente (línea " + widget.getLineNumber() + ")"
        ));
        panel.setPreferredSize(new Dimension(480, 550));

        // Panel superior: configuración y controles
        JPanel topPanel = createSmartControlPanel();

        // Panel central: imagen y progreso
        JPanel centerPanel = createSmartImagePanel();

        // Panel inferior: logs y estadísticas
        JPanel bottomPanel = createSmartStatsPanel();

        panel.add(topPanel, BorderLayout.NORTH);
        panel.add(centerPanel, BorderLayout.CENTER);
        panel.add(bottomPanel, BorderLayout.SOUTH);
    }

    private JPanel createSmartControlPanel() {
        JPanel controlPanel = new JPanel(new BorderLayout());
        controlPanel.setBorder(new EmptyBorder(8, 8, 8, 8));

        // Panel de dispositivo
        JPanel devicePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        devicePanel.add(new JLabel("🎯 Dispositivo:"));

        deviceSelector.setPreferredSize(new Dimension(220, 28));
        deviceSelector.setSelectedIndex(0); // Pixel 7 por defecto
        deviceSelector.setToolTipText("El sistema adaptativo optimizará automáticamente para este dispositivo");
        devicePanel.add(deviceSelector);

        // Panel de estrategia y botón
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        // Botón inteligente mejorado
        smartRenderButton.setBackground(new Color(33, 150, 243));
        smartRenderButton.setForeground(Color.WHITE);
        smartRenderButton.setFont(smartRenderButton.getFont().deriveFont(Font.BOLD));
        smartRenderButton.setFocusPainted(false);
        smartRenderButton.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createRaisedBevelBorder(),
                BorderFactory.createEmptyBorder(8, 16, 8, 16)
        ));
        smartRenderButton.addActionListener(this::startSmartRendering);

        // Botón de estadísticas
        JButton statsButton = new JButton("📊");
        statsButton.setToolTipText("Ver estadísticas del sistema adaptativo");
        statsButton.addActionListener(this::showAdaptiveStats);

        actionPanel.add(smartRenderButton);
        actionPanel.add(statsButton);

        controlPanel.add(devicePanel, BorderLayout.WEST);
        controlPanel.add(actionPanel, BorderLayout.EAST);

        return controlPanel;
    }

    private JPanel createSmartImagePanel() {
        JPanel centerPanel = new JPanel(new BorderLayout());

        // Panel de imagen con diseño mejorado
        imageLabel.setHorizontalAlignment(SwingConstants.CENTER);
        imageLabel.setVerticalAlignment(SwingConstants.CENTER);
        imageLabel.setPreferredSize(new Dimension(400, 280));
        imageLabel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLoweredBevelBorder(),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        imageLabel.setText("<html><center>" +
                "🧠 <b>Sistema de Renderizado Inteligente</b><br><br>" +
                "• 🔍 Análisis automático de compatibilidad<br>" +
                "• 🔧 Auto-reparación de dependencias<br>" +
                "• 📱 Optimización móvil nativa<br>" +
                "• 🎯 Estrategias adaptativas<br><br>" +
                "<i>Click 'Renderizado Inteligente' para comenzar</i>" +
                "</center></html>");
        imageLabel.setBackground(new Color(250, 250, 250));
        imageLabel.setOpaque(true);

        // Panel de progreso inteligente
        JPanel progressPanel = new JPanel(new BorderLayout());
        progressPanel.setBorder(new EmptyBorder(5, 5, 5, 5));

        // Configurar barra de progreso
        intelligentProgressBar.setStringPainted(true);
        intelligentProgressBar.setString("Sistema listo");
        intelligentProgressBar.setPreferredSize(new Dimension(400, 25));
        intelligentProgressBar.setBackground(Color.WHITE);
        intelligentProgressBar.setForeground(new Color(76, 175, 80));

        // Labels de estado
        statusLabel.setFont(statusLabel.getFont().deriveFont(Font.BOLD, 12f));
        strategyLabel.setFont(strategyLabel.getFont().deriveFont(Font.ITALIC, 11f));
        strategyLabel.setForeground(new Color(96, 125, 139));

        progressPanel.add(intelligentProgressBar, BorderLayout.CENTER);

        JPanel statusPanel = new JPanel(new GridLayout(2, 1));
        statusPanel.add(statusLabel);
        statusPanel.add(strategyLabel);
        progressPanel.add(statusPanel, BorderLayout.SOUTH);

        centerPanel.add(imageLabel, BorderLayout.CENTER);
        centerPanel.add(progressPanel, BorderLayout.SOUTH);

        return centerPanel;
    }

    private JPanel createSmartStatsPanel() {
        JPanel bottomPanel = new JPanel(new BorderLayout());

        // Área de logs adaptativos
        adaptiveLogArea.setEditable(false);
        adaptiveLogArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 10));
        adaptiveLogArea.setBackground(new Color(45, 45, 45));
        adaptiveLogArea.setForeground(new Color(187, 187, 187));
        adaptiveLogArea.setText("🧠 Sistema Ultra-Adaptativo inicializado\n" +
                "📊 Analizando compatibilidad del proyecto...\n" +
                "🎯 Listo para renderizado inteligente\n");

        JScrollPane logScrollPane = new JScrollPane(adaptiveLogArea);
        logScrollPane.setPreferredSize(new Dimension(400, 80));
        logScrollPane.setBorder(BorderFactory.createTitledBorder("🔍 Log Adaptativo"));

        bottomPanel.add(logScrollPane, BorderLayout.CENTER);

        return bottomPanel;
    }

    private void startSmartRendering(ActionEvent e) {
        if (isRendering.get()) {
            parent.addLog("WARN", "Renderizado ya en progreso...");
            return;
        }

        DeviceOption selectedDevice = (DeviceOption) deviceSelector.getSelectedItem();
        if (selectedDevice == null) {
            parent.addLog("ERROR", "No hay dispositivo seleccionado");
            return;
        }

        isRendering.set(true);
        smartRenderButton.setEnabled(false);
        deviceSelector.setEnabled(false);

        parent.addLog("INFO", "🧠 Iniciando renderizado ultra-adaptativo para: " + widget.getClassName());
        parent.addLog("INFO", "🎯 Dispositivo: " + selectedDevice.displayName);

        addAdaptiveLog("🚀 Iniciando renderizado ultra-adaptativo...");
        addAdaptiveLog("🎯 Dispositivo objetivo: " + selectedDevice.displayName);
        addAdaptiveLog("🔍 Analizando compatibilidad del proyecto...");

        updateSmartProgress(5, "🔍 Analizando compatibilidad...", "Detectando dependencias y configuraciones");

        ultraRenderer.renderWidgetUltraAdaptive(widget, selectedDevice.device,
                new UltraAdaptiveFlutterRenderer.UltraAdaptiveCallback() {

                    @Override
                    public void onSuccess(BufferedImage image, String strategyDescription) {
                        SwingUtilities.invokeLater(() -> {
                            parent.addLog("SUCCESS", "🎉 Renderizado ultra-adaptativo completado: " + strategyDescription);

                            addAdaptiveLog("✅ Renderizado exitoso: " + strategyDescription);
                            addAdaptiveLog("🖼️ Imagen generada: " + image.getWidth() + "x" + image.getHeight());
                            addAdaptiveLog("🎯 Estrategia aplicada con éxito");

                            // Mostrar imagen escalada
                            ImageIcon scaledIcon = scaleImageForSmartPanel(image);
                            imageLabel.setIcon(scaledIcon);
                            imageLabel.setText("");

                            updateSmartProgress(100, "✅ Renderizado completado", strategyDescription);

                            resetSmartUI();
                        });
                    }

                    @Override
                    public void onError(Exception error, String context) {
                        SwingUtilities.invokeLater(() -> {
                            parent.addLog("ERROR", "❌ Renderizado ultra-adaptativo falló: " + error.getMessage());

                            addAdaptiveLog("❌ Error: " + error.getMessage());
                            addAdaptiveLog("🔧 Contexto: " + context);
                            addAdaptiveLog("💡 Revisa las dependencias del proyecto");

                            imageLabel.setText("<html><center>" +
                                    "❌ <b>Renderizado falló</b><br><br>" +
                                    context + "<br><br>" +
                                    "<i>" + error.getMessage() + "</i>" +
                                    "</center></html>");

                            updateSmartProgress(0, "❌ Error en renderizado", context);

                            resetSmartUI();
                        });
                    }
                });
    }

    private void updateSmartProgress(int value, String status, String strategy) {
        intelligentProgressBar.setValue(value);
        intelligentProgressBar.setString(value + "% - " + status);
        statusLabel.setText(status);
        strategyLabel.setText("Estrategia: " + strategy);
        currentStrategy = strategy;
    }

    private void addAdaptiveLog(String message) {
        SwingUtilities.invokeLater(() -> {
            String timestamp = java.time.LocalTime.now().format(
                    java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
            adaptiveLogArea.append("[" + timestamp + "] " + message + "\n");
            adaptiveLogArea.setCaretPosition(adaptiveLogArea.getDocument().getLength());
        });
    }

    private void resetSmartUI() {
        isRendering.set(false);
        smartRenderButton.setEnabled(true);
        deviceSelector.setEnabled(true);
    }

    private void showAdaptiveStats(ActionEvent e) {
        String stats = ultraRenderer.getUltraStats();

        JTextArea statsArea = new JTextArea(stats);
        statsArea.setEditable(false);
        statsArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

        JScrollPane scrollPane = new JScrollPane(statsArea);
        scrollPane.setPreferredSize(new Dimension(500, 300));

        JOptionPane.showMessageDialog(panel, scrollPane,
                "📊 Estadísticas del Sistema Ultra-Adaptativo",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private ImageIcon scaleImageForSmartPanel(BufferedImage image) {
        int panelWidth = 400;
        int panelHeight = 280;

        double scaleX = (double) panelWidth / image.getWidth();
        double scaleY = (double) panelHeight / image.getHeight();
        double scale = Math.min(scaleX, scaleY);

        int newWidth = (int) (image.getWidth() * scale);
        int newHeight = (int) (image.getHeight() * scale);

        Image scaledImage = image.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH);
        return new ImageIcon(scaledImage);
    }

    public JPanel getPanel() {
        return panel;
    }
}