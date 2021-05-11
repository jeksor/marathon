buildscript {
    repositories {
        mavenCentral()
        google()
        maven { url = uri("https://jitpack.io") }
    }
    dependencies {
        classpath(BuildPlugins.kotlinPlugin)
        classpath(BuildPlugins.androidGradle)
        classpath("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.1")
    }
}

allprojects {
    buildscript {
        repositories {
            mavenCentral()
            google()
            maven { url = uri("https://jitpack.io") }
        }
    }

    repositories {
        maven { url = uri("$rootDir/../build/repository") }
        mavenCentral()
        google()
    }
}
