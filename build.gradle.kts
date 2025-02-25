// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    extra.apply {
        set("hilt_version", "2.49")
    }
    dependencies {
        classpath("com.google.dagger:hilt-android-gradle-plugin:${project.extra["hilt_version"]}")
    }
}


plugins {
    id("com.android.application") version "8.6.0" apply false
    id("com.android.library") version "8.6.0" apply false
    id("org.jetbrains.kotlin.android") version "1.9.23" apply false
    id("com.google.dagger.hilt.android") version "2.49" apply false
    id("com.google.devtools.ksp") version "1.9.10-1.0.13" apply false
}