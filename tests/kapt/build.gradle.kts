plugins {
    `test-library`
    kotlin("kapt")
}

dependencies {
    kapt(projects.compiler)
}

val createAnnotationsUsingConstructor = providers.gradleProperty("kotshi.createAnnotationsUsingConstructor")
    .forUseAtConfigurationTime()
    .orNull
    ?.toBoolean()

if (createAnnotationsUsingConstructor != null) {
    kapt {
        arguments {
            arg("kotshi.createAnnotationsUsingConstructor", createAnnotationsUsingConstructor)
        }
    }
}