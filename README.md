Kotshi ![Build status](https://travis-ci.org/ansman/kotshi.svg?branch=master)
===

An annotations processor that generates [Moshi](https://github.com/square/moshi) adapters from immutable Kotlin data classes.

Moshi's default reflective adapters assume your classes are compiled from Java code which causes problem for Kotlin
data classes.

There is a reflective adapter for Kotlin but that requires the kotlin reflection library which adds a lot of methods and
increase the binary size which in a constrained environment such as Android is something is not preferable.

This is where Kotshi comes in, it generates fast and optimized adapters for your Kotlin data classes, just as if you'd
hand written them yourself. It will automatically regenerate the adapters when you modify your class.

It's made to work with Kotlin data classes with minimal setup, through there are [limitations](#limitations).
Most of the limitations will be addressed when the support for Kotlin annotation processors improves.

Usage
---
First you must annotate your Kotlin data classes with the `@JsonSerializable` annotation:
```kotlin
@JsonSerializable
data class Person(
    val name: String,
    val email: String?,
    // This property uses a custom getter name which requires two annotations.
    @get:JvmName("hasVerifiedAccount") @Getter("hasVerifiedAccount")
    val hasVerifiedAccount: Boolean,
    // This property has a different name in the Json than here so @Json must be applied.
    @Json(name = "created_at")
    val signUpDate: Date,
    // This field has a json qualifier applied, the generated adapter will request an adapter with the qualifier.
    @NullIfEmpty
    val jobTitle: String?
)
```

Then create a class that will be your factory:
```kotlin
@KotshiJsonAdapterFactory
abstract class ApplicationJsonAdapterFactory : JsonAdapter.Factory {
    companion object {
        val INSTANCE: ApplicationJsonAdapterFactory = KotshiApplicationJsonAdapterFactory()
    }
}
```

Lastly just add the factory to your Moshi instance and you're all set:
```kotlin
val moshi = Moshi.Builder()
    .add(ApplicationJsonAdapterFactory.INSTANCE)
    .build()
```

By default adapters aren't requested for primitive types (even boxed primitive
types) since it is worse for performance and most people will not have custom
adapters anyway.
If you need to use custom adapters you can enable it per module be passing the
`useAdaptersForPrimitives` to `@KotshiJsonAdapterFactory` or on a per adapter
by passing the same argument to `@JsonSerializable` (the default is to follow
the module wide setting).

### Annotations
* `@GetterName` must be used when overriding the default getter name using `@get:JvmName("...")`.
* `@JsonSerializable` is the annotation used to generate `JsonAdapter`'s. Should only be placed on Kotlin data classes.
* `@KotshiConstructor` should be used when there are multiple constructors in the class. Place it on the primary constructor.
* `@KotshiJsonAdapterFactory` makes Kotshi generate a JsonAdapter factory. Should be placed on an abstract class that implements `JsonAdapter.Factory`.
* `@JsonDefaultValue` used for enabling default values (see [below](#default-values))
* `@JsonDefaultValueString` used for specifying default values for String properties inline
* `@JsonDefaultValueBoolean` used for specifying default values for Boolean properties inline
* `@JsonDefaultValueByte` used for specifying default values for Byte properties inline
* `@JsonDefaultValueChar` used for specifying default values for Char properties inline
* `@JsonDefaultValueShort` used for specifying default values for Short properties inline
* `@JsonDefaultValueInt` used for specifying default values for Int properties inline
* `@JsonDefaultValueLong` used for specifying default values for Long properties inline
* `@JsonDefaultValueFloat` used for specifying default values for Float properties inline
* `@JsonDefaultValueDouble` used for specifying default values for Double properties inline

### Default values
You can use default values by first annotating a function, field, constructor or enum type with the annotation
`@JsonDefaultValue`. This will be the provider of the default value.

You then annotate a parameter of the same type (or a supertype) with the same annotation.

If you need to have multiple default values of the same type you can create a custom default value annotation by
annotating it with `@JsonDefaultValue`.

If you don't want to define default value providers for primitive and string properties you can use the specialized
default value annotations (`@JsonDefaultValueString`, `@JsonDefaultValueInt` etc).

```kotlin
@Target(AnnotationTarget.VALUE_PARAMETER,
        AnnotationTarget.FUNCTION,
        AnnotationTarget.CONSTRUCTOR,
        AnnotationTarget.FIELD,
        AnnotationTarget.PROPERTY_GETTER)
@MustBeDocumented
@Retention(AnnotationRetention.SOURCE)
@JsonDefaultValue // Makes this annotation a custom default value annotation
annotation class StringWithNA

@JsonSerializable
data class MyClass(
    @JsonDefaultValue
    val name: String,
    @StringWithNA
    val address: String,
    @JsonDefaultValueInt(-1)
    val age: Int
) {
    companion object {
        @JsonDefaultValue
        @JvmField
        val defaultString = ""

        @StringWithNA
        fun defaultStringWithNA() = "N/A"
    }
}
```
The default value provider is allowed to return `null` but only if it's annotated with `@Nullable`.

### Transient Values

Fields marked with `@Transient` are not serialized. When constructing, the adapter supplies the specified
[default value](#default-values) instead.

Limitations
---
Currently KAPT does not allow processing Kotlin files directly but rather the generated stubs. This has some downsides
since some Kotlin features are not available in Java.

Another limitation is that custom getter names for the JVM cannot be accessed from the constructor parameter which requires
you to annotate the parameter with `@Getter`. This limitation will be removed when the library starts generating Kotlin code.

Even though Kotlin nor Moshi prevents having mutable objects Kotshi tries to enforce that for the reason of promoting a good
design as well as avoiding complexity in the generated code. This means that all the properties that you want serialized must
be declared in the primary constructor of the class. This means that `var` properties declared outside the constructor will
not be serialized.

Download
---
```groovy
compile 'se.ansman.kotshi:api:1.0.3' // Use implementation if using Android
kapt 'se.ansman.kotshi:compiler:1.0.3'
```
Snapshots of the development version are available in [Sonatype's snapshots repository](https://oss.sonatype.org/content/repositories/snapshots/).

License
---
```text
Copyright 2017-2018 Nicklas Ansman Giertz.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
