plugins {
    id("library")
    alias(libs.plugins.ksp)
}

tasks.withType<Test>().configureEach {
    systemProperty("usingLegacyMoshi", providers.gradleProperty("kotshi.internal.useLegacyMoshi").orElse("false").get())
    maxHeapSize = "1g"
    jvmArgs(
        "--add-opens=jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED",
        "--add-opens=jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED",
        "--add-opens=jdk.compiler/com.sun.tools.javac.comp=ALL-UNNAMED",
        "--add-opens=jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED",
        "--add-opens=jdk.compiler/com.sun.tools.javac.jvm=ALL-UNNAMED",
        "--add-opens=jdk.compiler/com.sun.tools.javac.main=ALL-UNNAMED",
        "--add-opens=jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED",
        "--add-opens=jdk.compiler/com.sun.tools.javac.processing=ALL-UNNAMED",
        "--add-opens=jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED",
        "--add-opens=jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED"
    )
}
dependencies {
    implementation(projects.api)
    implementation(projects.compiler)
    ksp(projects.compiler)

    if (providers.gradleProperty("kotshi.internal.useLegacyMoshi").orNull?.toBooleanStrict() == true) {
        compileOnly(libs.moshi.latest)
        testImplementation(libs.oldestSupportedMoshi)
    } else {
        implementation(libs.moshi.latest)
    }
    compileOnly(libs.findBugs)

    testRuntimeOnly(libs.ksp)
    testImplementation(libs.compileTesting.core)
    testImplementation(libs.ksp.api)
    testImplementation(libs.ksp.commonDeps)
    testImplementation(libs.ksp.aaEmbeddable)
    testImplementation(libs.compileTesting.ksp)
}