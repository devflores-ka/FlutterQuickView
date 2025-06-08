package com.github.devflores_ka.flutterquickview.analyzer;

import com.github.devflores_ka.flutterquickview.analyzer.models.WidgetNode;
import com.intellij.openapi.diagnostic.Logger;

import java.util.List;

/**
 * Utilidad para testing y debugging del analizador
 * Permite probar el análisis con diferentes tipos de contenido
 */
public class AnalyzerTestUtility {
    private static final Logger LOG = Logger.getInstance(AnalyzerTestUtility.class);

    /**
     * Prueba el analizador con diferentes casos de test
     */
    public static void runAnalyzerTests() {
        LOG.info("=== INICIANDO TESTS DEL ANALIZADOR ===");

        // Test 1: Widget simple StatelessWidget
        testCase1_SimpleStatelessWidget();

        // Test 2: Widget StatefulWidget
        testCase2_StatefulWidget();

        // Test 3: Múltiples widgets en un archivo
        testCase3_MultipleWidgets();

        // Test 4: Widget sin Preview en el nombre
        testCase4_NonPreviewWidget();

        // Test 5: Contenido malformado
        testCase5_MalformedContent();

        LOG.info("=== TESTS COMPLETADOS ===");
    }

    private static void testCase1_SimpleStatelessWidget() {
        LOG.info("\n=== TEST 1: StatelessWidget Simple ===");

        String content = """
            import 'package:flutter/material.dart';

            class ButtonPreview extends StatelessWidget {
              const ButtonPreview({super.key});

              @override
              Widget build(BuildContext context) {
                return MaterialApp(
                  home: Scaffold(
                    body: Center(
                      child: ElevatedButton(
                        onPressed: () {},
                        child: const Text('Test Button'),
                      ),
                    ),
                  ),
                );
              }
            }
            """;

        List<WidgetNode> results = FlutterCodeAnalyzer.analyzeTextAdvanced(content, "button.dart");

        LOG.info("Resultado: " + results.size() + " widgets encontrados");
        if (results.size() == 1) {
            WidgetNode widget = results.get(0);
            LOG.info("✅ ÉXITO - Widget: " + widget.getClassName() + " (línea " + widget.getLineNumber() + ")");
        } else {
            LOG.error("❌ FALLO - Se esperaba 1 widget, se encontraron: " + results.size());
        }
    }

    private static void testCase2_StatefulWidget() {
        LOG.info("\n=== TEST 2: StatefulWidget ===");

        String content = """
            import 'package:flutter/material.dart';

            class CounterPreview extends StatefulWidget {
              const CounterPreview({super.key});

              @override
              State<CounterPreview> createState() => _CounterPreviewState();
            }

            class _CounterPreviewState extends State<CounterPreview> {
              int counter = 0;

              @override
              Widget build(BuildContext context) {
                return MaterialApp(
                  home: Scaffold(
                    body: Center(
                      child: Text('Counter: $counter'),
                    ),
                  ),
                );
              }
            }
            """;

        List<WidgetNode> results = FlutterCodeAnalyzer.analyzeTextAdvanced(content, "counter.dart");

        LOG.info("Resultado: " + results.size() + " widgets encontrados");
        if (results.size() == 1) {
            WidgetNode widget = results.get(0);
            LOG.info("✅ ÉXITO - Widget: " + widget.getClassName() + " (línea " + widget.getLineNumber() + ")");
        } else {
            LOG.error("❌ FALLO - Se esperaba 1 widget, se encontraron: " + results.size());
        }
    }

    private static void testCase3_MultipleWidgets() {
        LOG.info("\n=== TEST 3: Múltiples Widgets (debería fallar con nueva lógica) ===");

        String content = """
            import 'package:flutter/material.dart';

            class FirstPreview extends StatelessWidget {
              @override
              Widget build(BuildContext context) {
                return Text('First');
              }
            }

            class SecondPreview extends StatelessWidget {
              @override
              Widget build(BuildContext context) {
                return Text('Second');
              }
            }
            """;

        List<WidgetNode> results = FlutterCodeAnalyzer.analyzeTextAdvanced(content, "multiple.dart");

        LOG.info("Resultado: " + results.size() + " widgets encontrados");
        // Con la nueva lógica, debería encontrar ambos pero quizás queramos limitarlo a 1
        for (WidgetNode widget : results) {
            LOG.info("Widget encontrado: " + widget.getClassName() + " (línea " + widget.getLineNumber() + ")");
        }
    }

    private static void testCase4_NonPreviewWidget() {
        LOG.info("\n=== TEST 4: Widget sin Preview (debería ser ignorado) ===");

        String content = """
            import 'package:flutter/material.dart';

            class RegularButton extends StatelessWidget {
              @override
              Widget build(BuildContext context) {
                return ElevatedButton(
                  onPressed: () {},
                  child: Text('Regular Button'),
                );
              }
            }
            """;

        List<WidgetNode> results = FlutterCodeAnalyzer.analyzeTextAdvanced(content, "regular.dart");

        LOG.info("Resultado: " + results.size() + " widgets encontrados");
        if (results.size() == 0) {
            LOG.info("✅ ÉXITO - Correctamente ignorado widget sin 'Preview'");
        } else {
            LOG.error("❌ FALLO - Se esperaban 0 widgets, se encontraron: " + results.size());
        }
    }

    private static void testCase5_MalformedContent() {
        LOG.info("\n=== TEST 5: Contenido Malformado ===");

        String content = """
            import 'package:flutter/material.dart';

            class BrokenPreview extends StatelessWidget
              // Falta llave de apertura
              Widget build(context) {
                return Text('Broken');
            """;

        List<WidgetNode> results = FlutterCodeAnalyzer.analyzeTextAdvanced(content, "broken.dart");

        LOG.info("Resultado: " + results.size() + " widgets encontrados");
        LOG.info("Contenido malformado manejado correctamente");
    }

    /**
     * Test específico para el contenido que está fallando
     */
    public static void testButtonPreviewSpecific() {
        LOG.info("\n=== TEST ESPECÍFICO: ButtonPreview ===");

        String content = """
            import 'package:flutter/material.dart';

            class ButtonPreview extends StatelessWidget {
              const ButtonPreview({super.key});

              @override
              Widget build(BuildContext context) {
                return MaterialApp(
                  home: Scaffold(
                    body: Center(
                      child: ElevatedButton(
                        onPressed: () {},
                        child: const Text('Test Button'),
                      ),
                    ),
                  ),
                );
              }
            }
            """;

        // Imprimir estadísticas detalladas
        FlutterCodeAnalyzer.printAnalysisStats(content, "button.dart");

        // Ejecutar análisis
        List<WidgetNode> results = FlutterCodeAnalyzer.analyzeTextAdvanced(content, "button.dart");

        LOG.info("=== RESULTADO ESPECÍFICO ===");
        LOG.info("Widgets encontrados: " + results.size());

        for (WidgetNode widget : results) {
            LOG.info("Widget: " + widget.getClassName());
            LOG.info("Línea: " + widget.getLineNumber());
            LOG.info("Código (preview): " + widget.getSourceCode().substring(0, Math.min(100, widget.getSourceCode().length())));
        }
    }

    /**
     * Ejecuta un test rápido con el contenido proporcionado
     */
    public static void quickTest(String content, String fileName) {
        LOG.info("\n=== QUICK TEST: " + fileName + " ===");

        // Estadísticas
        FlutterCodeAnalyzer.printAnalysisStats(content, fileName);

        // Análisis
        List<WidgetNode> results = FlutterCodeAnalyzer.analyzeTextAdvanced(content, fileName);

        LOG.info("=== RESULTADO QUICK TEST ===");
        LOG.info("Widgets encontrados: " + results.size());

        for (WidgetNode widget : results) {
            LOG.info("✅ " + widget.getClassName() + " (línea " + widget.getLineNumber() + ")");
        }
    }
}