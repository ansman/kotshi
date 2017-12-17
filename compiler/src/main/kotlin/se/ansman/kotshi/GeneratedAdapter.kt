package se.ansman.kotshi

import com.squareup.javapoet.ClassName

data class GeneratedAdapter(
    val className: ClassName,
    val requiresMoshi: Boolean = true,
    val requiresTypes: Boolean = false
) {
    init {
        assert(!requiresTypes || requiresMoshi) {
            "An adapter requiring types must also require a Moshi instance."
        }
    }
}
