Change Log
===

Version 0.2.0 (2017-11-21)
---
* Kotlin 1.1.60 is now used.
* The generated adapters have been optimized so that if multiple fields use the same type the same adapter is used. (#7).
* Adapters are no longer used for primitive types (int, float etc) since it can be bad for performance. This behavior can be disabled using a new flag. (#9).

Version 0.1.1 (2017-06-29)
---
* Fix a bug that caused adapter generation for inner classes to fail.

Version 0.1.0 (2017-06-29)
---
Initial release.
