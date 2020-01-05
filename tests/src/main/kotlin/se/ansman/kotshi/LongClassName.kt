package se.ansman.kotshi

// Test for https://github.com/ansman/kotshi/issues/123
@JsonSerializable
data class AAAAAAAAAAAAAA(val b: BBBBBBBBBBBBBBB) {
    @JsonSerializable
    data class BBBBBBBBBBBBBBB(val c: CCCCCCCCCCCCCCCCC) {
        @JsonSerializable
        data class CCCCCCCCCCCCCCCCC(val d: DDDDDDDDDDDDDDDDDD) {
            @JsonSerializable
            data class DDDDDDDDDDDDDDDDDD(val e: EEEEEEEEEEEEEEEEEEEEEE) {
                @JsonSerializable
                data class EEEEEEEEEEEEEEEEEEEEEE(
                    val eeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee: Int
                )
            }
        }
    }
}