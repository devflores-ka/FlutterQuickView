package com.github.devflores_ka.flutterquickview.analyzer;

import com.github.devflores_ka.flutterquickview.analyzer.models.WidgetNode;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Visitor mejorado que analiza archivos Dart para encontrar clases Preview
 * VERSIÓN MEJORADA con mejor detección y logging
 */
public class AstVisitor extends PsiRecursiveElementVisitor {
    private static final Logger LOG = Logger.getInstance(AstVisitor.class);

    // Patrones mejorados para detectar widgets Flutter
    private static final Pattern PREVIEW_CLASS_PATTERN = Pattern.compile(".*Preview$");
    private static final Pattern WIDGET_EXTENDS_PATTERN = Pattern.compile(".*(StatelessWidget|StatefulWidget).*");

    private final List<WidgetNode> previewWidgets = new ArrayList<>();
    private final String fileName;
    private final String fileContent;

    public AstVisitor(String fileName, String fileContent) {
        this.fileName = fileName;
        this.fileContent = fileContent;
        LOG.info("=== INICIALIZANDO AST VISITOR ===");
        LOG.info("Archivo: " + fileName);
        LOG.info("Contenido: " + fileContent.length() + " caracteres");
    }

    @Override
    public void visitElement(@NotNull PsiElement element) {
        super.visitElement(element);

        // Analizar diferentes tipos de elementos PSI
        analyzeElement(element);
    }

    /**
     * Analiza un elemento PSI en busca de declaraciones de clase Preview
     */
    private void analyzeElement(PsiElement element) {
        String elementText = element.getText();
        if (elementText == null || elementText.trim().isEmpty()) {
            return;
        }

        // Log para debugging (solo elementos relevantes)
        if (elementText.contains("class") && elementText.contains("Preview")) {
            LOG.debug("🔍 Elemento PSI relevante encontrado:");
            LOG.debug("  Tipo: " + element.getClass().getSimpleName());
            LOG.debug("  Texto: " + elementText.substring(0, Math.min(100, elementText.length())));
        }

        // Diferentes estrategias según el tipo de elemento
        if (isClassDeclaration(element)) {
            analyzeClassDeclaration(element);
        } else if (isMultiLineClassElement(element)) {
            analyzeMultiLineClass(element);
        }
    }

    /**
     * Verifica si el elemento es una declaración de clase de una sola línea
     */
    private boolean isClassDeclaration(PsiElement element) {
        String elementText = element.getText();
        if (elementText == null) return false;

        // Buscar patrones de declaración de clase en una línea
        String trimmed = elementText.trim();
        return trimmed.startsWith("class ") &&
                trimmed.contains("extends") &&
                !trimmed.contains("{") &&
                trimmed.length() < 200; // Evitar elementos muy largos
    }

    /**
     * Verifica si el elemento contiene una clase multi-línea
     */
    private boolean isMultiLineClassElement(PsiElement element) {
        String elementText = element.getText();
        if (elementText == null) return false;

        return elementText.contains("class ") &&
                elementText.contains("Preview") &&
                elementText.contains("{") &&
                (elementText.contains("StatelessWidget") || elementText.contains("StatefulWidget"));
    }

    /**
     * Analiza una declaración de clase de una línea
     */
    private void analyzeClassDeclaration(PsiElement classElement) {
        String classText = classElement.getText();
        if (classText == null) return;

        LOG.info("🔍 ANALIZANDO DECLARACIÓN DE CLASE:");
        LOG.info("  Texto: " + classText);

        try {
            // Extraer nombre de la clase
            String className = extractClassName(classText);
            if (className == null) {
                LOG.debug("  ❌ No se pudo extraer nombre de clase");
                return;
            }

            LOG.info("  Clase: " + className);

            // Verificar que sea una clase Preview
            if (!PREVIEW_CLASS_PATTERN.matcher(className).matches()) {
                LOG.debug("  ❌ No termina en 'Preview': " + className);
                return;
            }

            // Verificar que extienda de Widget
            if (!WIDGET_EXTENDS_PATTERN.matcher(classText).matches()) {
                LOG.debug("  ❌ No extiende StatelessWidget o StatefulWidget: " + classText);
                return;
            }

            LOG.info("  ✅ Clase Preview válida detectada: " + className);

            // Buscar el código completo de la clase
            String fullClassCode = findCompleteClassFromElement(classElement, className);

            if (fullClassCode.isEmpty()) {
                LOG.warn("  ❌ No se pudo extraer código completo para: " + className);
                return;
            }

            // Crear el widget node
            createWidgetNode(className, classElement, fullClassCode);

        } catch (Exception e) {
            LOG.warn("Error analizando declaración de clase: " + e.getMessage());
        }
    }

    /**
     * Analiza una clase multi-línea completa
     */
    private void analyzeMultiLineClass(PsiElement element) {
        String elementText = element.getText();
        LOG.info("🔍 ANALIZANDO CLASE MULTI-LÍNEA:");
        LOG.info("  Tamaño: " + elementText.length() + " caracteres");

        try {
            // Extraer nombre de la clase del texto completo
            String className = extractClassNameFromMultiLine(elementText);
            if (className == null) {
                LOG.debug("  ❌ No se pudo extraer nombre de clase multi-línea");
                return;
            }

            LOG.info("  Clase: " + className);

            // Verificar que sea Preview
            if (!PREVIEW_CLASS_PATTERN.matcher(className).matches()) {
                LOG.debug("  ❌ No termina en 'Preview': " + className);
                return;
            }

            // Verificar que extienda Widget
            if (!WIDGET_EXTENDS_PATTERN.matcher(elementText).matches()) {
                LOG.debug("  ❌ No extiende Widget: " + className);
                return;
            }

            LOG.info("  ✅ Clase Preview multi-línea válida: " + className);

            // Usar el texto completo como código de la clase
            createWidgetNode(className, element, elementText);

        } catch (Exception e) {
            LOG.warn("Error analizando clase multi-línea: " + e.getMessage());
        }
    }

    /**
     * Extrae el nombre de clase de una declaración simple
     */
    private String extractClassName(String classText) {
        String[] parts = classText.trim().split("\\s+");
        if (parts.length >= 2 && "class".equals(parts[0])) {
            return parts[1];
        }
        return null;
    }

    /**
     * Extrae el nombre de clase de un texto multi-línea
     */
    private String extractClassNameFromMultiLine(String text) {
        // Buscar patrón "class NombreClase extends"
        Pattern pattern = Pattern.compile("class\\s+(\\w+)\\s+extends", Pattern.MULTILINE);
        java.util.regex.Matcher matcher = pattern.matcher(text);

        if (matcher.find()) {
            return matcher.group(1);
        }

        return null;
    }

    /**
     * Busca el código completo de la clase a partir del elemento PSI
     */
    private String findCompleteClassFromElement(PsiElement startElement, String className) {
        // Estrategia 1: Buscar en el elemento padre
        PsiElement parent = startElement.getParent();
        if (parent != null) {
            String parentText = parent.getText();
            if (parentText != null && parentText.contains(className) && parentText.contains("{")) {
                return parentText;
            }
        }

        // Estrategia 2: Buscar en el contenido del archivo
        return findCompleteClassInContent(className, startElement.getTextRange().getStartOffset());
    }

    /**
     * Busca el código completo de la clase en el contenido del archivo
     */
    private String findCompleteClassInContent(String className, int approximateOffset) {
        // Buscar la declaración de clase en el contenido
        int classIndex = fileContent.indexOf("class " + className, Math.max(0, approximateOffset - 100));
        if (classIndex == -1) {
            // Búsqueda más amplia
            classIndex = fileContent.indexOf("class " + className);
        }

        if (classIndex == -1) {
            LOG.warn("No se encontró 'class " + className + "' en el contenido del archivo");
            return "";
        }

        // Encontrar el bloque completo usando conteo de llaves
        int braceStart = fileContent.indexOf('{', classIndex);
        if (braceStart == -1) {
            LOG.warn("No se encontró '{' después de la declaración de clase");
            return "";
        }

        // Contar llaves para encontrar el final
        int braceCount = 1;
        int i = braceStart + 1;

        while (i < fileContent.length() && braceCount > 0) {
            char c = fileContent.charAt(i);
            if (c == '{') braceCount++;
            else if (c == '}') braceCount--;
            i++;
        }

        if (braceCount == 0) {
            String fullClass = fileContent.substring(classIndex, i);
            LOG.info("  ✅ Código completo extraído: " + fullClass.length() + " caracteres");
            return fullClass;
        }

        LOG.warn("No se encontró cierre de llaves para la clase: " + className);
        return "";
    }

    /**
     * Crea un WidgetNode para la clase detectada
     */
    private void createWidgetNode(String className, PsiElement element, String fullClassCode) {
        try {
            int startOffset = element.getTextRange().getStartOffset();
            int endOffset = startOffset + fullClassCode.length();
            int lineNumber = calculateLineNumber(startOffset);

            WidgetNode widgetNode = new WidgetNode(
                    className,
                    fileName,
                    startOffset,
                    endOffset,
                    lineNumber,
                    true,
                    fullClassCode
            );

            previewWidgets.add(widgetNode);

            LOG.info("  ✅ WidgetNode creado: " + className + " (línea " + lineNumber + ")");

        } catch (Exception e) {
            LOG.error("Error creando WidgetNode para " + className, e);
        }
    }

    /**
     * Calcula el número de línea basado en el offset
     */
    private int calculateLineNumber(int offset) {
        int lineNumber = 1;
        for (int i = 0; i < Math.min(offset, fileContent.length()); i++) {
            if (fileContent.charAt(i) == '\n') {
                lineNumber++;
            }
        }
        return lineNumber;
    }

    /**
     * Obtiene la lista de widgets Preview encontrados
     */
    public List<WidgetNode> getPreviewWidgets() {
        LOG.info("=== RESUMEN AST VISITOR ===");
        LOG.info("Widgets encontrados: " + previewWidgets.size());

        for (WidgetNode widget : previewWidgets) {
            LOG.info("  - " + widget.getClassName() + " (línea " + widget.getLineNumber() + ")");
        }

        return new ArrayList<>(previewWidgets);
    }
}