package se.ansman.kotshi

import com.squareup.javapoet.TypeName
import javax.lang.model.element.VariableElement

fun VariableElement.getGetterName(): String {
    // TODO: Don't convert it to a TypeName before comparing
    var typeName = asType().asTypeName()
    if (typeName.isBoxedPrimitive) {
        typeName = typeName.unbox()
    }
    return if (typeName == TypeName.BOOLEAN && simpleName.startsWith("is")) {
        simpleName.toString()
    } else {
        "get${simpleName.toString().capitalize()}"
    }
}
