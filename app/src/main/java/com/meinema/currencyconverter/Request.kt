package com.meinema.currencyconverter

data class Request(
    val time_last_update_utc: String,
    val rates: Map<String, Double>
)

