rootProject.name = "asmifier"

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}
buildscript {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
    dependencies {
        classpath("com.likethesalad.tools:artifact-publisher:3.5.0")
    }
}
dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
    repositories {
        mavenCentral()
    }
}

include(":asmifier-plugin")