package se.ansman.kotshi.kapt

import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.tag
import com.squareup.moshi.JsonQualifier
import se.ansman.kotshi.Types
import se.ansman.kotshi.kapt.AnnotationModelValueVisitor.Companion.toAnnotationModel
import se.ansman.kotshi.model.AnnotationModel
import javax.lang.model.element.AnnotationMirror


fun List<AnnotationSpec>.find(typeName: ClassName): AnnotationSpec? = find { it.typeName == typeName }
fun List<AnnotationSpec>.has(typeName: ClassName): Boolean = find(typeName) != null

fun List<AnnotationSpec>.jsonName(): String? = find(Types.Moshi.json)?.let { spec ->
    requireNotNull<AnnotationMirror>(spec.tag()).getValueOrNull("name")
}

@OptIn(ExperimentalUnsignedTypes::class)
fun List<AnnotationSpec>.qualifiers(): Set<AnnotationModel> =
    mapNotNullTo(mutableSetOf()) { spec ->
        val mirror = requireNotNull(spec.tag<AnnotationMirror>())
        if (mirror.annotationType.asElement().getAnnotation(JsonQualifier::class.java) == null) {
            return@mapNotNullTo null
        }
        mirror.toAnnotationModel()
    }