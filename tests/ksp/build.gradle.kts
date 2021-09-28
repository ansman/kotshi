import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

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

if (providers.gradleProperty("kotshi.createAnnotationsUsingConstructor").forUseAtConfigurationTime().orNull.toBoolean()) {
    tasks.withType<KotlinCompile> {
        kotlinOptions {
            languageVersion = "1.6"
        }
    }
    ksp {
        arg("kotshi.createAnnotationsUsingConstructor", "true")
    }
}