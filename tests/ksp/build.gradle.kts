plugins {
    `test-library`
    id("com.google.devtools.ksp") version deps.ksp.version
}

kotlin {
    sourceSets.main {
        kotlin.srcDir("build/generated/ksp/main/kotlin")
    }
    sourceSets.test {
        kotlin.srcDir("build/generated/ksp/test/kotlin")
    }
}

dependencies {
    ksp(projects.compiler)
}

val createAnnotationsUsingConstructor = providers.gradleProperty("kotshi.createAnnotationsUsingConstructor")
    .forUseAtConfigurationTime()
    .orNull

if (createAnnotationsUsingConstructor != null) {
    ksp {
        arg("kotshi.createAnnotationsUsingConstructor", createAnnotationsUsingConstructor)
    }
}