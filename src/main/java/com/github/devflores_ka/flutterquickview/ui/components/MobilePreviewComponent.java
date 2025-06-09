package com.github.devflores_ka.flutterquickview.ui.components;

import com.github.devflores_ka.flutterquickview.analyzer.models.WidgetNode;
import com.github.devflores_ka.flutterquickview.generator.MobilePreviewGenerator;
import com.github.devflores_ka.flutterquickview.renderer.FlutterMobileRenderer;
import com.github.devflores_ka.flutterquickview.ui.PreviewToolWindowsFactory;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;

/**
 * Componente mejorado para mostrar previews móviles realistas
 */
public class MobilePreviewComponent {
    private final WidgetNode widget;
    private final FlutterMobileRenderer mobileRenderer;
    private final PreviewToolWindowsFactory.PreviewToolWindowContent parent;
    private final JPanel panel;
    private final JLabel imageLabel;
    private final JLabel statusLabel;
    private final JButton renderButton;
    private final JComboBox<DeviceOption> deviceSelector;
    private final JCheckBox enableMobileMode;

    // Opciones de dispositivos para el selector
    private static class DeviceOption {
        final MobilePreviewGenerator.MobileDevice device;
        final String displayName;

        DeviceOption(MobilePreviewGenerator.MobileDevice device, String displayName) {
            this.device = device;
            this.displayName = displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    private static final DeviceOption[] DEVICE_OPTIONS = {
            new DeviceOption(MobilePreviewGenerator.MobileDevice.PIXEL_7, "📱 Google Pixel 7 (Android)"),
            new DeviceOption(MobilePreviewGenerator.MobileDevice.IPHONE_14, "📱 iPhone 14 (iOS)"),
            new DeviceOption(MobilePreviewGenerator.MobileDevice.SAMSUNG_GALAXY_S23, "📱 Galaxy S23 (Android)"),
            new DeviceOption(MobilePreviewGenerator.MobileDevice.IPHONE_14_PRO_MAX, "📱 iPhone 14 Pro Max (iOS)")
    };

    public MobilePreviewComponent(WidgetNode widget, FlutterMobileRenderer mobileRenderer,
                                  PreviewToolWindowsFactory.PreviewToolWindowContent parent) {
        this.widget = widget;
        this.mobileRenderer = mobileRenderer;
        this.parent = parent;
        this.panel = new JPanel(new BorderLayout());
        this.imageLabel = new JLabel();
        this.statusLabel = new JLabel("Listo para renderizar");
        this.renderButton = new JButton("🚀 Renderizar Móvil");
        this.deviceSelector = new JComboBox<>(DEVICE_OPTIONS);
        this.enableMobileMode = new JCheckBox("Modo Móvil Realista", true);

        setupComponent();
    }

    private void setupComponent() {
        // Configurar panel principal
        panel.setBorder(BorderFactory.createTitledBorder(
                "📱 " + widget.getClassName() + " (línea " + widget.getLineNumber() + ")"
        ));
        panel.setPreferredSize(new Dimension(420, 400));

        // Panel de configuración móvil
        JPanel configPanel = createMobileConfigPanel();

        // Panel de imagen con aspecto móvil
        setupImagePanel();

        // Panel de controles
        JPanel controlPanel = createControlPanel();

        panel.add(configPanel, BorderLayout.NORTH);
        panel.add(imageLabel, BorderLayout.CENTER);
        panel.add(controlPanel, BorderLayout.SOUTH);
    }

    /**
     * Crea panel de configuración específica para móvil
     */
    private JPanel createMobileConfigPanel() {
        JPanel configPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        configPanel.setBorder(new EmptyBorder(5, 5, 5, 5));

        // Checkbox para habilitar modo móvil
        enableMobileMode.setToolTipText("Renderizar con apariencia móvil nativa");
        configPanel.add(enableMobileMode);

        // Selector de dispositivo
        JLabel deviceLabel = new JLabel("Dispositivo:");
        deviceLabel.setFont(deviceLabel.getFont().deriveFont(Font.BOLD, 11f));
        configPanel.add(deviceLabel);

        deviceSelector.setPreferredSize(new Dimension(200, 25));
        deviceSelector.setSelectedIndex(0); // Pixel 7 por defecto
        deviceSelector.setToolTipText("Selecciona el dispositivo móvil a simular");
        configPanel.add(deviceSelector);

        // Agregar listener para cambios de dispositivo
        deviceSelector.addActionListener(e -> updateDeviceInfo());

        return configPanel;
    }

    /**
     * Configura el panel de imagen con proporciones móviles
     */
    private void setupImagePanel() {
        imageLabel.setHorizontalAlignment(SwingConstants.CENTER);
        imageLabel.setVerticalAlignment(SwingConstants.CENTER);
        imageLabel.setPreferredSize(new Dimension(393, 280)); // Proporción móvil
        imageLabel.setBorder(BorderFactory.createLoweredBevelBorder());
        imageLabel.setText("<html><center>🚀 Click 'Renderizar Móvil' para generar<br>preview con apariencia nativa</center></html>");
        imageLabel.setBackground(new Color(248, 249, 250));
        imageLabel.setOpaque(true);
    }

    /**
     * Crea panel de controles mejorado
     */
    private JPanel createControlPanel() {
        JPanel controlPanel = new JPanel(new BorderLayout());
        controlPanel.setBorder(new EmptyBorder(5, 5, 5, 5));

        // Panel de status con información del dispositivo
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statusPanel.add(statusLabel);

        // Información del dispositivo seleccionado
        JLabel deviceInfoLabel = createDeviceInfoLabel();
        statusPanel.add(deviceInfoLabel);

        // Panel de botones
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        // Botón de renderizado mejorado
        renderButton.setBackground(new Color(66, 165, 245));
        renderButton.setForeground(Color.WHITE);
        renderButton.setFocusPainted(false);
        renderButton.addActionListener(this::startMobileRendering);
        buttonPanel.add(renderButton);

        // Botón de opciones adicionales
        JButton optionsButton = new JButton("⚙️");
        optionsButton.setToolTipText("Opciones avanzadas de renderizado");
        optionsButton.addActionListener(this::showAdvancedOptions);
        buttonPanel.add(optionsButton);

        controlPanel.add(statusPanel, BorderLayout.CENTER);
        controlPanel.add(buttonPanel, BorderLayout.EAST);

        return controlPanel;
    }

    /**
     * Crea label con información del dispositivo
     */
    private JLabel createDeviceInfoLabel() {
        JLabel infoLabel = new JLabel();
        infoLabel.setFont(infoLabel.getFont().deriveFont(Font.ITALIC, 10f));
        infoLabel.setForeground(Color.GRAY);
        updateDeviceInfoLabel(infoLabel);
        return infoLabel;
    }

    /**
     * Actualiza información del dispositivo seleccionado
     */
    private void updateDeviceInfo() {
        DeviceOption selected = (DeviceOption) deviceSelector.getSelectedItem();
        if (selected != null) {
            parent.addLog("INFO", "Dispositivo seleccionado: " + selected.displayName +
                    " (" + selected.device.width + "x" + selected.device.height + ")");
        }
    }

    /**
     * Actualiza label de información del dispositivo
     */
    private void updateDeviceInfoLabel(JLabel infoLabel) {
        DeviceOption selected = (DeviceOption) deviceSelector.getSelectedItem();
        if (selected != null) {
            MobilePreviewGenerator.MobileDevice device = selected.device;
            infoLabel.setText(String.format("(%dx%d, %.1fx DPI, %s)",
                    device.width, device.height, device.pixelRatio, device.platform));
        }
    }

    /**
     * Inicia el renderizado móvil
     */
    public void startMobileRendering(ActionEvent e) {
        if (!enableMobileMode.isSelected()) {
            // Usar renderizado clásico si el modo móvil está deshabilitado
            parent.addLog("INFO", "Modo móvil deshabilitado, usando renderizado clásico");
            // Aquí llamarías al renderizado original
            return;
        }

        DeviceOption selectedDevice = (DeviceOption) deviceSelector.getSelectedItem();
        if (selectedDevice == null) {
            parent.addLog("ERROR", "No hay dispositivo seleccionado");
            return;
        }

        parent.addLog("INFO", "Iniciando renderizado móvil para: " + widget.getClassName());
        parent.addLog("INFO", "Dispositivo: " + selectedDevice.displayName);

        statusLabel.setText("Renderizando en " + selectedDevice.device.platform + "...");
        renderButton.setEnabled(false);
        imageLabel.setIcon(null);
        imageLabel.setText("<html><center>🔄 Renderizando " + widget.getClassName() +
                "<br>en " + selectedDevice.displayName + "...</center></html>");

        // Usar el renderizador móvil
        mobileRenderer.renderMobileWidgetWithProgress(widget, new FlutterMobileRenderer.MobileRenderCallback() {
            @Override
            public void onSuccess(BufferedImage image) {
                SwingUtilities.invokeLater(() -> {
                    parent.addLog("SUCCESS", "Renderizado móvil completado para " + widget.getClassName() +
                            " - Imagen: " + image.getWidth() + "x" + image.getHeight());

                    // Mostrar imagen escalada para el panel
                    ImageIcon scaledIcon = scaleImageForPanel(image);
                    imageLabel.setIcon(scaledIcon);
                    imageLabel.setText("");

                    statusLabel.setText("✅ Renderizado móvil exitoso (" + image.getWidth() + "x" + image.getHeight() + ")");
                    renderButton.setEnabled(true);
                });
            }

            @Override
            public void onError(Exception error) {
                SwingUtilities.invokeLater(() -> {
                    parent.addLog("ERROR", "Renderizado móvil falló para " + widget.getClassName() +
                            ": " + error.getMessage());

                    imageLabel.setText("<html><center>❌ Error en renderizado móvil:<br>" +
                            error.getMessage() + "</center></html>");
                    statusLabel.setText("❌ Renderizado falló");
                    renderButton.setEnabled(true);
                });
            }
        });
    }

    /**
     * Muestra opciones avanzadas de renderizado
     */
    private void showAdvancedOptions(ActionEvent e) {
        String[] options = {
                "Renderizar en todos los dispositivos",
                "Exportar imagen a archivo",
                "Configurar tema personalizado",
                "Ver código generado"
        };

        String choice = (String) JOptionPane.showInputDialog(
                panel,
                "Selecciona una opción avanzada:",
                "Opciones de Renderizado Móvil",
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0]
        );

        if (choice != null) {
            handleAdvancedOption(choice);
        }
    }

    /**
     * Maneja las opciones avanzadas seleccionadas
     */
    private void handleAdvancedOption(String choice) {
        switch (choice) {
            case "Renderizar en todos los dispositivos":
                renderAllDevices();
                break;
            case "Exportar imagen a archivo":
                exportCurrentImage();
                break;
            case "Configurar tema personalizado":
                showThemeConfig();
                break;
            case "Ver código generado":
                showGeneratedCode();
                break;
        }
    }

    /**
     * Renderiza el widget en todos los dispositivos disponibles
     */
    private void renderAllDevices() {
        parent.addLog("INFO", "Iniciando renderizado en todos los dispositivos...");

        for (DeviceOption deviceOption : DEVICE_OPTIONS) {
            parent.addLog("INFO", "Renderizando en: " + deviceOption.displayName);
            // Aquí implementarías el renderizado para cada dispositivo
        }
    }

    /**
     * Exporta la imagen actual a un archivo
     */
    private void exportCurrentImage() {
        Icon currentIcon = imageLabel.getIcon();
        if (currentIcon instanceof ImageIcon) {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Exportar Preview Móvil");
            fileChooser.setSelectedFile(new java.io.File(widget.getClassName() + "_mobile_preview.png"));

            if (fileChooser.showSaveDialog(panel) == JFileChooser.APPROVE_OPTION) {
                try {
                    Image image = ((ImageIcon) currentIcon).getImage();
                    BufferedImage bufferedImage = new BufferedImage(
                            image.getWidth(null), image.getHeight(null), BufferedImage.TYPE_INT_ARGB);

                    Graphics2D g2 = bufferedImage.createGraphics();
                    g2.drawImage(image, 0, 0, null);
                    g2.dispose();

                    javax.imageio.ImageIO.write(bufferedImage, "PNG", fileChooser.getSelectedFile());
                    parent.addLog("SUCCESS", "Imagen exportada: " + fileChooser.getSelectedFile().getName());
                } catch (Exception ex) {
                    parent.addLog("ERROR", "Error exportando imagen: " + ex.getMessage());
                }
            }
        } else {
            parent.addLog("WARN", "No hay imagen para exportar");
        }
    }

    /**
     * Muestra configuración de tema
     */
    private void showThemeConfig() {
        String[] themes = {"Light (Claro)", "Dark (Oscuro)", "System (Sistema)"};
        String selected = (String) JOptionPane.showInputDialog(
                panel,
                "Selecciona el tema para renderizado:",
                "Configuración de Tema",
                JOptionPane.QUESTION_MESSAGE,
                null,
                themes,
                themes[0]
        );

        if (selected != null) {
            parent.addLog("INFO", "Tema seleccionado: " + selected);
            // Aquí implementarías el cambio de tema
        }
    }

    /**
     * Muestra el código Dart generado
     */
    private void showGeneratedCode() {
        DeviceOption selected = (DeviceOption) deviceSelector.getSelectedItem();
        if (selected != null) {
            String generatedCode = MobilePreviewGenerator.generateMobileTest(widget, selected.device);

            JTextArea codeArea = new JTextArea(generatedCode);
            codeArea.setEditable(false);
            codeArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

            JScrollPane scrollPane = new JScrollPane(codeArea);
            scrollPane.setPreferredSize(new Dimension(600, 400));

            JOptionPane.showMessageDialog(panel, scrollPane,
                    "Código Dart Generado - " + selected.displayName,
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }

    /**
     * Escala imagen para que quepa en el panel manteniendo proporción móvil
     */
    private ImageIcon scaleImageForPanel(BufferedImage image) {
        int panelWidth = 393;  // Ancho del panel
        int panelHeight = 280; // Alto del panel

        // Calcular escala manteniendo proporción
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