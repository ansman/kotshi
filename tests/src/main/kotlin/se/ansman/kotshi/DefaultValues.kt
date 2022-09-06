package se.ansman.kotshi

@JsonSerializable
data class ClassWithDefaultValues(
    val v1: Byte = Byte.MAX_VALUE,
    val v2: Char = Char.MAX_VALUE,
    val v3: Short = Short.MAX_VALUE,
    val v4: Int = Int.MAX_VALUE,
    val v5: Long = Long.MAX_VALUE,
    val v6: Float = Float.MAX_VALUE,
    val v7: Double = Double.MAX_VALUE,
    val v8: String = "n/a",
    val v9: List<String> = emptyList(),
    val v10: String
)

@JsonSerializable
data class WithCompanionFunction(val v: String?)

@JsonSerializable
data class WithStaticFunction(val v: String?)

@JsonSerializable
data class WithCompanionProperty(val v: String?)

@JsonSerializable
data class WithStaticProperty(val v: String?)

@JsonSerializable
data class GenericClassWithDefault<out T>(val v: T?)

@JsonSerializable
data class ClassWithConstructorAsDefault(val v: String?) {
    constructor() : this("ClassWithConstructorAsDefault")
}

@JsonSerializable
data class GenericClassWithConstructorAsDefault<T : CharSequence>(val v: T?) {
    constructor() : this(null)
}

