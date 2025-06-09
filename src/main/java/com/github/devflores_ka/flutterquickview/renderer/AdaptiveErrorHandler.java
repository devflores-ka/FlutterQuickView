package com.github.devflores_ka.flutterquickview.renderer;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Manejador adaptativo de errores que aprende de fallos y auto-repara configuraciones
 */
public class AdaptiveErrorHandler {
    private static final Logger LOG = Logger.getInstance(AdaptiveErrorHandler.class);

    private final Project project;
    private final Map<String, Integer> errorPatternCounts = new HashMap<>();
    private final List<AutoRepairStrategy> learnedStrategies = new ArrayList<>();

    public AdaptiveErrorHandler(Project project) {
        this.project = project;
        initializeKnownPatterns();
    }

    /**
     * Analiza un error y sugiere una estrategia de reparación automática
     */
    public AutoRepairSuggestion analyzeError(Exception error, String pubspecContent) {
        String errorMessage = error.getMessage();
        LOG.info("🔍 Analizando error para auto-reparación: " + errorMessage.substring(0, Math.min(100, errorMessage.length())));

        // Detectar patrones de error conocidos
        ErrorPattern detectedPattern = detectErrorPattern(errorMessage);

        if (detectedPattern != null) {
            // Incrementar contador para aprendizaje
            errorPatternCounts.merge(detectedPattern.name(), 1, Integer::sum);

            // Generar sugerencia de reparación
            AutoRepairSuggestion suggestion = generateRepairSuggestion(detectedPattern, pubspecContent);

            LOG.info("✨ Estrategia de auto-reparación generada: " + suggestion.description);
            return suggestion;
        }

        // Si no se reconoce el patrón, usar estrategia genérica
        return generateGenericRepairSuggestion(errorMessage, pubspecContent);
    }

    /**
     * Detecta patrones de error conocidos
     */
    private ErrorPattern detectErrorPattern(String errorMessage) {
        // Patrón 1: Conflictos de versiones de dependencias
        if (Pattern.compile("version solving failed|dependency conflict|incompatible versions", Pattern.CASE_INSENSITIVE)
                .matcher(errorMessage).find()) {
            return ErrorPattern.VERSION_CONFLICT;
        }

        // Patrón 2: Dependencias pinneadas por Flutter SDK
        if (Pattern.compile("pinned to version.*by.*from the flutter SDK", Pattern.CASE_INSENSITIVE)
                .matcher(errorMessage).find()) {
            return ErrorPattern.SDK_PINNED_DEPENDENCY;
        }

        // Patrón 3: integration_test conflicts
        if (errorMessage.contains("integration_test") && errorMessage.contains("forbidden")) {
            return ErrorPattern.INTEGRATION_TEST_CONFLICT;
        }

        // Patrón 4: material_color_utilities conflicts
        if (errorMessage.contains("material_color_utilities")) {
            return ErrorPattern.MATERIAL_COLOR_UTILITIES_CONFLICT;
        }

        // Patrón 5: build_runner conflicts
        if (errorMessage.contains("build_runner") || errorMessage.contains("source_gen")) {
            return ErrorPattern.BUILD_RUNNER_CONFLICT;
        }

        // Patrón 6: Flutter SDK version mismatch
        if (Pattern.compile("requires Flutter SDK version|flutter.*not compatible", Pattern.CASE_INSENSITIVE)
                .matcher(errorMessage).find()) {
            return ErrorPattern.FLUTTER_VERSION_MISMATCH;
        }

        // Patrón 7: Dart SDK constraints
        if (errorMessage.contains("Dart SDK constraint") || errorMessage.contains("sdk constraint")) {
            return ErrorPattern.DART_SDK_CONSTRAINT;
        }

        return null; // Patrón no reconocido
    }

    /**
     * Genera sugerencia de reparación basada en el patrón detectado
     */
    private AutoRepairSuggestion generateRepairSuggestion(ErrorPattern pattern, String pubspecContent) {
        switch (pattern) {
            case VERSION_CONFLICT:
                return createVersionConflictRepair(pubspecContent);

            case SDK_PINNED_DEPENDENCY:
                return createSdkPinnedRepair(pubspecContent);

            case INTEGRATION_TEST_CONFLICT:
                return createIntegrationTestRepair(pubspecContent);

            case MATERIAL_COLOR_UTILITIES_CONFLICT:
                return createMaterialColorUtilitiesRepair(pubspecContent);

            case BUILD_RUNNER_CONFLICT:
                return createBuildRunnerRepair(pubspecContent);

            case FLUTTER_VERSION_MISMATCH:
                return createFlutterVersionRepair(pubspecContent);

            case DART_SDK_CONSTRAINT:
                return createDartSdkRepair(pubspecContent);

            default:
                return generateGenericRepairSuggestion("Unknown pattern", pubspecContent);
        }
    }

    /**
     * Crea reparación para conflictos de versiones
     */
    private AutoRepairSuggestion createVersionConflictRepair(String pubspecContent) {
        String repairedPubspec = pubspecContent
                .replaceAll("material_color_utilities: \\^[0-9.]+", "")
                .replaceAll("integration_test:[\\s\\S]*?sdk: flutter", "")
                .replaceAll("flutter_driver:[\\s\\S]*?sdk: flutter", "");

        // Limpiar líneas vacías extra
        repairedPubspec = repairedPubspec.replaceAll("\n\\s*\n\\s*\n", "\n\n");

        return new AutoRepairSuggestion(
                RepairType.REMOVE_CONFLICTING_DEPENDENCIES,
                repairedPubspec,
                "Eliminadas dependencias conflictivas detectadas automáticamente",
                85
        );
    }

    /**
     * Crea reparación para dependencias pinneadas por SDK
     */
    private AutoRepairSuggestion createSdkPinnedRepair(String pubspecContent) {
        // Remover todas las dependencias que suelen estar pinneadas
        String repairedPubspec = pubspecContent
                .replaceAll("material_color_utilities:.*", "")
                .replaceAll("meta:.*", "")
                .replaceAll("analyzer:.*", "")
                .replaceAll("integration_test:.*", "")
                .replaceAll("flutter_driver:.*", "");

        return new AutoRepairSuggestion(
                RepairType.REMOVE_SDK_PINNED_DEPENDENCIES,
                repairedPubspec,
                "Eliminadas dependencias pinneadas por Flutter SDK",
                90
        );
    }

    /**
     * Crea reparación específica para integration_test
     */
    private AutoRepairSuggestion createIntegrationTestRepair(String pubspecContent) {
        String repairedPubspec = pubspecContent
                .replaceAll("integration_test:[\\s\\S]*?sdk: flutter", "")
                .replaceAll("flutter_driver:[\\s\\S]*?sdk: flutter", "");

        return new AutoRepairSuggestion(
                RepairType.REMOVE_INTEGRATION_TEST,
                repairedPubspec,
                "Eliminado integration_test para evitar conflictos de dependencias",
                95
        );
    }

    /**
     * Crea reparación para material_color_utilities
     */
    private AutoRepairSuggestion createMaterialColorUtilitiesRepair(String pubspecContent) {
        String repairedPubspec = pubspecContent
                .replaceAll("material_color_utilities:.*", "# material_color_utilities removed due to version conflicts");

        return new AutoRepairSuggestion(
                RepairType.REMOVE_MATERIAL_COLOR_UTILITIES,
                repairedPubspec,
                "Eliminado material_color_utilities por conflicto de versiones con Flutter SDK",
                95
        );
    }

    /**
     * Crea reparación para build_runner
     */
    private AutoRepairSuggestion createBuildRunnerRepair(String pubspecContent) {
        String repairedPubspec = pubspecContent
                .replaceAll("build_runner:.*", "")
                .replaceAll("source_gen:.*", "")
                .replaceAll("json_serializable:.*", "");

        return new AutoRepairSuggestion(
                RepairType.REMOVE_BUILD_TOOLS,
                repairedPubspec,
                "Eliminadas herramientas de build que causaban conflictos",
                80
        );
    }

    /**
     * Crea reparación para versión de Flutter
     */
    private AutoRepairSuggestion createFlutterVersionRepair(String pubspecContent) {
        String repairedPubspec = pubspecContent
                .replaceAll("flutter: \"[^\"]*\"", "flutter: \">=3.10.0\"");

        return new AutoRepairSuggestion(
                RepairType.ADJUST_FLUTTER_VERSION,
                repairedPubspec,
                "Ajustada versión de Flutter a constraint compatible",
                75
        );
    }

    /**
     * Crea reparación para Dart SDK
     */
    private AutoRepairSuggestion createDartSdkRepair(String pubspecContent) {
        String repairedPubspec = pubspecContent
                .replaceAll("sdk: '[^']*'", "sdk: '>=3.0.0 <4.0.0'");

        return new AutoRepairSuggestion(
                RepairType.ADJUST_DART_VERSION,
                repairedPubspec,
                "Ajustada versión de Dart SDK a constraint compatible",
                80
        );
    }

    /**
     * Genera sugerencia genérica cuando no se reconoce el patrón
     */
    private AutoRepairSuggestion generateGenericRepairSuggestion(String errorMessage, String pubspecContent) {
        // Estrategia genérica: pubspec ultra-minimalista
        String minimalPubspec = """
            name: flutter_emergency_preview
            description: Emergency minimal preview project
            version: 1.0.0+1
            publish_to: 'none'
            
            environment:
              sdk: '>=3.0.0 <4.0.0'
            
            dependencies:
              flutter:
                sdk: flutter
                
            dev_dependencies:
              flutter_test:
                sdk: flutter
                
            flutter:
              uses-material-design: true
            """;

        return new AutoRepairSuggestion(
                RepairType.EMERGENCY_MINIMAL,
                minimalPubspec,
                "Configuración de emergencia ultra-minimalista (patrón de error no reconocido)",
                70
        );
    }

    /**
     * Inicializa patrones de error conocidos
     */
    private void initializeKnownPatterns() {
        // Registrar estrategias aprendidas de errores comunes
        learnedStrategies.add(new AutoRepairStrategy(
                "material_color_utilities_conflict",
                "Conflicto con material_color_utilities pinneada por SDK",
                RepairType.REMOVE_MATERIAL_COLOR_UTILITIES,
                95
        ));

        learnedStrategies.add(new AutoRepairStrategy(
                "integration_test_forbidden",
                "integration_test prohibida por conflictos de versión",
                RepairType.REMOVE_INTEGRATION_TEST,
                98
        ));

        learnedStrategies.add(new AutoRepairStrategy(
                "version_solving_failed",
                "Fallo general en resolución de versiones",
                RepairType.REMOVE_CONFLICTING_DEPENDENCIES,
                85
        ));

        LOG.info("🧠 Inicializadas " + learnedStrategies.size() + " estrategias de auto-reparación");
    }

    /**
     * Aplica una sugerencia de reparación automáticamente
     */
    public RepairResult applyRepairSuggestion(AutoRepairSuggestion suggestion, String originalError) {
        try {
            LOG.info("🔧 Aplicando auto-reparación: " + suggestion.description);

            // Registrar intento de reparación para aprendizaje
            recordRepairAttempt(suggestion, originalError);

            return new RepairResult(
                    true,
                    suggestion.repairedPubspec,
                    suggestion.description,
                    suggestion.successProbability
            );

        } catch (Exception e) {
            LOG.error("❌ Error aplicando auto-reparación", e);
            return new RepairResult(
                    false,
                    null,
                    "Error aplicando reparación: " + e.getMessage(),
                    0
            );
        }
    }

    /**
     * Registra intento de reparación para machine learning futuro
     */
    private void recordRepairAttempt(AutoRepairSuggestion suggestion, String originalError) {
        // Por ahora solo logging, en el futuro podríamos persistir para ML
        LOG.info("📊 Registrando intento de reparación: " + suggestion.type + " para error que contiene: " +
                originalError.substring(0, Math.min(50, originalError.length())));
    }

    /**
     * Obtiene estadísticas de patrones de error aprendidos
     */
    public String getErrorLearningStats() {
        StringBuilder stats = new StringBuilder();
        stats.append("Patrones de error detectados:\n");

        errorPatternCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .forEach(entry ->
                        stats.append("  - ").append(entry.getKey()).append(": ").append(entry.getValue()).append(" veces\n"));

        return stats.toString();
    }

    /**
     * Predice la probabilidad de éxito de una reparación
     */
    public int predictRepairSuccessProbability(ErrorPattern pattern, String pubspecContent) {
        // Lógica simple de predicción basada en experiencia acumulada
        int baseSuccessRate = getBaseSuccessRate(pattern);

        // Ajustar basado en complejidad del pubspec
        int complexityPenalty = calculateComplexityPenalty(pubspecContent);

        // Ajustar basado en experiencia previa
        int experienceBonus = getExperienceBonus(pattern);

        return Math.max(10, Math.min(95, baseSuccessRate - complexityPenalty + experienceBonus));
    }

    private int getBaseSuccessRate(ErrorPattern pattern) {
        switch (pattern) {
            case INTEGRATION_TEST_CONFLICT: return 95;
            case MATERIAL_COLOR_UTILITIES_CONFLICT: return 90;
            case SDK_PINNED_DEPENDENCY: return 85;
            case VERSION_CONFLICT: return 80;
            case BUILD_RUNNER_CONFLICT: return 75;
            case FLUTTER_VERSION_MISMATCH: return 70;
            case DART_SDK_CONSTRAINT: return 75;
            default: return 60;
        }
    }

    private int calculateComplexityPenalty(String pubspecContent) {
        int dependencies = (int) pubspecContent.lines()
                .filter(line -> line.trim().matches("^[a-zA-Z_][a-zA-Z0-9_]*:.*"))
                .count();

        if (dependencies > 20) return 15;
        if (dependencies > 10) return 8;
        if (dependencies > 5) return 3;
        return 0;
    }

    private int getExperienceBonus(ErrorPattern pattern) {
        String patternName = pattern.name();
        Integer count = errorPatternCounts.get(patternName);

        if (count == null) return 0;
        if (count > 5) return 10;  // Mucha experiencia
        if (count > 2) return 5;   // Alguna experiencia
        return 0;
    }

    /**
     * Genera una estrategia de reparación en cascada (múltiples intentos)
     */
    public List<AutoRepairSuggestion> generateCascadeRepairStrategy(String errorMessage, String pubspecContent) {
        List<AutoRepairSuggestion> cascade = new ArrayList<>();

        // Estrategia 1: Reparación específica del error
        ErrorPattern pattern = detectErrorPattern(errorMessage);
        if (pattern != null) {
            cascade.add(generateRepairSuggestion(pattern, pubspecContent));
        }

        // Estrategia 2: Reparación conservadora (eliminar dependencias problemáticas)
        cascade.add(new AutoRepairSuggestion(
                RepairType.CONSERVATIVE_CLEANUP,
                generateConservativeCleanupPubspec(pubspecContent),
                "Limpieza conservadora: eliminadas dependencias conocidas como problemáticas",
                80
        ));

        // Estrategia 3: Configuración minimalista
        cascade.add(new AutoRepairSuggestion(
                RepairType.MINIMAL_FALLBACK,
                generateMinimalFallbackPubspec(),
                "Fallback minimalista: solo dependencias esenciales de Flutter",
                90
        ));

        // Estrategia 4: Emergencia (solo Flutter)
        cascade.add(new AutoRepairSuggestion(
                RepairType.EMERGENCY_MINIMAL,
                generateEmergencyPubspec(),
                "Configuración de emergencia: Flutter puro sin extras",
                95
        ));

        return cascade;
    }

    private String generateConservativeCleanupPubspec(String original) {
        return original
                .replaceAll("material_color_utilities:.*", "")
                .replaceAll("integration_test:.*", "")
                .replaceAll("flutter_driver:.*", "")
                .replaceAll("build_runner:.*", "")
                .replaceAll("source_gen:.*", "")
                .replaceAll("analyzer:.*", "")
                .replaceAll("meta:.*", "")
                .replaceAll("\n\\s*\n", "\n");
    }

    private String generateMinimalFallbackPubspec() {
        return """
            name: flutter_minimal_fallback
            description: Minimal fallback preview project
            version: 1.0.0+1
            publish_to: 'none'
            
            environment:
              sdk: '>=3.0.0 <4.0.0'
            
            dependencies:
              flutter:
                sdk: flutter
              cupertino_icons: ^1.0.6
                
            dev_dependencies:
              flutter_test:
                sdk: flutter
                
            flutter:
              uses-material-design: true
            """;
    }

    private String generateEmergencyPubspec() {
        return """
            name: flutter_emergency
            description: Emergency Flutter-only project
            version: 1.0.0+1
            publish_to: 'none'
            
            environment:
              sdk: '>=3.0.0 <4.0.0'
            
            dependencies:
              flutter:
                sdk: flutter
                
            dev_dependencies:
              flutter_test:
                sdk: flutter
                
            flutter:
              uses-material-design: true
            """;
    }

    // Enums y clases de datos

    public enum ErrorPattern {
        VERSION_CONFLICT,
        SDK_PINNED_DEPENDENCY,
        INTEGRATION_TEST_CONFLICT,
        MATERIAL_COLOR_UTILITIES_CONFLICT,
        BUILD_RUNNER_CONFLICT,
        FLUTTER_VERSION_MISMATCH,
        DART_SDK_CONSTRAINT
    }

    public enum RepairType {
        REMOVE_CONFLICTING_DEPENDENCIES,
        REMOVE_SDK_PINNED_DEPENDENCIES,
        REMOVE_INTEGRATION_TEST,
        REMOVE_MATERIAL_COLOR_UTILITIES,
        REMOVE_BUILD_TOOLS,
        ADJUST_FLUTTER_VERSION,
        ADJUST_DART_VERSION,
        CONSERVATIVE_CLEANUP,
        MINIMAL_FALLBACK,
        EMERGENCY_MINIMAL
    }

    public static class AutoRepairSuggestion {
        public final RepairType type;
        public final String repairedPubspec;
        public final String description;
        public final int successProbability; // 0-100

        public AutoRepairSuggestion(RepairType type, String repairedPubspec, String description, int successProbability) {
            this.type = type;
            this.repairedPubspec = repairedPubspec;
            this.description = description;
            this.successProbability = successProbability;
        }
    }

    public static class RepairResult {
        public final boolean success;
        public final String repairedContent;
        public final String description;
        public final int probability;

        public RepairResult(boolean success, String repairedContent, String description, int probability) {
            this.success = success;
            this.repairedContent = repairedContent;
            this.description = description;
            this.probability = probability;
        }
    }

    public static class AutoRepairStrategy {
        public final String patternId;
        public final String description;
        public final RepairType repairType;
        public final int successRate;

        public AutoRepairStrategy(String patternId, String description, RepairType repairType, int successRate) {
            this.patternId = patternId;
            this.description = description;
            this.repairType = repairType;
            this.successRate = successRate;
        }
    }
}