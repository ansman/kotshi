package se.ansman.kotshi

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@JsonSerializable
data class ClassWithDefaultValues(
        @UseJsonDefaultValue
        val v1: WithCompanionFunction,
        @UseJsonDefaultValue
        val v2: WithStaticFunction,
        @UseJsonDefaultValue
        val v3: WithCompanionProperty,
        @UseJsonDefaultValue
        val v4: WithStaticProperty,
        @UseJsonDefaultValue
        val v5: GenericClassWithDefault<String>,
        @UseJsonDefaultValue
        val v6: GenericClassWithDefault<Int>,
        @UseJsonDefaultValue
        val v7: LocalDate,
        @UseJsonDefaultValue
        val v8: LocalTime,
        @UseJsonDefaultValue
        val v9: LocalDateTime,
        val v10: WithCompanionFunction, // No annotations, should not get a default value
        @DefaultQualifier
        val v11: WithCompanionFunction,
        @UseJsonDefaultValue
        val v12: ClassWithConstructorAsDefault,
        @UseJsonDefaultValue
        val v13: GenericClassWithConstructorAsDefault<String>,
        @UseJsonDefaultValue
        val v14: Int
)

@JsonSerializable
data class WithCompanionFunction(val v: String?) {
    companion object {
        @JsonDefaultValueProvider
        fun provideDefault(): WithCompanionFunction = WithCompanionFunction("WithCompanionFunction")

        @JsonDefaultValueProvider
        @DefaultQualifier
        fun provideQualifiedDefault() = WithCompanionFunction("DefaultQualifier")
    }
}

@JsonSerializable
data class WithStaticFunction(val v: String?) {
    companion object {
        @JsonDefaultValueProvider
        @JvmStatic
        fun provideDefault() = WithStaticFunction("WithStaticFunction")
    }
}

@JsonSerializable
data class WithCompanionProperty(val v: String?) {
    companion object {
        @get:JsonDefaultValueProvider
        val defaultValue = WithCompanionProperty("WithCompanionProperty")
    }
}

@JsonSerializable
data class WithStaticProperty(val v: String?) {
    companion object {
        @JvmField
        @field:JsonDefaultValueProvider
        val defaultValue = WithStaticProperty("WithStaticProperty")
    }
}

@JsonSerializable
data class GenericClassWithDefault<out T>(val v: T?) {
    companion object {
        @JsonDefaultValueProvider
        fun <T> provideDefault() = GenericClassWithDefault<T>(null)

        @JsonDefaultValueProvider
        fun provideIntDefault() = GenericClassWithDefault(4711)
    }
}

object DefaultProvider {
    @JsonDefaultValueProvider
    fun provideDefaultLocalDate(): LocalDate = LocalDate.MIN

    @JsonDefaultValueProvider
    @JvmStatic
    fun provideDefaultLocalTime(): LocalTime = LocalTime.MIN
}

class OtherDefaultProvider private constructor() {
    @JsonDefaultValueProvider
    fun provideDefaultLocalDateTime(): LocalDateTime = LocalDateTime.MIN

    companion object {
        @JvmStatic
        val instance = OtherDefaultProvider()
    }
}

@JsonSerializable
data class ClassWithConstructorAsDefault(val v: String?) {
    @JsonDefaultValueProvider
    constructor() : this("ClassWithConstructorAsDefault")
}

@JsonSerializable
data class GenericClassWithConstructorAsDefault<T : CharSequence>(val v: T?) {
    @JsonDefaultValueProvider
    constructor() : this(null)
}

@JsonDefaultValueProvider
fun provideIntDefault() = 4711

@Target(AnnotationTarget.PROPERTY, AnnotationTarget.FUNCTION, AnnotationTarget.VALUE_PARAMETER)
@MustBeDocumented
@Retention(AnnotationRetention.SOURCE)
@UseJsonDefaultValue
annotation class DefaultQualifier