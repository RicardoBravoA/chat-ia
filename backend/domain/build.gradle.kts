plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    implementation(libs.kotlinx.datetime)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.junit.jupiter)
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
}
