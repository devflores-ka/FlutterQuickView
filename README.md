# FlutterQuickView 🚀

> Plugin para Android Studio / IntelliJ que permite previsualizar widgets Flutter directamente en el IDE, inspirado en Jetpack Compose Preview.

## ✨ Visión

**FlutterQuickView** busca brindar una experiencia de desarrollo más rápida y fluida para aplicaciones Flutter, permitiendo a los desarrolladores **renderizar vistas previas de widgets individuales** sin necesidad de ejecutar toda la aplicación. De esta forma, se logra una productividad similar a la que ofrece Jetpack Compose en Android Studio con su funcionalidad `@Preview`.

---

## 🎯 Objetivo del Plugin

- Permitir la **previsualización inmediata** de widgets Flutter directamente desde el editor.
- Facilitar pruebas visuales rápidas durante el desarrollo de UI.
- Evitar tener que ejecutar toda la app solo para ver una pantalla o componente.

---

## 🧩 ¿Cómo funciona?

FlutterQuickView detecta widgets de previsualización definidos dentro del mismo archivo `.dart`, como por ejemplo:

```dart
class LoginScreenPreview extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: LoginScreen(),
    );
  }
}
```
---
## Luego:

1. El plugin analiza el archivo y detecta clases que terminan en Preview.

2. Genera automáticamente un archivo temporal preview.dart.

3. Lanza un proceso flutter run con ese archivo como target.

4. Muestra el resultado directamente en una Tool Window del IDE.

---
## Estructura actual del proyecto

src/
└── main/
    └── java/
        └── com.github.devflores_ka.flutterquickview/
            ├── analyzer/              # Análisis de código Dart y AST
            │   ├── models/
            │   │   └── WidgetNode.java
            │   ├── AstVisitor.java
            │   └── FlutterCodeAnalyzer.java
            ├── renderer/              # Renderizado y procesos de Flutter
            │   ├── process/
            │   │   └── FlutterProcessManager.java
            │   └── FlutterRenderService.java
            ├── settings/              # Configuración del plugin
            │   └── PluginSettings.java
            ├── ui/                    # Interfaz de usuario del plugin
            │   ├── actions/
            │   │   └── PreviewAction.java
            │   └── PreviewToolWindowsFactory.java
            └── FlutterQuickViewBundle.java

---
## Estado actual

En desarrollo. Actualmente estamos trabajando en:

  Análisis básico de archivos .dart para detección de clases *Preview.

  Generación de archivos temporales para previsualización.

  Renderizado usando procesos flutter run.

## Roadmap

 [ ] Detectar widgets *Preview en archivos abiertos.

 [ ] Generar y ejecutar preview.dart automáticamente.

 [ ] Capturar imagen o embebido directo del render.

 [ ] Mostrar la vista previa en una Tool Window.

 [ ] Agregar soporte para temas claros/oscuro.

 [ ] Agregar recarga en caliente (hot reload).

 [ ] Personalizar tamaño, DPI, orientación de la preview.

---
## Licencia

MIT License — libre para uso y modificación. Agradecemos contribuciones.

---
## Autor

Desarrollado por @devflores_ka — Proyecto open source pensado para mejorar la experiencia Flutter en el desarrollo diario.
