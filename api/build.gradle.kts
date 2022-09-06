import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.incremental.mkdirsOrThrow
import java.net.URL

plugins {
    `published-library`
}

dependencies {
    api(deps.moshi.oldestSupported)
    testImplementation(deps.junit)
    testImplementation(deps.truth)
    testImplementation(deps.kotlin.junit)
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = freeCompilerArgs + listOf(
            "-opt-in=kotlin.RequiresOptIn"
        )
    }
}

tasks.compileTestKotlin {
    kotlinOptions {
        freeCompilerArgs = freeCompilerArgs + listOf(
            "-Xjvm-default=all"
        )
    }
}

val packagesMarkdown = buildDir.resolve("generated/docs/packages.md")
val buildPackagesDocs by tasks.registering(Task::class) {
    val readme = rootDir.resolve("README.md")
    inputs.file(readme)
    outputs.file(packagesMarkdown)
    doFirst {
        packagesMarkdown.parentFile.mkdirsOrThrow()
        packagesMarkdown.writer().use { writer ->
            writer.write("# Module kotshi\n\n")
            readme.reader().use { reader ->
                reader.copyTo(writer)
            }
        }
    }
}

tasks.withType<org.jetbrains.dokka.gradle.DokkaTask> {
    dependsOn(buildPackagesDocs)
    moduleName.set("kotshi")
    dokkaSourceSets {
        configureEach {
            sourceLink {
                localDirectory.set(file("src/main/kotlin"))
                remoteUrl.set(URL("https://github.com/ansman/kotshi/blob/main/api/src/main/kotlin"))
                remoteLineSuffix.set("#L")
            }
            externalDocumentationLink { url.set(URL("https://square.github.io/okio/2.x/okio/")) }
            externalDocumentationLink { url.set(URL("https://square.github.io/moshi/1.x/moshi/")) }
            includes.from(packagesMarkdown)
        }
    }
}