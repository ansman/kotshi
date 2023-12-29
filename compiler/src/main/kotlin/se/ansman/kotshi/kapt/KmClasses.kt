package se.ansman.kotshi.kapt

import kotlinx.metadata.KmClass
import kotlinx.metadata.kind

val KmClass.isClass get() = kind == kotlinx.metadata.ClassKind.CLASS
val KmClass.isObject get() = kind == kotlinx.metadata.ClassKind.OBJECT
val KmClass.isEnum get() = kind == kotlinx.metadata.ClassKind.ENUM_CLASS