package com.aks_labs.tulsi.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.aks_labs.tulsi.R

// Custom font families
val PacificoFont = FontFamily(
    Font(R.font.pacifico_regular)
)

val MontserratFont = FontFamily(
    Font(R.font.montserrat_regular, FontWeight.Normal)
)

// System font families
val SansSerifCondensedFont = FontFamily.SansSerifCondensed
val SerifFont = FontFamily.Serif
val CursiveFont = FontFamily.Cursive
val MonospaceFont = FontFamily.Monospace

// App title font families
val TulsiTitleFont = PacificoFont
val PhotosTitleFont = SansSerifCondensedFont // Changed from MontserratFont to SansSerifCondensedFont

// Set of Material typography styles to start with
val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    titleLarge = TextStyle(
        fontFamily = MontserratFont,
        fontWeight = FontWeight.Normal,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    labelSmall = TextStyle(
        fontFamily = MontserratFont,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)

