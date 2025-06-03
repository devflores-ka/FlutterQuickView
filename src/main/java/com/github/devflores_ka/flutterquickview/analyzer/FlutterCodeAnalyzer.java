package com.github.devflores_ka.flutterquickview.analyzer;

import com.github.devflores_ka.flutterquickview.analyzer.models.WidgetNode;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Analizador principal que detecta widgets Preview en archivos Dart
 */
public class FlutterCodeAnalyzer {
    private static final Logger LOG = Logger.getInstance(FlutterCodeAnalyzer.class);

    /**
     * Analiza un archivo Dart y retorna una lista de widgets Preview encontrados
     */
    public static List<WidgetNode> analyzeFile(VirtualFile file, Project project) {
        if (!isDartFile(file)) {
            return new ArrayList<>();
        }

        try {
            // Leer contenido del archivo
            String content = FileUtil.loadTextAndClose(file.getInputStream());
            LOG.info("Analizando archivo: " + file.getName() + " (" + content.length() + " caracteres)");

            // Debug: mostrar parte del contenido
            String preview = content.length() > 200 ? content.substring(0, 200) + "..." : content;
            LOG.info("Contenido preview: " + preview);

            // Obtener el archivo PSI para análisis detallado
            PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
            if (psiFile == null) {
                LOG.warn("No se pudo obtener PsiFile para: " + file.getPath());
                // Fallback: usar análisis de texto directo
                return analyzeText(content, file.getName());
            }

            // Realizar análisis con el visitor
            AstVisitor visitor = new AstVisitor(file.getName(), content);
            psiFile.accept(visitor);

            List<WidgetNode> results = visitor.getPreviewWidgets();

            if (results.isEmpty()) {
                LOG.info("El visitor PSI no encontró widgets. Intentando análisis de texto directo...");
                // Fallback: usar análisis de texto directo
                results = analyzeText(content, file.getName());
            }

            if (!results.isEmpty()) {
                LOG.info("Encontrados " + results.size() + " widgets Preview en " + file.getName());
                for (WidgetNode widget : results) {
                    LOG.info("  - " + widget.getClassName() + " (línea " + widget.getLineNumber() + ")");
                }
            } else {
                LOG.info("No se encontraron widgets Preview en " + file.getName());
            }

            return results;

        } catch (IOException e) {
            LOG.error("Error leyendo archivo " + file.getPath(), e);
            return new ArrayList<>();
        }
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
     * Busca widgets Preview en el contenido de texto directamente (útil para testing)
     */
    public static List<WidgetNode> analyzeText(String content, String fileName) {
        List<WidgetNode> results = new ArrayList<>();

        LOG.info("Iniciando análisis de texto directo para: " + fileName);

        String[] lines = content.split("\n");
        int currentOffset = 0;

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();

            LOG.debug("Línea " + (i + 1) + ": " + line);

            // Detectar declaraciones de clase que terminan en Preview
            if (line.startsWith("class ") && line.contains("Preview")) {
                LOG.info("Encontrada línea candidata: " + line);

                // Verificar que extiende de Widget
                if (line.contains("StatelessWidget") || line.contains("StatefulWidget")) {
                    String className = extractClassNameFromLine(line);

                    if (className != null && className.endsWith("Preview")) {
                        LOG.info("Widget Preview detectado: " + className);

                        // Buscar el bloque completo de la clase
                        String fullClassCode = extractFullClassFromText(content, currentOffset, className);

                        WidgetNode widget = new WidgetNode(
                                className,
                                fileName,
                                currentOffset,
                                currentOffset + fullClassCode.length(),
                                i + 1,
                                true,
                                fullClassCode
                        );

                        results.add(widget);
                        LOG.info("Widget agregado: " + className);
                    } else {
                        LOG.info("Clase no termina en Preview: " + className);
                    }
                } else {
                    LOG.info("Clase no extiende StatelessWidget o StatefulWidget: " + line);
                }
            }

            currentOffset += lines[i].length() + 1; // +1 para el \n
        }

        LOG.info("Análisis de texto completado. Widgets encontrados: " + results.size());
        return results;
    }

    private static String extractClassNameFromLine(String line) {
        // Extraer nombre de clase de declaraciones como "class LoginPreview extends StatelessWidget"
        LOG.debug("Extrayendo nombre de clase de: " + line);

        // Limpiar espacios y dividir por espacios
        String[] parts = line.trim().split("\\s+");

        if (parts.length >= 2 && "class".equals(parts[0])) {
            String className = parts[1];
            LOG.debug("Nombre de clase extraído: " + className);
            return className;
        }

        LOG.debug("No se pudo extraer nombre de clase");
        return null;
    }

    private static String extractFullClassFromText(String content, int startOffset, String className) {
        // Buscar desde el startOffset hacia adelante
        int classIndex = content.indexOf("class " + className, startOffset);
        if (classIndex == -1) return "";

        // Encontrar la primera llave de apertura
        int braceStart = content.indexOf('{', classIndex);
        if (braceStart == -1) return "";

        // Contar llaves para encontrar el final
        int braceCount = 1;
        int i = braceStart + 1;

        while (i < content.length() && braceCount > 0) {
            char c = content.charAt(i);
            if (c == '{') braceCount++;
            else if (c == '}') braceCount--;
            i++;
        }

        if (braceCount == 0) {
            return content.substring(classIndex, i);
        }

        return "";
    }
}