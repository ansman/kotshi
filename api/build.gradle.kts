plugins {
    id("published-library")
}

dependencies {
    api(libs.oldestSupportedMoshi)
}

val packagesMarkdown = layout.buildDirectory.file("generated/docs/packages.md")
val buildPackagesDocs = tasks.register<CopyReadmeTask>("buildPackagesDocs") {
    readme.set(rootProject.file("README.md"))
    outputFile.set(packagesMarkdown)
}

dokka {
    moduleName.set("kotshi")
    dokkaSourceSets {
        configureEach {
            includes.from(buildPackagesDocs.flatMap { it.outputFile })
        }
    }
}

abstract class CopyReadmeTask : DefaultTask() {
    @get:PathSensitive(PathSensitivity.NONE)
    @get:InputFile
    abstract val readme: RegularFileProperty

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @TaskAction
    fun copyReadme() {
        outputFile.get().asFile.writer().use { writer ->
            writer.write("# Module kotshi\n\n")
            readme.get().asFile.reader().use { reader ->
                reader.copyTo(writer)
            }
        }
    }
}
