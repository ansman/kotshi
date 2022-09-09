package se.ansman.kotshi

object Options {
    const val createAnnotationsUsingConstructor = "kotshi.createAnnotationsUsingConstructor"
    const val useLegacyDataClassRenderer = "kotshi.useLegacyDataClassRenderer"
    const val generatedAnnotation = "kotshi.generatedAnnotation"

    val possibleGeneratedAnnotations = setOf(Types.Java.generatedJDK9, Types.Java.generatedPreJDK9)
        .associateBy { it.canonicalName }
}