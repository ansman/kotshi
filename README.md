Kotshi
===
An annotations processor that generates Moshi adapters from Kotlin data classes.

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
    @Json(name = "sign_up_date")
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
        val INSTANCE = KotshiApplicationJsonAdapterFactory()
    }
}
```

Lastly just add the factory to your Moshi instance and you're all set:
```kotlin
val moshi = Moshi.Builder()
    .add(ApplicationJsonAdapterFactory.INSTANCE)
    .build()
```

### Annotations
* `@GetterName` must be used when overriding the default getter name using `@get:JvmName("...")`.
* `@JsonSerializable` is the annotation used to generate `JsonAdapter`'s. Should only be placed on Kotlin data classes.
* `@KotshiConstructor` should be used when there are multiple constructors in the class. Place it on the primary constructor.
* `@KotshiJsonAdapterFactory` makes Kotshi generate a JsonAdapter factory. Should be placed on an abstract class that implements `JsonAdapter.Factory`.

Limitations
---
Currently KAPT does not allow processing Kotlin files directly but rather the generated stubs. This has some downsides
since some Kotlin features are not available in Java.

Another limitation is that custom getter names for the JVM cannot be accessed from the constructor parameter which requires
you to annotate the parameter with `@Getter`. This limitation will be removed when the library starts generating Kotlin code.

Currently default values are not supported in Kotshi but will hopefully be added later through annotations.

Download
---
```groovy
compile 'se.ansman.kotshi:api:0.1.1'
kapt 'se.ansman.kotshi:compiler:0.1.1'
```
Snapshots of the development version are available in [Sonatype's snapshots repository](https://oss.sonatype.org/content/repositories/snapshots/).

License
---
```text
Copyright 2017 Nicklas Ansman Giertz.

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
