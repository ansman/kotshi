package se.ansman.kotshi

import com.squareup.javapoet.AnnotationSpec
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.TypeSpec
import javax.lang.model.SourceVersion
import javax.lang.model.util.Elements

fun TypeSpec.Builder.maybeAddGeneratedAnnotation(elements: Elements, sourceVersion: SourceVersion) =
    apply {
        val generatedName = if (sourceVersion > SourceVersion.RELEASE_8) {
            "javax.annotation.processing.Generated"
        } else {
            "javax.annotation.Generated"
        }
        val typeElement = elements.getTypeElement(generatedName)
        if (typeElement != null) {
            addAnnotation(AnnotationSpec.builder(ClassName.get(typeElement))
                .addMember("value", "\$S", "se.ansman.kotshi.KotshiProcessor")
                .build())
        }
    }
