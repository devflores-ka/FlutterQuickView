plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.9.25"
    id("org.jetbrains.intellij.platform") version "2.3.0"
}

group = "com.github.devflores-ka"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        create("IC", "2024.2.5")
        testFramework(org.jetbrains.intellij.platform.gradle.TestFrameworkType.Platform)
    }
}

// Configuración explícita de Java (ANTES de intellijPlatform)
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

// UNA SOLA configuración de intellijPlatform
intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild = "242"
            untilBuild = "243.*"
        }

        changeNotes = """
      Initial version
    """.trimIndent()
    }

    // Forzar Java 17
    buildSearchableOptions = false
}

tasks {
    // Set the JVM compatibility versions - JAVA 17
    withType<JavaCompile> {
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = "17"
    }
}