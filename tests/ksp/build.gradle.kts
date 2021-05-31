plugins {
    `test-library`
    id("com.google.devtools.ksp") version deps.ksp.version
}

sourceSets {
    main {
        java {
            srcDir(buildDir.resolve("generated/ksp/main/kotlin"))
        }
    }
    test {
        java {
            srcDir(buildDir.resolve("generated/ksp/test/kotlin"))
        }
    }
}

dependencies {
    ksp(projects.compiler)
}