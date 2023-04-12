import org.jetbrains.dokka.gradle.DokkaTask

plugins {
    id("published-library")
}

dependencies {
    api(libs.moshi.oldestSupported)
}

val packagesMarkdown = buildDir.resolve("generated/docs/packages.md")
val buildPackagesDocs by tasks.registering {
    val readme = rootDir.resolve("README.md")
    inputs.file(readme)
    outputs.file(packagesMarkdown)
    doFirst {
        packagesMarkdown.writer().use { writer ->
            writer.write("# Module kotshi\n\n")
            readme.reader().use { reader ->
                reader.copyTo(writer)
            }
        }
    }
}

tasks.withType<DokkaTask>().configureEach {
    dependsOn(buildPackagesDocs)
    moduleName.set("kotshi")
    dokkaSourceSets {
        configureEach {
            includes.from(packagesMarkdown)
        }
    }
}