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
        const val version = "1.10.2"
        const val core = "com.squareup:kotlinpoet:$version"
        const val metadata = "com.squareup:kotlinpoet-metadata:$version"
        const val ksp = "com.squareup:kotlinpoet-ksp:$version"
    }

    const val autocommon = "com.google.auto:auto-common:0.11"
    const val junit = "junit:junit:4.12"
    const val truth = "com.google.truth:truth:0.30"
    const val compiletesting = "com.google.testing.compile:compile-testing:0.10"
    const val moshi = "com.squareup.moshi:moshi:1.8.0"
    const val findBugs = "com.google.code.findbugs:jsr305:3.0.2"

    object incap {
        const val version = "0.2"
        const val api = "net.ltgt.gradle.incap:incap:$version"
        const val compiler = "net.ltgt.gradle.incap:incap-processor:$version"
    }

    object ksp {
        const val version = "1.6.0-1.0.1"
        const val api = "com.google.devtools.ksp:symbol-processing-api:$version"
    }
}