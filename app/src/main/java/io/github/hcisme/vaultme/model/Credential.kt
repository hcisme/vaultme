package io.github.hcisme.vaultme.model

import androidx.compose.ui.graphics.Color

data class Credential(
    val platform: String,
    val username: String,
    val iconColor: Color = Color.Gray
)
