plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    application
}

dependencies {
    implementation(project(":domain"))
    implementation(project(":application"))
    implementation(project(":infrastructure-mongo"))
    implementation(libs.mongodb.kotlin.coroutine)

    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.content.negotiation)
    implementation("io.ktor:ktor-server-websockets:2.3.12")
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.ktor.server.auth)
    implementation(libs.ktor.server.status.pages)
    implementation(libs.ktor.server.call.logging)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.datetime)
    implementation(libs.logback.classic)

    testImplementation(libs.kotlin.test)
    testImplementation(libs.junit.jupiter)
}

application {
    mainClass.set("com.bank.banking.api.ApplicationKt")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
}
