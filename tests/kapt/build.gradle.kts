plugins {
    `test-library`
    kotlin("kapt")
}

dependencies {
    kapt(projects.compiler)
}