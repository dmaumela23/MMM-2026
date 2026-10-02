package com.maumela.magnummanagement.data.api

import kotlinx.serialization.json.Json

/** The one JSON configuration used by Retrofit and by tests. */
object ApiJson {
    val instance: Json = Json {
        ignoreUnknownKeys = true // new backend fields never break the app
        explicitNulls = false    // null fields are omitted when sending (partial updates)
        encodeDefaults = true    // default values ARE sent (e.g. availability = true)
    }
}