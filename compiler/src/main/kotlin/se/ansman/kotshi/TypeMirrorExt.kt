package se.ansman.kotshi

import com.squareup.javapoet.TypeName
import javax.lang.model.type.TypeMirror

fun TypeMirror.asTypeName(): TypeName = TypeName.get(this)