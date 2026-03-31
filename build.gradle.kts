// Top-level build file
buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        // Aapke latest versions ke saath compatible
        classpath("com.android.tools.build:gradle:8.7.0")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.0.21")
    }
}

plugins {
    id("com.android.application") version "8.7.0" apply false
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21" apply false

    // Kapt ko hata kar KSP add karo jo Kotlin 2.0.21 ko support kare
    id("com.google.devtools.ksp") version "2.0.21-1.0.25" apply false

    alias(libs.plugins.google.gms.google.services) apply false
}