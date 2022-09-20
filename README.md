# Kotshi [![Build Gradle](https://github.com/ansman/kotshi/actions/workflows/gradle.yml/badge.svg)](https://github.com/ansman/kotshi/actions/workflows/gradle.yml) ![Maven Central](https://img.shields.io/maven-central/v/se.ansman.kotshi/api)
An annotation processor that generates [Moshi](https://github.com/square/moshi) adapters from Kotlin classes.

There is a reflective adapter for Kotlin but that requires the kotlin reflection library which adds a lot of methods and
increase the binary size which in a constrained environment such as Android is something is not preferable.

This is where Kotshi comes in, it generates fast and optimized adapters for your Kotlin data classes, just as if you'd
written them by hand yourself. It will automatically regenerate the adapters when you modify your class.

It's made to work with minimal setup, through there are [limitations](#limitations).
Most of the limitations will be addressed as the support for Kotlin annotation processors improves.

You can find the generated documentation by visiting [kotshi.ansman.se](https://kotshi.ansman.se/).

## Usage
First you must annotate your objects with the `@JsonSerializable` annotation:
```kotlin
@JsonSerializable
data class Person(
    val name: String,
    val email: String?,
    val hasVerifiedAccount: Boolean,
    // This property has a different name in the Json than here so @JsonProperty must be applied.
    @JsonProperty(name = "created_at")
    val signUpDate: Date,
    // This field has a default value which will be used if the field is missing.
    val jobTitle: String? = null
)
```

The following types are supported:
* `object` (serialized as an empty JSON object)
* `data class`
* `enum class`
* `sealed class`

Then create a class that will be your factory:
```kotlin
@KotshiJsonAdapterFactory
object ApplicationJsonAdapterFactory : JsonAdapter.Factory by KotshiApplicationJsonAdapterFactory
```

Lastly just add the factory to your Moshi instance, and you're all set:
```kotlin
val moshi = Moshi.Builder()
    .add(ApplicationJsonAdapterFactory)
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
* `@JsonSerializable` is the annotation used to generate `JsonAdapter`'s. Should only be placed on data classes, enums, sealed classes and objects.
* `@KotshiJsonAdapterFactory` makes Kotshi generate a JsonAdapter factory. Should be placed on an abstract class that implements `JsonAdapter.Factory`.
* `@JsonDefaultValue` can be used to annotate a fallback for enums or sealed classes when an unknown entry is encountered. The default is to thrown an exception.
* `@JsonProperty` can be used to customize how a property or enum entry is serialized to and from JSON.
* `@Polymorphic` and `@PolymorphicLabel` used on sealed classes and their implementions.
* `@RegisterJsonAdapter` registers a json adapter into the Kotshi json adapter factory.

### Default Values
You can use default values just like you normally would in Kotlin.

Due to limitations in Kotlin two instances of the object will be created when a class uses default values
([youtrack issue](https://youtrack.jetbrains.com/issue/KT-18695)). This also means that composite default values are not
supported (for example a `fullName` property that is `"$firstName $lastName"`).

For enum entries and sealed classes you may annotate a single type with `@JsonDefaultValue` to indicate that the entry
should be used when an unknown value is encountered (by default an exception is thrown).

### Transient Values
Properties marked with `@Transient` are not serialized. All transient properties must have a default value.

Only properties declared in the constructor needs to be annotated since other properties are ignores.

### Custom Names
By default, the property or enum entry name is used when reading and writing JSON. To change the name used you may use 
the `@JsonProperty` annotation or the regular `@Json` annotation from Moshi to annotate the property or enum entry.

### Json Qualifiers
Kotshi has full support for `@JsonQualifier`, both plain and those with arguments. Simply annotate a property with the
desired qualifiers and Kotshi will pick them up.

### Registered adapters
It's often required to have a few adapters that are handwritten, for example for framework classes. Handling this in a 
custom factory can be tedious, especially for generic types. To make this easier you may annotate any class or object
that extends `JsonAdapter` with `@RegisterJsonAdapter` and Kotshi will generate the needed code in the adapter factory.

### Options

#### `kotshi.createAnnotationsUsingConstructor`
This option enables a new way of creating annotations instances at runtime. Normally Kotshi uses reflection to create
the qualifier annotations but as of 1.5.30 of Kotlin you can enable creating annotations by calling the constructor.

This behavior is enabled by default when using language version 1.6 but can be explicitly enabled or disabled using this
option. You can enable it like this:
```kotlin
// If you are using Groovy build scripts this will be `tasks.withType(KotlinCompile) {`
// This is only needed when using Kotlin 1.5
tasks.withType<KotlinCompile> {
    kotlinOptions {
        languageVersion = "1.6"
    }
}

// When using KAPT
kapt {
    arguments {
        arg("kotshi.createAnnotationsUsingConstructor", false)
    }
}

// When using KSP
ksp {
    arg("kotshi.createAnnotationsUsingConstructor", "false")
}
```

See more about instantiating annotations here: https://kotlinlang.org/docs/whatsnew1530.html#instantiation-of-annotation-classes

## Limitations
* Kotshi only processes files written in Kotlin, types written in Java are not supported.
* Only data classes, enums, sealed classes and objects are supported.
  - Only constructor properties will be serialized.
  - Qualifiers whose arguments are named as a Java keyword cannot be seen by annotations processors and cannot be used.
* Due to limitation in KAPT, properties with a `java` keyword as a name cannot be marked as transient.
* Default values that depend on other constructor properties is not supported ([youtrack issue](https://youtrack.jetbrains.com/issue/KT-18695)).
* Due to a KAPT bug/limitation you cannot add qualifiers to parameters that are inline classes ([youtrack issue](https://youtrack.jetbrains.com/issue/KT-36352)).

## Download

```kotlin
plugins {
    ...
    kotlin("kapt")
    // If you are using KSP then use this
    id("com.google.devtools.ksp") version "<version>"
}

dependencies {
    val kotshiVersion = "2.9.1"
    implementation("se.ansman.kotshi:api:$kotshiVersion")
    kapt("se.ansman.kotshi:compiler:$kotshiVersion")
    // If you are using KSP then you use instead
    ksp("se.ansman.kotshi:compiler:$kotshiVersion")
}
```
Snapshots of the development version are available in [the sonatype snapshots repository](https://oss.sonatype.org/#view-repositories;snapshots~browsestorage~se/ansman/kotshi/).

## License
```text
Copyright 2017-2022 Nicklas Ansman Giertz.

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
