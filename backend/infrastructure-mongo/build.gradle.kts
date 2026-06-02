plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    implementation(project(":domain"))
    implementation(libs.kotlinx.datetime)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.mongodb.kotlin.coroutine)
    implementation(libs.bcrypt)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.junit.jupiter)
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
}
