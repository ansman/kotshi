Change Log
===
Version 1.0.3 (2018-04-17)
---
Changes:
* Add Generated annotation to types. ([#71](https://github.com/ansman/kotshi/pull/71))
* Add support for @Transient modifier ([#79](https://github.com/ansman/kotshi/pull/79))
* Update to Kotlin 1.2.31 ([#82](https://github.com/ansman/kotshi/pull/82))
* Fix a bug regarding getter name matching ([#75](https://github.com/ansman/kotshi/pull/75))

Version 1.0.2 (2018-02-28)
---
This version fixes some problems with wildcards when using `Any` or `Object` as
the upper or lower bound.

Changes:
* Treat wildcards as the upper/lower bound ([#68](https://github.com/ansman/kotshi/pull/68))

Version 1.0.1 (2018-02-20)
---
Due to what seems to be a bug in kapt the 1.0.0 way of implementing the factory
seems to have some issues. This releases also allows, and recommends, the pre
1.0.0 way of implementing the factory until the kapt issue has been resolved.
If the 1.0.0 way has worked for you it can be used in future releases too.

Changes:
* Fix a bug when using generics with wildcards ([#64](https://github.com/ansman/kotshi/pull/64))
* Allow the pre 1.0 way of implementing the factory ([#63](https://github.com/ansman/kotshi/pull/63))

Version 1.0.0 (2018-02-08)
---
Changes:
* Let the generated adapters implement toString ([#52](https://github.com/ansman/kotshi/pull/52))
* Update to Kotlin 1.2.21 ([#55](https://github.com/ansman/kotshi/pull/55))
* Change how you implement the adapter factory ([#56](https://github.com/ansman/kotshi/pull/56))

Version 0.3.0 (2018-01-03)
---
General changes:
* Kotshi now supports default values ([#10](https://github.com/ansman/kotshi/pull/10))
* Update to Kotlin 1.2.10 ([#38](https://github.com/ansman/kotshi/pull/38))
* Strings are now treated as a primitive and an adapter is not used unless specifically requested. ([#32](https://github.com/ansman/kotshi/pull/32))
* Fixed a bug that caused adapters to not be requested for primitives when qualifiers were used. ([#32](https://github.com/ansman/kotshi/pull/32))
* Fix a bug where the generated code would not compile due to name conflicts. ([#21](https://github.com/ansman/kotshi/pull/21))
* Fix a bug that would generate uncompilable code when using a generic type with default values. ([#12](https://github.com/ansman/kotshi/pull/12))
* The generated code is now prettier and faster.

Code changes:
* Read object opening before initializing locals. ([#15](https://github.com/ansman/kotshi/pull/15))
* When writing null, perform an early return. ([#16](https://github.com/ansman/kotshi/pull/16))
* Eliminate superfluous null check on allowed-nullable values. ([#17](https://github.com/ansman/kotshi/pull/17))
* Remove explicit null check for boxed primitives in writer. ([#18](https://github.com/ansman/kotshi/pull/18))
* Wrap the JSON name strings onto one per-line. ([#19](https://github.com/ansman/kotshi/pull/19))
* Generated factory should be package-private. ([#24](https://github.com/ansman/kotshi/pull/24))
* Skip emitting constructor when no adapters are needed. ([#36](https://github.com/ansman/kotshi/pull/36))
* Emit long literals with a "L" type suffix. ([#45](https://github.com/ansman/kotshi/pull/45))
* Add some aesthetic newline spaces in the generated file. ([#44](https://github.com/ansman/kotshi/pull/44))
* Implement support for supplying default values for properties ([#10](https://github.com/ansman/kotshi/pull/10))
* Update to Kotlin 1.2.0 ([#11](https://github.com/ansman/kotshi/pull/11))
* Fix a bug that caused uncompilable code to be generated ([#12](https://github.com/ansman/kotshi/pull/12))
* Use NameAllocator for adapter fields, locals ([#21](https://github.com/ansman/kotshi/pull/21))
* Treat strings as primitive values and skip adapters per default ([#32](https://github.com/ansman/kotshi/pull/32))
* Handle properties names as java keywords such as default better ([#37](https://github.com/ansman/kotshi/pull/37))
* Update to Kotlin 1.2.10 ([#38](https://github.com/ansman/kotshi/pull/38))
* Fix an issue with generics being used as generic type arguments ([#42](https://github.com/ansman/kotshi/pull/42))
* Skip generating a helper boolean if it's not needed ([#43](https://github.com/ansman/kotshi/pull/43))

Version 0.3.0-beta3 (2017-12-22)
---
Changes since beta2:
* Skip emitting constructor when no adapters are needed. ([#36](https://github.com/ansman/kotshi/pull/36))
* Handle properties names as java keywords such as default better ([#37](https://github.com/ansman/kotshi/pull/37))
* Update to Kotlin 1.2.10 ([#38](https://github.com/ansman/kotshi/pull/38))
* Fix an issue with generics being used as generic type arguments ([#42](https://github.com/ansman/kotshi/pull/42))

Version 0.3.0-beta2 (2017-12-18)
---
General changes (since beta1):
* Strings are now treated as a primitive and an adapter is not used unless specifically requested. ([#32](https://github.com/ansman/kotshi/pull/32))
* Fixed a bug that caused adapters to not be requested for primitives when qualifiers were used. ([#32](https://github.com/ansman/kotshi/pull/32))
* Fix a bug where the generated code would not compile due to name conflicts. ([#21](https://github.com/ansman/kotshi/pull/21))
* Fix a bug that would generate uncompilable code when using a generic type with default values. ([#12](https://github.com/ansman/kotshi/pull/12))
* The generated code is now prettier and faster.

Code changes (since beta1):
* Read object opening before initializing locals. ([#15](https://github.com/ansman/kotshi/pull/15))
* When writing null, perform an an early return. ([#16](https://github.com/ansman/kotshi/pull/16))
* Eliminate superfluous null check on allowed-nullable values. ([#17](https://github.com/ansman/kotshi/pull/17))
* Remove explicit null check for boxed primitives in writer. ([#18](https://github.com/ansman/kotshi/pull/18))
* Wrap the JSON name strings onto one per-line. ([#19](https://github.com/ansman/kotshi/pull/19))
* Add IDE imls and Gradle's reports dir to ignores. ([#29](https://github.com/ansman/kotshi/pull/29))
* Generated factory should be package-private. ([#24](https://github.com/ansman/kotshi/pull/24))
* Do not pass Moshi instance to adapter when not needed. ([#28](https://github.com/ansman/kotshi/pull/28))
* Do a direct continue inside switch cases. ([#27](https://github.com/ansman/kotshi/pull/27))
* Remove redundant apply. ([#34](https://github.com/ansman/kotshi/pull/34))
* Add a test for qualified primitive always calling an adapter. ([#35](https://github.com/ansman/kotshi/pull/35))
* Fix missing @JsonDefaultValue on custom annotation ([#31](https://github.com/ansman/kotshi/pull/31))
* Fix inconsistent indentation ([#33](https://github.com/ansman/kotshi/pull/33))
* Fix a bug that caused uncompilable code to be generated ([#12](https://github.com/ansman/kotshi/pull/12))
* Use NameAllocator for adapter fields, locals ([#21](https://github.com/ansman/kotshi/pull/21))
* Treat strings as primitive values and skip adapters per default ([#32](https://github.com/ansman/kotshi/pull/32))

Version 0.3.0-beta1 (2017-11-29)
---
* Implement support for supplying default values for properties ([#10](https://github.com/ansman/kotshi/pull/10))
* Update to Kotlin 1.2.0 ([#11](https://github.com/ansman/kotshi/pull/11))

Version 0.2.0 (2017-11-21)
---
* Kotlin 1.1.60 is now used.
* The generated adapters have been optimized so that if multiple fields use the same type the same adapter is used. ([#7](https://github.com/ansman/kotshi/pull/7)).
* Adapters are no longer used for primitive types (int, float etc) since it can be bad for performance. This behavior can be disabled using a new flag. ([#9](https://github.com/ansman/kotshi/pull/9)).

Version 0.1.1 (2017-06-29)
---
* Fix a bug that caused adapter generation for inner classes to fail.

Version 0.1.0 (2017-06-29)
---
Initial release.
