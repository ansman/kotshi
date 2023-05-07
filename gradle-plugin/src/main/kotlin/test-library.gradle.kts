import org.gradle.accessors.dm.LibrariesForLibs

plugins {
    id("library")
}

val libs = the<LibrariesForLibs>()

sourceSets {
    getByName("main") {
        java.srcDir(rootDir.resolve("tests/src/main/kotlin"))
    }
    getByName("test") {
        java.srcDirs(rootDir.resolve("tests/src/test/kotlin"))
    }
}

tasks.withType<Test>().configureEach {
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
    implementation(project(":api"))
    implementation(project(":compiler"))
    implementation(libs.moshi.current)
    compileOnly(libs.findBugs)
    testImplementation(libs.compileTesting.core)
}