package se.ansman.kotshi

import com.squareup.moshi.JsonAdapter

@OptIn(ExperimentalStdlibApi::class)
@KotshiJsonAdapterFactory
object TestFactory : JsonAdapter.Factory by KotshiTestFactory