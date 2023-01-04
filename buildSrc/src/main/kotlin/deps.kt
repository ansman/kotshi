@Suppress("ClassName", "MemberVisibilityCanBePrivate")
object deps {
    object kotlin {
        const val junit = "org.jetbrains.kotlin:kotlin-test-junit"
    }

    object autoService {
        const val version = "1.0.1"
        const val compiler = "com.google.auto.service:auto-service:$version"
        const val api = "com.google.auto.service:auto-service-annotations:$version"
    }

    object kotlinpoet {
        const val version = "1.12.0"
        const val core = "com.squareup:kotlinpoet:$version"
        const val metadata = "com.squareup:kotlinpoet-metadata:$version"
        const val ksp = "com.squareup:kotlinpoet-ksp:$version"
    }

    const val kotlinxMetadata = "org.jetbrains.kotlinx:kotlinx-metadata-jvm:0.5.0"

    const val autocommon = "com.google.auto:auto-common:1.2.1"
    const val junit = "junit:junit:4.12"
    const val truth = "com.google.truth:truth:1.1.3"
    object compileTesting {
        const val version = "1.4.9"
        const val core = "com.github.tschuchortdev:kotlin-compile-testing:$version"
        const val ksp = "com.github.tschuchortdev:kotlin-compile-testing-ksp:$version"
    }
    object moshi {
        const val oldestSupported = "com.squareup.moshi:moshi:1.8.0"
        const val current = "com.squareup.moshi:moshi:1.14.0"
    }
    const val findBugs = "com.google.code.findbugs:jsr305:3.0.2"

    object incap {
        const val version = "0.3"
        const val api = "net.ltgt.gradle.incap:incap:$version"
        const val compiler = "net.ltgt.gradle.incap:incap-processor:$version"
    }

    object ksp {
        const val version = "1.8.0-1.0.8"
        const val api = "com.google.devtools.ksp:symbol-processing-api:$version"
    }

    const val asm = "org.ow2.asm:asm:9.3"
}