rootProject.name = "mordant-build"

include(
    "mordant",
    "mordant-omnibus",
    "mordant-jvm-jna",
    "mordant-coroutines",
    "mordant-markdown",
)

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}
