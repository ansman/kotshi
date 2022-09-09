package se.ansman.kotshi.model

import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName

data class GeneratedAnnotation(
    val annotationClass: ClassName,
    val processorClass: ClassName,
) {
    fun toAnnotationSpec(): AnnotationSpec =
        AnnotationSpec.builder(annotationClass)
            .addMember("%S", processorClass.canonicalName)
            .addMember("comments = %S", "https://github.com/ansman/kotshi")
            .build()
}
