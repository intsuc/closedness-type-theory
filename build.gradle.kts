plugins {
    alias(libs.plugins.jvm)
    application
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.clikt)
    implementation(libs.kotlinxCollectionsImmutable)
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
    applicationName = "ctt"
    mainClass = "dev.intsuc.ctt.MainKt"
}
