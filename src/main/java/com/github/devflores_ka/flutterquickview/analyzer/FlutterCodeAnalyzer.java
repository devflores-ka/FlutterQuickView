package com.github.devflores_ka.flutterquickview.analyzer;

import com.github.devflores_ka.flutterquickview.analyzer.models.WidgetNode;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Analizador principal que detecta widgets Preview en archivos Dart - VERSIÓN CORREGIDA
 */
public class FlutterCodeAnalyzer {
    private static final Logger LOG = Logger.getInstance(FlutterCodeAnalyzer.class);

    /**
     * Analiza un archivo Dart y retorna una lista de widgets Preview encontrados
     */
    public static List<WidgetNode> analyzeFile(VirtualFile file, Project project) {
        if (!isDartFile(file)) {
            LOG.debug("Archivo no es .dart, saltando: " + file.getName());
            return new ArrayList<>();
        }

        try {
            // Leer contenido del archivo
            String content = FileUtil.loadTextAndClose(file.getInputStream());
            LOG.info("=== ANALIZANDO ARCHIVO ===");
            LOG.info("Archivo: " + file.getName());
            LOG.info("Tamaño: " + content.length() + " caracteres");
            LOG.info("Preview inicial: " + (content.length() > 300 ? content.substring(0, 300) + "..." : content));

            // SIEMPRE usar análisis de texto directo para mayor confiabilidad
            List<WidgetNode> results = analyzeTextAdvanced(content, file.getName());

            if (!results.isEmpty()) {
                LOG.info("✅ WIDGETS ENCONTRADOS: " + results.size());
                for (WidgetNode widget : results) {
                    LOG.info("  - " + widget.getClassName() + " (línea " + widget.getLineNumber() + ")");
                }
            } else {
                LOG.info("❌ NO SE ENCONTRARON WIDGETS PREVIEW");
            }

            return results;

        } catch (IOException e) {
            LOG.error("Error leyendo archivo " + file.getPath(), e);
            return new ArrayList<>();
        }
    }

    /**
     * Análisis de texto avanzado mejorado para detectar widgets Preview
     */
    public static List<WidgetNode> analyzeTextAdvanced(String content, String fileName) {
        List<WidgetNode> results = new ArrayList<>();

        LOG.info("=== ANÁLISIS DE TEXTO AVANZADO ===");
        LOG.info("Contenido a analizar: " + content.length() + " caracteres");

        // Limpiar contenido de comentarios que podrían interferir
        String cleanContent = removeComments(content);

        // Patrón más flexible para detectar clases Preview
        // Busca líneas que contengan "class", "Preview" y "extends" con widgets Flutter
        String[] lines = cleanContent.split("\n");

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();

            // Buscar líneas que declaren clases
            if (line.startsWith("class ") && line.contains("Preview") && line.contains("extends")) {
                LOG.info("🔍 LÍNEA CANDIDATA " + (i + 1) + ": " + line);

                // Usar patrón más específico en esta línea
                Pattern linePattern = Pattern.compile(
                        "class\\s+(\\w*Preview)\\s+extends\\s+(StatelessWidget|StatefulWidget)",
                        Pattern.CASE_INSENSITIVE
                );

                Matcher lineMatcher = linePattern.matcher(line);

                if (lineMatcher.find()) {
                    String className = lineMatcher.group(1);
                    String widgetType = lineMatcher.group(2);

                    LOG.info("✅ CLASE DETECTADA:");
                    LOG.info("  - Nombre: " + className);
                    LOG.info("  - Tipo: " + widgetType);
                    LOG.info("  - Línea: " + (i + 1));

                    // Verificar que termine en "Preview"
                    if (!className.endsWith("Preview")) {
                        LOG.info("  ❌ No termina en 'Preview', saltando");
                        continue;
                    }

                    // Calcular offset basado en la línea encontrada
                    int lineStartOffset = calculateOffsetFromLine(content, i);

                    // Extraer el código completo de la clase
                    String fullClassCode = extractCompleteClass(content, className, lineStartOffset);
                    LOG.info("  - Código extraído: " + fullClassCode.length() + " caracteres");

                    if (fullClassCode.isEmpty()) {
                        LOG.warn("  ❌ No se pudo extraer código completo para: " + className);
                        continue;
                    }

                    // Crear el nodo del widget
                    WidgetNode widget = new WidgetNode(
                            className,
                            fileName,
                            lineStartOffset,
                            lineStartOffset + fullClassCode.length(),
                            i + 1, // Número de línea (1-indexed)
                            true,
                            fullClassCode
                    );

                    results.add(widget);
                    LOG.info("  ✅ Widget agregado: " + className);
                } else {
                    LOG.debug("  ❌ Línea no coincide con patrón específico: " + line);
                }
            }
        }

        LOG.info("=== ANÁLISIS COMPLETADO ===");
        LOG.info("Total widgets encontrados: " + results.size());

        return results;
    }

    /**
     * Remueve comentarios del código para evitar falsos positivos
     */
    private static String removeComments(String content) {
        // Remover comentarios de línea //
        content = content.replaceAll("//.*", "");

        // Remover comentarios de bloque /* */
        content = content.replaceAll("/\\*[\\s\\S]*?\\*/", "");

        return content;
    }

    /**
     * Calcula el offset basado en el número de línea
     */
    private static int calculateOffsetFromLine(String content, int lineIndex) {
        String[] lines = content.split("\n");
        int offset = 0;

        for (int i = 0; i < Math.min(lineIndex, lines.length); i++) {
            offset += lines[i].length() + 1; // +1 para el \n
        }

        return offset;
    }


    /**
     * Extrae el código completo de una clase usando conteo de llaves
     */
    private static String extractCompleteClass(String content, String className, int classStartOffset) {
        LOG.debug("Extrayendo clase completa: " + className);

        try {
            // Buscar la apertura de la clase '{'
            int braceStart = content.indexOf('{', classStartOffset);
            if (braceStart == -1) {
                LOG.warn("No se encontró '{' para la clase: " + className);
                return "";
            }

            // Contar llaves para encontrar el cierre
            int braceCount = 1;
            int currentPos = braceStart + 1;

            while (currentPos < content.length() && braceCount > 0) {
                char c = content.charAt(currentPos);

                if (c == '{') {
                    braceCount++;
                } else if (c == '}') {
                    braceCount--;
                }

                currentPos++;
            }

            if (braceCount == 0) {
                // Extraer desde el inicio de "class" hasta el cierre final
                String extractedCode = content.substring(classStartOffset, currentPos);
                LOG.debug("Código extraído exitosamente: " + extractedCode.length() + " caracteres");
                return extractedCode;
            } else {
                LOG.warn("No se encontró cierre de llaves para: " + className);
                return "";
            }

        } catch (Exception e) {
            LOG.error("Error extrayendo clase: " + className, e);
            return "";
        }
    }

    /**
     * Calcula el número de línea basado en el offset
     */
    private static int calculateLineNumber(String content, int offset) {
        int lineNumber = 1;
        for (int i = 0; i < Math.min(offset, content.length()); i++) {
            if (content.charAt(i) == '\n') {
                lineNumber++;
            }
        }
        return lineNumber;
    }

    /**
     * Analiza múltiples archivos Dart
     */
    public static List<WidgetNode> analyzeFiles(List<VirtualFile> files, Project project) {
        List<WidgetNode> allWidgets = new ArrayList<>();

        for (VirtualFile file : files) {
            allWidgets.addAll(analyzeFile(file, project));
        }

        return allWidgets;
    }

    /**
     * Verifica si un archivo es un archivo Dart válido
     */
    public static boolean isDartFile(VirtualFile file) {
        return file != null &&
                !file.isDirectory() &&
                "dart".equalsIgnoreCase(file.getExtension());
    }

    /**
     * Busca widgets Preview en el contenido de texto directamente (DEPRECATED - usar analyzeTextAdvanced)
     */
    @Deprecated
    public static List<WidgetNode> analyzeText(String content, String fileName) {
        return analyzeTextAdvanced(content, fileName);
    }

    /**
     * Método de utilidad para debugging - imprime estadísticas del análisis
     */
    public static void printAnalysisStats(String content, String fileName) {
        LOG.info("=== ESTADÍSTICAS DE ANÁLISIS ===");
        LOG.info("Archivo: " + fileName);
        LOG.info("Tamaño: " + content.length() + " caracteres");
        LOG.info("Líneas: " + (content.split("\n").length));

        // Contar ocurrencias de palabras clave
        long classCount = Pattern.compile("\\bclass\\b").matcher(content).results().count();
        long previewCount = Pattern.compile("\\bPreview\\b").matcher(content).results().count();
        long statelessCount = Pattern.compile("\\bStatelessWidget\\b").matcher(content).results().count();
        long statefulCount = Pattern.compile("\\bStatefulWidget\\b").matcher(content).results().count();

        LOG.info("Ocurrencias 'class': " + classCount);
        LOG.info("Ocurrencias 'Preview': " + previewCount);
        LOG.info("Ocurrencias 'StatelessWidget': " + statelessCount);
        LOG.info("Ocurrencias 'StatefulWidget': " + statefulCount);

        // Buscar líneas que contengan "class" y "Preview"
        String[] lines = content.split("\n");
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.contains("class") && line.contains("Preview")) {
                LOG.info("Línea " + (i + 1) + " contiene class + Preview: " + line);
            }
        }
    }
}