package com.github.devflores_ka.flutterquickview;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import org.jetbrains.annotations.NotNull;

/**
 * Servicio a nivel de aplicación para FlutterQuickView
 * Se inicializa una vez cuando se carga el IDE y se mantiene durante toda la sesión
 */
@Service(Service.Level.APP)
public final class FlutterQuickViewApplicationComponent {
    private static final Logger LOG = Logger.getInstance(FlutterQuickViewApplicationComponent.class);

    /**
     * Constructor del servicio - se llama automáticamente
     */
    public FlutterQuickViewApplicationComponent() {
        initialize();
    }

    /**
     * Inicializa el servicio
     */
    private void initialize() {
        LOG.info("Inicializando FlutterQuickView Application Service");

        // Inicializar configuraciones globales
        initializeGlobalSettings();

        // Registrar listeners globales
        registerGlobalListeners();

        LOG.info("FlutterQuickView Application Service inicializado correctamente");
    }

    /**
     * Obtiene la instancia del servicio de aplicación
     */
    public static FlutterQuickViewApplicationComponent getInstance() {
        return ApplicationManager.getApplication().getService(FlutterQuickViewApplicationComponent.class);
    }

    /**
     * Inicializa configuraciones globales del plugin
     */
    private void initializeGlobalSettings() {
        try {
            // Configurar valores por defecto
            LOG.debug("Configurando ajustes globales por defecto");

            // Aquí puedes inicializar configuraciones que aplican a todos los proyectos
            // Por ejemplo: temas por defecto, configuraciones de renderizado, etc.

        } catch (Exception e) {
            LOG.error("Error inicializando configuraciones globales", e);
        }
    }

    /**
     * Registra listeners globales
     */
    private void registerGlobalListeners() {
        try {
            LOG.debug("Registrando listeners globales");

            // Aquí puedes registrar listeners que funcionen a nivel de aplicación
            // Por ejemplo: cambios en el sistema de archivos, cambios de tema del IDE, etc.

        } catch (Exception e) {
            LOG.error("Error registrando listeners globales", e);
        }
    }

    /**
     * Limpia recursos globales al cerrar
     */
    private void cleanupGlobalResources() {
        try {
            LOG.debug("Limpiando recursos globales");

            // Aquí limpias cualquier recurso que hayas creado
            // Por ejemplo: cerrar conexiones, limpiar caches, etc.

        } catch (Exception e) {
            LOG.error("Error limpiando recursos globales", e);
        }
    }

    /**
     * Verifica si el plugin está inicializado correctamente
     */
    public boolean isInitialized() {
        return true; // Por ahora siempre true, puedes agregar validaciones
    }
}