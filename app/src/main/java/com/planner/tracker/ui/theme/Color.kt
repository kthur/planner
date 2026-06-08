package com.planner.tracker.ui.theme

import androidx.compose.ui.graphics.Color

val DarkNavy = Color(0xFF1A1A2E)
val Navy = Color(0xFF16213E)
val DarkBlue = Color(0xFF0F3460)
val Accent = Color(0xFFE94560)
val CardBackground = Color(0xFF1E2A4A)
val SurfaceColor = Color(0xFF16213E)
val TextPrimary = Color(0xFFEEEEEE)
val TextSecondary = Color(0xFFAAAAAA)

val HealthColor = Color(0xFF4CAF50)
val MindColor = Color(0xFF2196F3)
val FamilyColor = Color(0xFFFF9800)
val LanguageColor = Color(0xFF9C27B0)
val FinanceColor = Color(0xFFF44336)
val TechColor = Color(0xFF00BCD4)

fun categoryColor(category: com.planner.tracker.data.Category): Color = when (category) {
    com.planner.tracker.data.Category.HEALTH -> HealthColor
    com.planner.tracker.data.Category.MIND -> MindColor
    com.planner.tracker.data.Category.FAMILY -> FamilyColor
    com.planner.tracker.data.Category.LANGUAGE -> LanguageColor
    com.planner.tracker.data.Category.FINANCE -> FinanceColor
    com.planner.tracker.data.Category.TECHNOLOGY -> TechColor
}
