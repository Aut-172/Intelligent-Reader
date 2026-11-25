plugins {
    kotlin("multiplatform")
}

kotlin {
    jvmToolchain(21)

    // W-E01: the only target under test is the Windows JVM target.
    jvm()

    sourceSets {
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}
