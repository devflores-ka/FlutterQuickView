package com.github.devflores_ka.flutterquickview.analyzer;

import com.github.devflores_ka.flutterquickview.analyzer.models.WidgetNode;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Visitor que analiza archivos Dart para encontrar clases que terminan en "Preview"
 * y que extienden de StatelessWidget o StatefulWidget
 */
public class AstVisitor extends PsiRecursiveElementVisitor {
    private static final Logger LOG = Logger.getInstance(AstVisitor.class);

    // Patrones para detectar widgets Flutter
    private static final Pattern PREVIEW_CLASS_PATTERN = Pattern.compile(".*Preview$");
    private static final Pattern STATELESS_WIDGET_PATTERN = Pattern.compile(".*StatelessWidget.*");
    private static final Pattern STATEFUL_WIDGET_PATTERN = Pattern.compile(".*StatefulWidget.*");

    private final List<WidgetNode> previewWidgets = new ArrayList<>();
    private final String fileName;
    private final String fileContent;

    public AstVisitor(String fileName, String fileContent) {
        this.fileName = fileName;
        this.fileContent = fileContent;
    }

    @Override
    public void visitElement(@NotNull PsiElement element) {
        super.visitElement(element);

        // Buscar declaraciones de clase
        if (isClassDeclaration(element)) {
            analyzeClassDeclaration(element);
        }
    }

    private boolean isClassDeclaration(PsiElement element) {
        // En archivos Dart, buscamos elementos que contengan "class"
        String elementText = element.getText();
        return elementText != null &&
                elementText.trim().startsWith("class ") &&
                !elementText.contains("{") &&
                elementText.contains("extends");
    }

    private void analyzeClassDeclaration(PsiElement classElement) {
        String classText = classElement.getText();
        if (classText == null) return;

        try {
            // Extraer nombre de la clase
            String className = extractClassName(classText);
            if (className == null) return;

            // Verificar si es una clase Preview
            if (!PREVIEW_CLASS_PATTERN.matcher(className).matches()) {
                return;
            }

            // Verificar si extiende de Widget
            if (!extendsFlutterWidget(classText)) {
                return;
            }

            // Obtener el código completo de la clase
            String fullClassCode = getFullClassCode(classElement);

            // Calcular posiciones
            int startOffset = classElement.getTextRange().getStartOffset();
            int endOffset = classElement.getTextRange().getEndOffset();
            int lineNumber = getLineNumber(startOffset);

            // Crear y agregar el nodo
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

            LOG.info("Detectado widget Preview: " + className + " en " + fileName + ":" + lineNumber);

        } catch (Exception e) {
            LOG.warn("Error analizando clase en " + fileName + ": " + e.getMessage());
        }
    }

    private String extractClassName(String classText) {
        // Extraer nombre de clase de declaraciones como "class LoginPreview extends StatelessWidget"
        String[] parts = classText.trim().split("\\s+");
        if (parts.length >= 2 && "class".equals(parts[0])) {
            return parts[1];
        }
        return null;
    }

    private boolean extendsFlutterWidget(String classText) {
        return STATELESS_WIDGET_PATTERN.matcher(classText).find() ||
                STATEFUL_WIDGET_PATTERN.matcher(classText).find();
    }

    private String getFullClassCode(PsiElement classElement) {
        // Buscar el bloque completo de la clase incluyendo las llaves
        PsiElement parent = classElement.getParent();
        if (parent != null) {
            // Buscar el siguiente elemento que contenga llaves
            PsiElement next = classElement.getNextSibling();
            while (next != null) {
                String nextText = next.getText();
                if (nextText != null && nextText.contains("{")) {
                    // Encontrar el bloque completo
                    return findCompleteClassBlock(classElement);
                }
                next = next.getNextSibling();
            }
        }
        return classElement.getText();
    }

    private String findCompleteClassBlock(PsiElement startElement) {
        int startOffset = startElement.getTextRange().getStartOffset();

        // Buscar el inicio de la clase en el contenido del archivo
        int classStart = fileContent.indexOf("class", startOffset - 100);
        if (classStart == -1) classStart = startOffset;

        // Encontrar el final del bloque usando conteo de llaves
        int braceCount;
        int i = fileContent.indexOf('{', classStart);
        if (i == -1) return startElement.getText();

        braceCount = 1;
        i++;

        while (i < fileContent.length() && braceCount > 0) {
            char c = fileContent.charAt(i);
            if (c == '{') braceCount++;
            else if (c == '}') braceCount--;
            i++;
        }

        if (braceCount == 0) {
            return fileContent.substring(classStart, i);
        }

        return startElement.getText();
    }

    private int getLineNumber(int offset) {
        int lineNumber = 1;
        for (int i = 0; i < offset && i < fileContent.length(); i++) {
            if (fileContent.charAt(i) == '\n') {
                lineNumber++;
            }
        }
        return lineNumber;
    }

    public List<WidgetNode> getPreviewWidgets() {
        return new ArrayList<>(previewWidgets);
    }
}