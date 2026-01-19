package com.fizzy.android.domain.model

import androidx.compose.ui.graphics.Color

data class Tag(
    val id: Long,
    val name: String,
    val color: String
) {
    val backgroundColor: Color
        get() = try {
            Color(android.graphics.Color.parseColor(color))
        } catch (e: Exception) {
            Color(0xFF6B7280) // Default gray
        }

    val textColor: Color
        get() {
            // Calculate luminance and return white or black
            val bgColor = backgroundColor
            val luminance = 0.299 * bgColor.red + 0.587 * bgColor.green + 0.114 * bgColor.blue
            return if (luminance > 0.5) Color.Black else Color.White
        }
}
