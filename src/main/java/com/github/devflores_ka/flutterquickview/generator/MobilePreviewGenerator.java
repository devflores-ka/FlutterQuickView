package com.github.devflores_ka.flutterquickview.generator;

import com.github.devflores_ka.flutterquickview.analyzer.models.WidgetNode;
import com.intellij.openapi.diagnostic.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Generador especializado para crear previews con apariencia móvil nativa
 */
public class MobilePreviewGenerator {
    private static final Logger LOG = Logger.getInstance(MobilePreviewGenerator.class);

    // Configuraciones de dispositivos móviles populares
    public enum MobileDevice {
        IPHONE_14(393, 852, 3.0, "iOS"),
        PIXEL_7(393, 851, 2.625, "Android"),        // ← VERIFICAR que sea 2.625 (con PUNTO)
        SAMSUNG_GALAXY_S23(393, 851, 3.0, "Android"),
        IPHONE_14_PRO_MAX(430, 932, 3.0, "iOS");

        public final int width;
        public final int height;
        public final double pixelRatio;  // ← DEBE usar punto decimal
        public final String platform;

        MobileDevice(int width, int height, double pixelRatio, String platform) {
            this.width = width;
            this.height = height;
            this.pixelRatio = pixelRatio;
            this.platform = platform;
        }
    }

    private static final MobileDevice DEFAULT_DEVICE = MobileDevice.PIXEL_7;

    /**
     * Genera un test específico para renderizado móvil realista
     */
    public static String generateMobileTest(WidgetNode widget, MobileDevice device) {
        // CORREGIR: Formato decimal con Locale.US
        String pixelRatioStr = String.format(java.util.Locale.US, "%.1f", device.pixelRatio);

        return String.format(java.util.Locale.US, """
        import 'package:flutter/material.dart';
        import 'package:flutter/services.dart';
        import 'package:flutter_test/flutter_test.dart';
        
        %s
        
        void main() {
          group('%s Mobile Preview', () {
            testWidgets('renders on %s', (WidgetTester tester) async {
              // Configurar tamaño de dispositivo específico
              await tester.binding.setSurfaceSize(Size(%d, %d));
              tester.binding.window.devicePixelRatioTestValue = %s;
              tester.binding.window.textScaleFactorTestValue = 1.0;
              
              // Simular plataforma móvil
              tester.binding.defaultBinaryMessenger.setMockMethodCallHandler(
                SystemChannels.platform,
                (call) async {
                  switch (call.method) {
                    case 'SystemChrome.setApplicationSwitcherDescription':
                    case 'SystemChrome.setSystemUIOverlayStyle':
                    case 'SystemNavigator.routeUpdated':
                      return;
                    default:
                      return null;
                  }
                },
              );
              
              // Crear app con configuración móvil nativa
              await tester.pumpWidget(
                MaterialApp(
                  debugShowCheckedModeBanner: false,
                  title: 'Mobile Preview',
                  theme: %s,
                  home: _MobilePreviewWrapper(
                    device: '%s',
                    child: %s(),
                  ),
                ),
              );
              
              // Esperar estabilización completa
              await tester.pumpAndSettle(Duration(seconds: 2));
              
              // Simular gestos móviles comunes
              await _simulateMobileInteractions(tester);
              
              // Capturar con configuración móvil
              await expectLater(
                find.byType(MaterialApp),
                matchesGoldenFile('%s_mobile_%s.png'),
              );
              
              // Limpiar
              tester.binding.window.clearDevicePixelRatioTestValue();
              tester.binding.window.clearTextScaleFactorTestValue();
            });
          });
        }
        
        // Wrapper que simula comportamiento móvil nativo
        class _MobilePreviewWrapper extends StatelessWidget {
          final String device;
          final Widget child;
          
          const _MobilePreviewWrapper({
            required this.device,
            required this.child,
          });
          
          @override
          Widget build(BuildContext context) {
            return Scaffold(
              backgroundColor: %s,
              body: SafeArea(
                child: Container(
                  width: double.infinity,
                  height: double.infinity,
                  decoration: BoxDecoration(
                    color: Theme.of(context).scaffoldBackgroundColor,
                    // Simular bordes redondeados de pantalla móvil
                    borderRadius: device.contains('iPhone') 
                      ? BorderRadius.circular(25.0)
                      : BorderRadius.circular(12.0),
                  ),
                  child: ClipRRect(
                    borderRadius: device.contains('iPhone') 
                      ? BorderRadius.circular(25.0)
                      : BorderRadius.circular(12.0),
                    child: child,
                  ),
                ),
              ),
            );
          }
        }
        
        // Simula interacciones típicas de móvil
        Future<void> _simulateMobileInteractions(WidgetTester tester) async {
          // Simular que la app está "enfocada"
          await tester.pump(Duration(milliseconds: 100));
          
          // Simular orientación portrait estable
          await tester.pump(Duration(milliseconds: 200));
          
          // Permitir animaciones de entrada
          await tester.pump(Duration(milliseconds: 300));
        }
        """,
                removeImportsFromCode(widget.getSourceCode()),
                widget.getClassName(),
                device.platform,
                device.width,
                device.height,
                pixelRatioStr,  // CORREGIDO
                generateMobileTheme(device),
                device.name(),
                widget.getClassName(),
                widget.getClassName().toLowerCase(),
                device.name().toLowerCase(),
                generateBackgroundColor(device)
        );
    }

    /**
     * Genera tema específico para la plataforma móvil
     */
    private static String generateMobileTheme(MobileDevice device) {
        if (device.platform.equals("iOS")) {
            return """
                ThemeData(
                  useMaterial3: false, // iOS usa su propio design system
                  brightness: Brightness.light,
                  primarySwatch: Colors.blue,
                  fontFamily: 'SF Pro Text', // Font nativo iOS
                  visualDensity: VisualDensity.compact,
                  // Configuraciones específicas iOS
                  appBarTheme: AppBarTheme(
                    backgroundColor: Colors.transparent,
                    elevation: 0,
                    scrolledUnderElevation: 0,
                  ),
                  cardTheme: CardTheme(
                    elevation: 1,
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(12),
                    ),
                  ),
                )""";
        } else {
            return """
                ThemeData(
                  useMaterial3: true,
                  colorScheme: ColorScheme.fromSeed(
                    seedColor: Colors.blue,
                    brightness: Brightness.light,
                  ),
                  fontFamily: 'Roboto', // Font nativo Android
                  visualDensity: VisualDensity.adaptivePlatformDensity,
                  // Configuraciones específicas Android Material 3
                  appBarTheme: AppBarTheme(
                    centerTitle: false,
                    elevation: 0,
                    scrolledUnderElevation: 3,
                  ),
                  cardTheme: CardTheme(
                    elevation: 1,
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(16),
                    ),
                  ),
                  buttonTheme: ButtonThemeData(
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(20),
                    ),
                  ),
                )""";
        }
    }

    /**
     * Genera color de fondo según la plataforma
     */
    private static String generateBackgroundColor(MobileDevice device) {
        if (device.platform.equals("iOS")) {
            return "Color(0xFFF2F2F7)"; // iOS system background
        } else {
            return "Theme.of(context).colorScheme.background"; // Android Material background
        }
    }

    /**
     * Crea configuración específica de proyecto móvil
     */
    public static void createMobileProjectConfig(Path projectDir, MobileDevice device) throws IOException {
        // Archivo de configuración específico del dispositivo
        String configContent = String.format("""
            // Configuración automática para %s
            class MobileConfig {
              static const double deviceWidth = %d;
              static const double deviceHeight = %d;
              static const double devicePixelRatio = %f;
              static const String platform = '%s';
              
              static const bool isIOS = platform == 'iOS';
              static const bool isAndroid = platform == 'Android';
              
              // Configuraciones específicas de la plataforma
              static const double defaultElevation = isIOS ? 1.0 : 3.0;
              static const double defaultBorderRadius = isIOS ? 12.0 : 16.0;
              static const String defaultFontFamily = isIOS ? 'SF Pro Text' : 'Roboto';
            }
            """,
                device.name(),
                device.width,
                device.height,
                device.pixelRatio,
                device.platform
        );

        Files.write(projectDir.resolve("lib/mobile_config.dart"), configContent.getBytes());

        // Flutter driver config para testing móvil
        String driverConfig = """
            // Configuración de Flutter Driver para testing móvil
            import 'package:flutter_driver/driver_extension.dart';
            import 'package:flutter/material.dart';
            import 'mobile_config.dart';
            
            void main() {
              // Habilitar driver extension para automation
              enableFlutterDriverExtension();
              
              // Configurar app para testing móvil
              runApp(MyApp());
            }
            
            class MyApp extends StatelessWidget {
              @override
              Widget build(BuildContext context) {
                return MaterialApp(
                  debugShowCheckedModeBanner: false,
                  title: 'Mobile Preview App',
                  theme: MobileConfig.isIOS ? _iOSTheme() : _androidTheme(),
                  home: Container(), // Placeholder
                );
              }
              
              ThemeData _iOSTheme() {
                return ThemeData(
                  brightness: Brightness.light,
                  primarySwatch: Colors.blue,
                  fontFamily: MobileConfig.defaultFontFamily,
                  visualDensity: VisualDensity.compact,
                );
              }
              
              ThemeData _androidTheme() {
                return ThemeData(
                  useMaterial3: true,
                  colorScheme: ColorScheme.fromSeed(seedColor: Colors.blue),
                  fontFamily: MobileConfig.defaultFontFamily,
                  visualDensity: VisualDensity.adaptivePlatformDensity,
                );
              }
            }
            """;

        Files.write(projectDir.resolve("lib/main_driver.dart"), driverConfig.getBytes());
    }

    /**
     * Genera pubspec.yaml optimizado para renderizado móvil
     */
    public static String generateMobilePubspec(MobileDevice device) {
        return String.format("""
            name: flutter_mobile_preview_%s
            description: Mobile preview for %s device
            version: 1.0.0+1
            publish_to: 'none'
            
            environment:
              sdk: '>=3.0.0 <4.0.0'
              flutter: ">=3.10.0"
            
            dependencies:
              flutter:
                sdk: flutter
              cupertino_icons: ^1.0.6
              %s
              
            dev_dependencies:
              flutter_test:
                sdk: flutter
              flutter_driver:
                sdk: flutter
              integration_test:
                sdk: flutter
              test: any
              
            flutter:
              uses-material-design: true
              
              # Fuentes nativas según plataforma
              fonts:
                - family: %s
                  fonts:
                    - asset: fonts/%s-Regular.ttf
                    - asset: fonts/%s-Medium.ttf
                      weight: 500
                    - asset: fonts/%s-Bold.ttf
                      weight: 700
            """,
                device.name().toLowerCase(),
                device.platform,
                device.platform.equals("iOS") ?
                        "# iOS dependencies\n  cupertino_icons: ^1.0.6" :
                        "# Android dependencies\n  material_color_utilities: ^0.5.0",
                device.platform.equals("iOS") ? "SF Pro Text" : "Roboto",
                device.platform.equals("iOS") ? "SFProText" : "Roboto",
                device.platform.equals("iOS") ? "SFProText" : "Roboto",
                device.platform.equals("iOS") ? "SFProText" : "Roboto"
        );
    }

    /**
     * Formatea device.pixelRatio de manera segura para cualquier Locale
     */
    public static String formatPixelRatio(double pixelRatio) {
        return String.format(java.util.Locale.US, "%.1f", pixelRatio);
    }

    private static String removeImportsFromCode(String sourceCode) {
        return sourceCode.replaceAll("^import\\s+[^;]+;\\s*\n?", "").trim();
    }
}