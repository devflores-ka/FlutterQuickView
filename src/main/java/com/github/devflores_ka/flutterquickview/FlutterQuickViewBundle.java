package com.github.devflores_ka.flutterquickview;

import com.intellij.AbstractBundle;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.PropertyKey;

/**
 * Bundle de recursos para internacionalización de FlutterQuickView
 * Maneja todos los mensajes y textos del plugin
 */
public final class FlutterQuickViewBundle extends AbstractBundle {
    @NonNls
    public static final String BUNDLE = "messages.FlutterQuickViewBundle";

    private static final FlutterQuickViewBundle INSTANCE = new FlutterQuickViewBundle();

    private FlutterQuickViewBundle() {
        super(BUNDLE);
    }

    /**
     * Obtiene un mensaje del bundle
     */
    @NotNull
    public static String message(@NotNull @PropertyKey(resourceBundle = BUNDLE) String key, Object... params) {
        return INSTANCE.getMessage(key, params);
    }

    /**
     * Obtiene un mensaje del bundle con lazy loading
     */
    @NotNull
    public static String messagePointer(@NotNull @PropertyKey(resourceBundle = BUNDLE) String key, Object... params) {
        return message(key, params);
    }

    // Métodos de conveniencia para mensajes comunes

    // Mensajes de la UI
    public static String uiPreviewActionText() {
        return message("action.preview.text");
    }

    public static String uiPreviewActionDescription() {
        return message("action.preview.description");
    }

    public static String uiToolWindowTitle() {
        return message("toolwindow.title");
    }

    // Mensajes de error
    public static String errorNoFileSelected() {
        return message("error.no.file.selected");
    }

    public static String errorNotDartFile() {
        return message("error.not.dart.file");
    }

    public static String errorAnalyzing(String fileName) {
        return message("error.analyzing.file", fileName);
    }

    // Mensajes informativos
    public static String infoNoWidgetsFound() {
        return message("info.no.widgets.found");
    }

    public static String infoWidgetsFound(int count) {
        return message("info.widgets.found", count);
    }

    public static String infoPreviewGenerated(String widgetName) {
        return message("info.preview.generated", widgetName);
    }

    // Mensajes de confirmación
    public static String confirmGeneratePreviews() {
        return message("confirm.generate.previews");
    }

    // Mensajes de configuración
    public static String settingsTitle() {
        return message("settings.title");
    }

    public static String settingsPreviewSize() {
        return message("settings.preview.size");
    }

    public static String settingsTheme() {
        return message("settings.theme");
    }

    // Mensajes de progreso
    public static String progressAnalyzing() {
        return message("progress.analyzing");
    }

    public static String progressGenerating() {
        return message("progress.generating");
    }

    public static String progressRendering() {
        return message("progress.rendering");
    }
}