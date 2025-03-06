plugins {
    alias(libs.plugins.jvm)
    application
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.clikt)
    implementation(libs.lsp4j)
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(23)
}

application {
    mainClass = "dev.intsuc.ctt.MainKt"
}
