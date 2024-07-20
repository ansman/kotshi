package se.ansman.kotshi

@OptIn(ExperimentalUnsignedTypes::class)
@JsonSerializable
data class ClassWithQualifierWithEmptyArrays(
    @QualifierWithArrays(
        booleanArrayArg = [],
        byteArrayArg = [],
//        ubyteArrayArg = [],
        charArrayArg = [],
        shortArrayArg = [],
//        ushortArrayArg = [],
        intArrayArg = [],
//        uintArrayArg = [],
        longArrayArg = [],
//        ulongArrayArg = [],
        floatArrayArg = [],
        doubleArrayArg = [],
        stringArrayArg = [],
        enumArrayArg = [],
        annotationArrayArg = [],
        classArrayArg = [],
    )
    val foo: String
)