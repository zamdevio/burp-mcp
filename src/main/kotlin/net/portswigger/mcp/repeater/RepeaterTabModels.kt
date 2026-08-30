package net.portswigger.mcp.repeater

import kotlinx.serialization.Serializable

@Serializable
data class RepeaterTabInfo(
    val id: String,
    val name: String?,
    val index: Int,
    val selected: Boolean,
    val hasRequest: Boolean,
    val hasResponse: Boolean,
)

@Serializable
data class RepeaterTabDetail(
    val id: String,
    val name: String?,
    val index: Int,
    val selected: Boolean,
    val request: String?,
    val response: String?,
    val requestAvailable: Boolean,
    val responseAvailable: Boolean,
)
