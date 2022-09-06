package se.ansman.kotshi

import com.squareup.kotlinpoet.ClassName

data class ProguardConfig(
    val targetClass: ClassName,
    val targetConstructorParams: List<String>,
) {
    fun outputFilePathWithoutExtension(canonicalName: String): String = "META-INF/proguard/kotshi-$canonicalName"

    fun writeTo(out: Appendable): Unit = out.run {
        val targetName = targetClass.reflectionName()
        // If the target class has default parameter values, keep its synthetic constructor
        //
        // -keepnames class kotlin.jvm.internal.DefaultConstructorMarker
        // -keepclassmembers @com.squareup.moshi.JsonClass @kotlin.Metadata class * {
        //     synthetic <init>(...);
        // }
        //
        appendLine("-if class $targetName")
        appendLine("-keepnames class kotlin.jvm.internal.DefaultConstructorMarker")
        appendLine("-if class $targetName")
        appendLine("-keepclassmembers class $targetName {")
        val allParams = targetConstructorParams.toMutableList()
        val maskCount = if (targetConstructorParams.isEmpty()) {
            0
        } else {
            (targetConstructorParams.size + 31) / 32
        }
        repeat(maskCount) {
            allParams += "int"
        }
        allParams += "kotlin.jvm.internal.DefaultConstructorMarker"
        val params = allParams.joinToString(",")
        appendLine("    public synthetic <init>($params);")
        appendLine("}")
    }
}
