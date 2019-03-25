package se.ansman.kotshi

import com.squareup.moshi.JsonAdapter

@KotshiJsonAdapterFactory
object TestFactory : JsonAdapter.Factory by KotshiTestFactory