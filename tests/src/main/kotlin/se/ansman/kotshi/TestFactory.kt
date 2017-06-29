package se.ansman.kotshi

import com.squareup.moshi.JsonAdapter

@KotshiJsonAdapterFactory
abstract class TestFactory : JsonAdapter.Factory {
    companion object {
        val INSTANCE: TestFactory = KotshiTestFactory()
    }
}