rootProject.name = "mordant-build"

include(
    "mordant",
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
