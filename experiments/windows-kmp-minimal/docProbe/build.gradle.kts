plugins {
    kotlin("jvm")
    application
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(project(":shared"))
    implementation("org.apache.pdfbox:pdfbox:3.0.5")
    implementation("org.slf4j:slf4j-nop:2.0.16")
}

application {
    mainClass.set("reader.windowsmin.docprobe.MainKt")
}
