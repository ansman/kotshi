@Suppress("ClassName", "MemberVisibilityCanBePrivate")
object deps {
    object kotlin {
        const val junit = "org.jetbrains.kotlin:kotlin-test-junit"
    }

    object autoService {
        const val version = "1.0"
        const val compiler = "com.google.auto.service:auto-service:$version"
        const val api = "com.google.auto.service:auto-service-annotations:$version"
    }

    object kotlinpoet {
        const val version = "1.9.0"
        const val core = "com.squareup:kotlinpoet:$version"
        const val metadata = "com.squareup:kotlinpoet-metadata:$version"
        const val metadataSpecs = "com.squareup:kotlinpoet-metadata-specs:$version"
        const val classinspector = "com.squareup:kotlinpoet-classinspector-elements:$version"
    }

    const val autocommon = "com.google.auto:auto-common:1.1"
    const val junit = "junit:junit:4.13.2"
    const val truth = "com.google.truth:truth:1.1.3"
    const val compiletesting = "com.google.testing.compile:compile-testing:0.19"
    const val moshi = "com.squareup.moshi:moshi:1.12.0"
    const val findBugs = "com.google.code.findbugs:jsr305:3.0.2"

    object incap {
        const val version = "0.3"
        const val api = "net.ltgt.gradle.incap:incap:$version"
        const val compiler = "net.ltgt.gradle.incap:incap-processor:$version"
    }

    object ksp {
        const val version = "1.5.10-1.0.0-beta01"
        const val api = "com.google.devtools.ksp:symbol-processing-api:$version"
    }
}