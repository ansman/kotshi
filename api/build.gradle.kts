import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.net.URL

plugins {
    `published-library`
}

dependencies {
    api(deps.moshi.oldestSupported)
    testImplementation(deps.junit)
    testImplementation(deps.truth)
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

tasks.withType<org.jetbrains.dokka.gradle.DokkaTask> {
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
        }
    }
}