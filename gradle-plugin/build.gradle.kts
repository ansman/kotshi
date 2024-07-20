plugins {
    kotlin("jvm") version embeddedKotlinVersion
    `kotlin-dsl`
    `version-catalog`
}

dependencies {
    implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))
    api(libs.kotlin.gradlePlugin)
    api(libs.dokka.gradlePlugin)
    api(libs.sonatypePublishFix)
    implementation(gradleKotlinDsl())
}

java.toolchain {
    languageVersion.set(JavaLanguageVersion.of(21))
}