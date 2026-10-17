// spec 100 · :core es Kotlin JVM puro: si algo importa android.*, no compila (reglas 5 y 14).
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    // Solo para leer los vectores de prueba (101, 103).
    testImplementation(libs.kotlinx.serialization.json)
}

// Ruta de los vectores compartidos para las pruebas (spec 100, sección 3).
val vectorsDir = rootProject.projectDir.resolve("../specs/shared/vectors").normalize().absolutePath

// Las pruebas de reglas del proyecto (A5) leen core/src y el catálogo de versiones.
val androidRootDir = rootProject.projectDir.absolutePath
val versionCatalog = rootProject.file("gradle/libs.versions.toml")

tasks.test {
    systemProperty("vectorsDir", vectorsDir)
    systemProperty("androidRootDir", androidRootDir)
    // Si cambia el catálogo o algún vector, las pruebas vuelven a correr (no pasan por caché con datos viejos).
    inputs.file(versionCatalog)
    inputs.files(fileTree(vectorsDir))
}
