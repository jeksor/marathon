buildscript {
    repositories {
        jcenter()
        mavenCentral()
        google()
    }
    dependencies {
        classpath(BuildPlugins.kotlinPlugin)
        classpath(BuildPlugins.androidGradle)
    }
}

allprojects {
    repositories {
        maven { url = uri("$rootDir/../build/repository") }
        jcenter()
        mavenCentral()
        google()
        maven { url = uri("https://dl.bintray.com/qameta/maven") }
    }
}
