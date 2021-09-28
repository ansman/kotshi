import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `test-library`
    kotlin("kapt")
}

dependencies {
    kapt(projects.compiler)
}

if (providers.gradleProperty("kotshi.createAnnotationsUsingConstructor").forUseAtConfigurationTime().orNull.toBoolean()) {
    tasks.withType<KotlinCompile> {
        kotlinOptions {
            languageVersion = "1.6"
        }
    }
    kapt {
        arguments {
            arg("kotshi.createAnnotationsUsingConstructor", true)
        }
    }
}