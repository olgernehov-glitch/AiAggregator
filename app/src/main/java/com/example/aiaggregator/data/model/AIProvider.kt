package com.example.aiaggregator.data.model

data class AIProvider(
    val id: String,
    val name: String,
    val baseUrl: String,
    val apiKey: String = "",
    val modelName: String = ""
)
