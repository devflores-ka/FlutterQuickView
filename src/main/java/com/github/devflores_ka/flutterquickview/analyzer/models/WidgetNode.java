package com.github.devflores_ka.flutterquickview.analyzer.models;

/**
 * Representa un nodo de widget Flutter encontrado en el análisis del código
 */
public class WidgetNode {
    private final String className;
    private final String fileName;
    private final int startOffset;
    private final int endOffset;
    private final int lineNumber;
    private final boolean isPreviewWidget;
    private final String sourceCode;

    public WidgetNode(String className, String fileName, int startOffset, int endOffset,
                      int lineNumber, boolean isPreviewWidget, String sourceCode) {
        this.className = className;
        this.fileName = fileName;
        this.startOffset = startOffset;
        this.endOffset = endOffset;
        this.lineNumber = lineNumber;
        this.isPreviewWidget = isPreviewWidget;
        this.sourceCode = sourceCode;
    }

    public String getClassName() {
        return className;
    }

    public String getFileName() {
        return fileName;
    }

    public int getStartOffset() {
        return startOffset;
    }

    public int getEndOffset() {
        return endOffset;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public boolean isPreviewWidget() {
        return isPreviewWidget;
    }

    public String getSourceCode() {
        return sourceCode;
    }

    @Override
    public String toString() {
        return "WidgetNode{" +
                "className='" + className + '\'' +
                ", fileName='" + fileName + '\'' +
                ", lineNumber=" + lineNumber +
                ", isPreview=" + isPreviewWidget +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WidgetNode that = (WidgetNode) o;
        return startOffset == that.startOffset &&
                endOffset == that.endOffset &&
                className.equals(that.className) &&
                fileName.equals(that.fileName);
    }

    @Override
    public int hashCode() {
        return className.hashCode() + fileName.hashCode() + startOffset + endOffset;
    }
}