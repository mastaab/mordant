plugins {
    id("mordant-kotlin-conventions")
    id("mordant-publishing-conventions")
}

kotlin {
    jvm()
    sourceSets {
        jvmMain.dependencies {
            implementation(project(":mordant"))
            implementation(project(":mordant-jvm-jna"))
        }
    }
}
