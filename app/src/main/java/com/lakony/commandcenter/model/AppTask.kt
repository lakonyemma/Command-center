package com.lakony.commandcenter.model

data class AppTask(
    val id: Long = System.currentTimeMillis(),
    val title: String,
    val category: String = "General",
    val completed: Boolean = false
)
