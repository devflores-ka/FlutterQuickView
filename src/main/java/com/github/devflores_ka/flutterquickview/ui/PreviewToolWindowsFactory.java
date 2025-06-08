package com.github.devflores_ka.flutterquickview.ui;

import com.github.devflores_ka.flutterquickview.analyzer.FlutterCodeAnalyzer;
import com.github.devflores_ka.flutterquickview.analyzer.models.WidgetNode;
import com.github.devflores_ka.flutterquickview.renderer.FlutterRendererService;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.util.messages.MessageBusConnection;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Factory para la Tool Window conectada con PreviewToolWindowService
 */
public class PreviewToolWindowsFactory implements ToolWindowFactory {
    private static final Logger LOG = Logger.getInstance(PreviewToolWindowsFactory.class);

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        PreviewToolWindowContent content = new PreviewToolWindowContent(project);

        Content toolWindowContent = ContentFactory.getInstance().createContent(
                content.getComponent(),
                "",
                false
        );

        toolWindow.getContentManager().addContent(toolWindowContent);
    }

    /**
     * Componente principal de la Tool Window que escucha eventos del servicio
     */
    public static class PreviewToolWindowContent implements PreviewToolWindowService.ToolWindowListener {
        private static final Logger LOG = Logger.getInstance(PreviewToolWindowContent.class);

        private final Project project;
        private final JPanel mainPanel;
        private final JPanel previewsPanel;
        private final JTextArea logsArea;
        private final JLabel statusLabel;
        private final JButton refreshButton;
        private final JButton clearLogsButton;
        private final JButton analyzeButton;
        private JScrollPane scrollPane;
        private JScrollPane logsScrollPane;

        // Servicios
        private final FlutterRendererService rendererService;
        private final PreviewToolWindowService toolWindowService;

        // Cache de componentes de preview
        private final ConcurrentMap<String, PreviewComponent> previewComponents = new ConcurrentHashMap<>();

        // Listeners
        private MessageBusConnection messageBusConnection;

        // Estado actual
        private VirtualFile currentFile;

        public PreviewToolWindowContent(Project project) {
            this.project = project;
            this.rendererService = FlutterRendererService.getInstance(project);
            this.toolWindowService = PreviewToolWindowService.getInstance(project);
            this.mainPanel = new JPanel(new BorderLayout());
            this.previewsPanel = new JPanel();
            this.logsArea = new JTextArea();
            this.statusLabel = new JLabel("Flutter Quick Previews - Ready");
            this.refreshButton = new JButton("Refresh");
            this.clearLogsButton = new JButton("Clear Logs");
            this.analyzeButton = new JButton("Analyze Current File");

            initializeComponents();
            setupLayout();
            setupListeners();

            // Registrarse como listener del servicio
            toolWindowService.addListener(this);

            // Log inicial
            addLog("INFO", "FlutterQuickView Tool Window initialized");
            addLog("INFO", "Project: " + project.getName());
            addLog("INFO", "Listening for preview events...");
        }

        // Implementación de ToolWindowListener

        @Override
        public void onFileAnalyzed(VirtualFile file, List<WidgetNode> widgets) {
            SwingUtilities.invokeLater(() -> {
                addLog("INFO", "Received analysis results for: " + file.getName());
                addLog("INFO", "Widgets found: " + widgets.size());

                this.currentFile = file;

                if (widgets.isEmpty()) {
                    showNoWidgetsFound(file.getName());
                    updateStatus("No Preview widgets found in " + file.getName());
                } else {
                    displayPreviews(widgets);
                    updateStatus("Found " + widgets.size() + " Preview widgets in " + file.getName());

                    // Log cada widget encontrado
                    for (WidgetNode widget : widgets) {
                        addLog("SUCCESS", "Widget detected: " + widget.getClassName() + " at line " + widget.getLineNumber());
                    }
                }
            });
        }

        @Override
        public void onAnalysisError(VirtualFile file, Exception error) {
            SwingUtilities.invokeLater(() -> {
                addLog("ERROR", "Analysis failed for " + file.getName() + ": " + error.getMessage());
                showErrorState("Analysis failed: " + error.getMessage());
                updateStatus("Error analyzing " + file.getName());
            });
        }

        @Override
        public void onStatusUpdate(String message) {
            SwingUtilities.invokeLater(() -> {
                updateStatus(message);
                addLog("STATUS", message);
            });
        }

        // Métodos de UI

        private void initializeComponents() {
            // Configurar panel de previews
            previewsPanel.setLayout(new BoxLayout(previewsPanel, BoxLayout.Y_AXIS));
            previewsPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

            // Configurar scroll pane para previews
            this.scrollPane = new JScrollPane(previewsPanel);
            scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
            scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

            // Configurar área de logs
            logsArea.setEditable(false);
            logsArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
            logsArea.setBackground(new Color(30, 30, 30));
            logsArea.setForeground(Color.WHITE);
            logsArea.setRows(8);

            // Configurar scroll pane para logs
            this.logsScrollPane = new JScrollPane(logsArea);
            logsScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
            logsScrollPane.setBorder(BorderFactory.createTitledBorder("FlutterQuickView Logs"));

            // Configurar botones
            refreshButton.setToolTipText("Refresh previews for current file");
            clearLogsButton.setToolTipText("Clear log messages");
            analyzeButton.setToolTipText("Analyze current Dart file for Preview widgets");

            // Configurar status
            statusLabel.setHorizontalAlignment(SwingConstants.LEFT);
        }

        private void setupLayout() {
            // Panel superior con controles
            JPanel topPanel = new JPanel(new BorderLayout());

            JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            titlePanel.add(new JLabel("Flutter Previews"));

            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            buttonPanel.add(analyzeButton);
            buttonPanel.add(refreshButton);
            buttonPanel.add(clearLogsButton);

            topPanel.add(titlePanel, BorderLayout.WEST);
            topPanel.add(buttonPanel, BorderLayout.EAST);

            // Panel central dividido (previews arriba, logs abajo)
            JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
            splitPane.setTopComponent(scrollPane);
            splitPane.setBottomComponent(logsScrollPane);
            splitPane.setResizeWeight(0.6); // 60% para previews, 40% para logs
            splitPane.setDividerLocation(300);

            // Panel inferior con status
            JPanel bottomPanel = new JPanel(new BorderLayout());
            bottomPanel.add(statusLabel, BorderLayout.CENTER);

            // Cache stats
            JLabel cacheStatsLabel = createCacheStatsLabel();
            bottomPanel.add(cacheStatsLabel, BorderLayout.EAST);

            // Ensamblar panel principal
            mainPanel.add(topPanel, BorderLayout.NORTH);
            mainPanel.add(splitPane, BorderLayout.CENTER);
            mainPanel.add(bottomPanel, BorderLayout.SOUTH);
        }

        private void setupListeners() {
            // Botón analyze
            analyzeButton.addActionListener(this::onAnalyzeClicked);

            // Botón refresh
            refreshButton.addActionListener(this::onRefreshClicked);

            // Botón clear logs
            clearLogsButton.addActionListener(e -> {
                logsArea.setText("");
                addLog("INFO", "Logs cleared");
            });

            // Listener para cambios de archivos
            messageBusConnection = project.getMessageBus().connect();
            messageBusConnection.subscribe(VirtualFileManager.VFS_CHANGES, new BulkFileListener() {
                @Override
                public void after(@NotNull List<? extends VFileEvent> events) {
                    for (VFileEvent event : events) {
                        VirtualFile file = event.getFile();
                        if (file != null && FlutterCodeAnalyzer.isDartFile(file)) {
                            SwingUtilities.invokeLater(() -> handleFileChanged(file));
                        }
                    }
                }
            });
        }

        private void onAnalyzeClicked(ActionEvent e) {
            addLog("INFO", "Analyze button clicked");

            if (currentFile != null) {
                addLog("INFO", "Re-analyzing current file: " + currentFile.getName());
                analyzeFileDirectly(currentFile);
            } else {
                addLog("WARN", "No current file to analyze. Use Ctrl+Shift+P on a Dart file first.");
                updateStatus("No file selected - Use Ctrl+Shift+P on a Dart file");

                // Mostrar mensaje instructivo
                showInstructionPanel();
            }
        }

        private void onRefreshClicked(ActionEvent e) {
            addLog("INFO", "Refresh button clicked");

            if (currentFile != null) {
                addLog("INFO", "Refreshing previews for: " + currentFile.getName());
                analyzeFileDirectly(currentFile);
            } else {
                addLog("WARN", "No file selected for refresh");
                updateStatus("No file selected - Use Ctrl+Shift+P on a Dart file");
                showInstructionPanel();
            }
        }

        /**
         * Analiza un archivo directamente (para botones de la Tool Window)
         */
        private void analyzeFileDirectly(VirtualFile file) {
            try {
                addLog("INFO", "Direct analysis started for: " + file.getName());
                updateStatus("Analyzing " + file.getName() + "...");

                List<WidgetNode> widgets = FlutterCodeAnalyzer.analyzeFile(file, project);

                // Actualizar directamente (sin pasar por el servicio)
                onFileAnalyzed(file, widgets);

            } catch (Exception ex) {
                addLog("ERROR", "Direct analysis failed: " + ex.getMessage());
                LOG.error("Error in direct analysis", ex);
                onAnalysisError(file, ex);
            }
        }

        private void displayPreviews(List<WidgetNode> widgets) {
            addLog("INFO", "Displaying " + widgets.size() + " preview components");

            // Limpiar previews existentes
            previewsPanel.removeAll();
            previewComponents.clear();

            for (WidgetNode widget : widgets) {
                addLog("DEBUG", "Creating preview component for: " + widget.getClassName());
                PreviewComponent previewComponent = new PreviewComponent(widget, rendererService, this);
                previewComponents.put(widget.getClassName(), previewComponent);
                previewsPanel.add(previewComponent.getPanel());
                previewsPanel.add(Box.createVerticalStrut(10)); // Espaciado
            }

            // Refresh UI
            previewsPanel.revalidate();
            previewsPanel.repaint();

            addLog("SUCCESS", "Preview components created successfully");
        }

        private void handleFileChanged(VirtualFile file) {
            addLog("INFO", "File changed detected: " + file.getName());

            // Si es el archivo actual, refrescar automáticamente
            if (currentFile != null && file.equals(currentFile)) {
                addLog("INFO", "Current file changed, auto-refreshing...");
                analyzeFileDirectly(file);
            }
        }

        private void showNoWidgetsFound(String fileName) {
            previewsPanel.removeAll();
            previewComponents.clear();

            JPanel noWidgetsPanel = createNoWidgetsPanel(fileName);
            previewsPanel.add(noWidgetsPanel);

            previewsPanel.revalidate();
            previewsPanel.repaint();
        }

        private void showErrorState(String error) {
            previewsPanel.removeAll();
            previewComponents.clear();

            JPanel errorPanel = createErrorPanel(error);
            previewsPanel.add(errorPanel);

            previewsPanel.revalidate();
            previewsPanel.repaint();
        }

        private void showInstructionPanel() {
            previewsPanel.removeAll();
            previewComponents.clear();

            JPanel instructionPanel = createInstructionPanel();
            previewsPanel.add(instructionPanel);

            previewsPanel.revalidate();
            previewsPanel.repaint();
        }

        private JPanel createInstructionPanel() {
            JPanel panel = new JPanel(new BorderLayout());
            panel.setBorder(new EmptyBorder(20, 20, 20, 20));

            JLabel titleLabel = new JLabel("Welcome to FlutterQuickView", SwingConstants.CENTER);
            titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 16f));

            JLabel messageLabel = new JLabel("<html><center>" +
                    "<b>How to use:</b><br><br>" +
                    "1. Open a Dart file with Preview widgets<br>" +
                    "2. Press <b>Ctrl+Shift+P</b> to analyze<br>" +
                    "3. Preview widgets will appear here<br>" +
                    "4. Click 'Render' to generate previews<br><br>" +
                    "<i>Preview widgets must end with 'Preview' and extend StatelessWidget or StatefulWidget</i>" +
                    "</center></html>", SwingConstants.CENTER);

            JPanel contentPanel = new JPanel();
            contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
            contentPanel.add(titleLabel);
            contentPanel.add(Box.createVerticalStrut(20));
            contentPanel.add(messageLabel);

            panel.add(contentPanel, BorderLayout.CENTER);
            return panel;
        }

        private JPanel createNoWidgetsPanel(String fileName) {
            JPanel panel = new JPanel(new BorderLayout());
            panel.setBorder(new EmptyBorder(20, 20, 20, 20));

            JLabel titleLabel = new JLabel("No Preview Widgets Found", SwingConstants.CENTER);
            titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 14f));

            JLabel messageLabel = new JLabel("<html><center>" +
                    "No Preview widgets were found in <b>" + fileName + "</b><br><br>" +
                    "To create Preview widgets:<br>" +
                    "• Create a class ending with 'Preview'<br>" +
                    "• Extend StatelessWidget or StatefulWidget<br>" +
                    "• Use the class to preview your main widgets<br><br>" +
                    "<i>Example:</i><br>" +
                    "<code>class MyWidgetPreview extends StatelessWidget { ... }</code>" +
                    "</center></html>", SwingConstants.CENTER);

            JPanel contentPanel = new JPanel();
            contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
            contentPanel.add(titleLabel);
            contentPanel.add(Box.createVerticalStrut(10));
            contentPanel.add(messageLabel);

            panel.add(contentPanel, BorderLayout.CENTER);
            return panel;
        }

        private JPanel createErrorPanel(String error) {
            JPanel panel = new JPanel(new BorderLayout());
            panel.setBorder(new EmptyBorder(20, 20, 20, 20));

            JLabel titleLabel = new JLabel("Error", SwingConstants.CENTER);
            titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 14f));
            titleLabel.setForeground(Color.RED);

            JLabel messageLabel = new JLabel("<html><center>" +
                    "An error occurred:<br><br>" +
                    "<i>" + error + "</i><br><br>" +
                    "Check the logs below for more details." +
                    "</center></html>", SwingConstants.CENTER);

            JPanel contentPanel = new JPanel();
            contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
            contentPanel.add(titleLabel);
            contentPanel.add(Box.createVerticalStrut(10));
            contentPanel.add(messageLabel);

            panel.add(contentPanel, BorderLayout.CENTER);
            return panel;
        }

        private JLabel createCacheStatsLabel() {
            JLabel label = new JLabel();

            // Actualizar stats cada 5 segundos
            Timer timer = new Timer(5000, e -> {
                String stats = rendererService.getCacheStats().toString();
                label.setText(stats);
            });
            timer.start();

            return label;
        }

        private void updateStatus(String message) {
            statusLabel.setText(message);
        }

        /**
         * Agrega un mensaje al panel de logs con timestamp
         */
        public void addLog(String level, String message) {
            SwingUtilities.invokeLater(() -> {
                String timestamp = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
                String logEntry = String.format("[%s] %s: %s\n", timestamp, level, message);

                logsArea.append(logEntry);

                // Auto-scroll al final
                logsArea.setCaretPosition(logsArea.getDocument().getLength());

                // Log también en el logger de IntelliJ
                switch (level) {
                    case "ERROR":
                        LOG.error(message);
                        break;
                    case "WARN":
                        LOG.warn(message);
                        break;
                    case "SUCCESS":
                    case "INFO":
                    case "STATUS":
                        LOG.info(message);
                        break;
                    case "DEBUG":
                        LOG.debug(message);
                        break;
                }
            });
        }

        public JComponent getComponent() {
            return mainPanel;
        }

        /**
         * Cleanup al cerrar
         */
        public void dispose() {
            // Desregistrarse del servicio
            toolWindowService.removeListener(this);

            if (messageBusConnection != null) {
                messageBusConnection.disconnect();
            }
            previewComponents.clear();

            addLog("INFO", "Tool Window disposed");
        }
    }

    /**
     * Componente individual para mostrar un preview de widget
     */
    private static class PreviewComponent {
        private final WidgetNode widget;
        private final FlutterRendererService rendererService;
        private final PreviewToolWindowContent parent;
        private final JPanel panel;
        private final JLabel imageLabel;
        private final JLabel statusLabel;
        private final JButton renderButton;

        public PreviewComponent(WidgetNode widget, FlutterRendererService rendererService, PreviewToolWindowContent parent) {
            this.widget = widget;
            this.rendererService = rendererService;
            this.parent = parent;
            this.panel = new JPanel(new BorderLayout());
            this.imageLabel = new JLabel();
            this.statusLabel = new JLabel("Ready to render");
            this.renderButton = new JButton("Render");

            setupComponent();
        }

        private void setupComponent() {
            panel.setBorder(BorderFactory.createTitledBorder(widget.getClassName() + " (line " + widget.getLineNumber() + ")"));
            panel.setPreferredSize(new Dimension(400, 350));

            // Panel de imagen
            imageLabel.setHorizontalAlignment(SwingConstants.CENTER);
            imageLabel.setVerticalAlignment(SwingConstants.CENTER);
            imageLabel.setPreferredSize(new Dimension(375, 250));
            imageLabel.setBorder(BorderFactory.createLoweredBevelBorder());
            imageLabel.setText("Click 'Render' to generate preview");

            // Panel de controles
            JPanel controlPanel = new JPanel(new BorderLayout());
            controlPanel.add(statusLabel, BorderLayout.CENTER);
            controlPanel.add(renderButton, BorderLayout.EAST);

            renderButton.addActionListener(e -> startRendering());

            panel.add(imageLabel, BorderLayout.CENTER);
            panel.add(controlPanel, BorderLayout.SOUTH);
        }

        public void startRendering() {
            parent.addLog("INFO", "Starting render for: " + widget.getClassName());
            statusLabel.setText("Rendering...");
            renderButton.setEnabled(false);
            imageLabel.setIcon(null);
            imageLabel.setText("Rendering " + widget.getClassName() + "...");

            rendererService.renderWidgetWithProgress(widget, new FlutterRendererService.RenderCallback() {
                @Override
                public void onSuccess(BufferedImage image) {
                    SwingUtilities.invokeLater(() -> {
                        parent.addLog("SUCCESS", "Render completed for " + widget.getClassName() +
                                " - Image: " + image.getWidth() + "x" + image.getHeight());

                        // Escalar la imagen para que quepa en el panel
                        ImageIcon scaledIcon = scaleImageToFit(image, 375, 250);
                        imageLabel.setIcon(scaledIcon);
                        imageLabel.setText("");
                        statusLabel.setText("Rendered successfully (" + image.getWidth() + "x" + image.getHeight() + ")");
                        renderButton.setEnabled(true);
                    });
                }

                @Override
                public void onError(Exception error) {
                    SwingUtilities.invokeLater(() -> {
                        parent.addLog("ERROR", "Render failed for " + widget.getClassName() +
                                ": " + error.getMessage());

                        imageLabel.setText("<html><center>Render failed:<br>" + error.getMessage() + "</center></html>");
                        statusLabel.setText("Render failed");
                        renderButton.setEnabled(true);
                    });
                }
            });
        }

        /**
         * Escala una imagen para que quepa en las dimensiones especificadas manteniendo la proporción
         */
        private ImageIcon scaleImageToFit(BufferedImage image, int maxWidth, int maxHeight) {
            int originalWidth = image.getWidth();
            int originalHeight = image.getHeight();

            // Calcular la escala para que la imagen quepa
            double scaleX = (double) maxWidth / originalWidth;
            double scaleY = (double) maxHeight / originalHeight;
            double scale = Math.min(scaleX, scaleY);

            int newWidth = (int) (originalWidth * scale);
            int newHeight = (int) (originalHeight * scale);

            java.awt.Image scaledImage = image.getScaledInstance(newWidth, newHeight, java.awt.Image.SCALE_SMOOTH);
            return new ImageIcon(scaledImage);
        }

        public JPanel getPanel() {
            return panel;
        }
    }
}