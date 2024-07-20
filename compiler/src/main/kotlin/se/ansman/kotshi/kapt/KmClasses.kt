package se.ansman.kotshi.kapt

import kotlin.metadata.KmClass
import kotlin.metadata.kind

val KmClass.isClass get() = kind == kotlin.metadata.ClassKind.CLASS
val KmClass.isObject get() = kind == kotlin.metadata.ClassKind.OBJECT
val KmClass.isEnum get() = kind == kotlin.metadata.ClassKind.ENUM_CLASS