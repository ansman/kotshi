package se.ansman.kotshi

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@JsonSerializable
data class ClassWithDefaultValues(
        @JsonDefaultValue
        val v1: WithCompanionFunction,
        @JsonDefaultValue
        val v2: WithStaticFunction,
        @JsonDefaultValue
        val v3: WithCompanionProperty,
        @JsonDefaultValue
        val v4: WithStaticProperty,
        @JsonDefaultValue
        val v5: GenericClassWithDefault<String>,
        @JsonDefaultValue
        val v6: GenericClassWithDefault<Int>,
        @JsonDefaultValue
        val v7: LocalDate,
        @JsonDefaultValue
        val v8: LocalTime,
        @JsonDefaultValue
        val v9: LocalDateTime,
        val v10: WithCompanionFunction, // No annotations, should not get a default value
        @OtherJsonDefaultValue
        val v11: WithCompanionFunction,
        @JsonDefaultValue
        val v12: ClassWithConstructorAsDefault,
        @JsonDefaultValue
        val v13: GenericClassWithConstructorAsDefault<String>,
        @JsonDefaultValue
        val v14: Int,
        @JsonDefaultValue
        val v15: SomeEnum
)

@JsonSerializable
data class WithCompanionFunction(val v: String?) {
    companion object {
        @JsonDefaultValue
        fun provideDefault(): WithCompanionFunction = WithCompanionFunction("WithCompanionFunction")

        @OtherJsonDefaultValue
        fun provideQualifiedDefault() = WithCompanionFunction("OtherJsonDefaultValue")
    }
}

@JsonSerializable
data class WithStaticFunction(val v: String?) {
    companion object {
        @JsonDefaultValue
        @JvmStatic
        fun provideDefault() = WithStaticFunction("WithStaticFunction")
    }
}

@JsonSerializable
data class WithCompanionProperty(val v: String?) {
    companion object {
        @get:JsonDefaultValue
        val defaultValue = WithCompanionProperty("WithCompanionProperty")
    }
}

@JsonSerializable
data class WithStaticProperty(val v: String?) {
    companion object {
        @JvmField
        @field:JsonDefaultValue
        val defaultValue = WithStaticProperty("WithStaticProperty")
    }
}

@JsonSerializable
data class GenericClassWithDefault<out T>(val v: T?) {
    companion object {
        @JsonDefaultValue
        fun <T> provideDefault() = GenericClassWithDefault<T>(null)

        @JsonDefaultValue
        fun provideIntDefault() = GenericClassWithDefault(4711)
    }
}

object DefaultProvider {
    @JsonDefaultValue
    fun provideDefaultLocalDate(): LocalDate = LocalDate.MIN

    @JsonDefaultValue
    @JvmStatic
    fun provideDefaultLocalTime(): LocalTime = LocalTime.MIN
}

class OtherDefaultProvider private constructor() {
    @JsonDefaultValue
    fun provideDefaultLocalDateTime(): LocalDateTime = LocalDateTime.MIN

    companion object {
        @JvmStatic
        val instance = OtherDefaultProvider()
    }
}

@JsonSerializable
data class ClassWithConstructorAsDefault(val v: String?) {
    @JsonDefaultValue
    constructor() : this("ClassWithConstructorAsDefault")
}

@JsonSerializable
data class GenericClassWithConstructorAsDefault<T : CharSequence>(val v: T?) {
    @JsonDefaultValue
    constructor() : this(null)
}

@JsonDefaultValue
fun provideIntDefault() = 4711

@Target(AnnotationTarget.PROPERTY, AnnotationTarget.FUNCTION, AnnotationTarget.VALUE_PARAMETER)
@MustBeDocumented
@Retention(AnnotationRetention.SOURCE)
@JsonDefaultValue
annotation class OtherJsonDefaultValue