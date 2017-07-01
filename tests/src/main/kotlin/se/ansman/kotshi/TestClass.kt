package se.ansman.kotshi

import com.squareup.moshi.Json

abstract class SuperClass {
    val someSuperProperty: String = "Hello"
    abstract val abstractProperty: String
}

@JsonSerializable
data class TestClass(
        val string: String,
        val nullableString: String?,
        @JvmField
        val integer: Int,
        val nullableInt: Int?,
        val isBoolean: Boolean,
        val isNullableBoolean: Boolean?,
        val list: List<String>,
        val nestedList: List<Map<String, Set<String>>>,
        override val abstractProperty: String,
        @Json(name = "other_name")
        val customName: String,
        @Hello
        val annotated: String,
        val genericClass: GenericClass<String, List<String>>
) : SuperClass() {

    val constantProperty: String = "Hello"

    val virtualProperty: String
        get() = "Fake"

    val delegatedProperty: String by lazy { "Lazy" }
}