plugins {
    library
    kotlin("kapt")
}

dependencies {
    implementation(projects.api)
    kapt(projects.compiler)
    compileOnly(deps.findBugs)
    testImplementation(deps.truth)
    testImplementation(deps.compiletesting)
    testImplementation(deps.junit)
    testImplementation(deps.kotlin.junit)
    testImplementation(deps.moshi)
}