package local.pianopracticeplanner.ui

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.unit.sp

internal object PianoPalette {
    val Blue=Color(0xFF3568A8)
    val Ink=Color(0xFF20344E)
    val Muted=Color(0xFF566C86)
    val Soft=Color(0xFFE8F0FA)
    val Line=Color(0xFFD4E0EF)
    val Background=Color(0xFFF4F7FC)
    val Heat=listOf(Color(0xFFEDF1F7),Color(0xFFE0EBF9),Color(0xFFBDD1EE),Color(0xFF89ADDA),Blue)
}

private val colors=lightColorScheme(
    primary=PianoPalette.Blue,onPrimary=Color.White,primaryContainer=PianoPalette.Soft,onPrimaryContainer=PianoPalette.Ink,
    secondary=PianoPalette.Blue,onSecondary=Color.White,secondaryContainer=PianoPalette.Soft,onSecondaryContainer=PianoPalette.Ink,
    tertiary=PianoPalette.Blue,onTertiary=Color.White,tertiaryContainer=PianoPalette.Soft,onTertiaryContainer=PianoPalette.Ink,
    background=PianoPalette.Background,onBackground=PianoPalette.Ink,surface=Color.White,onSurface=PianoPalette.Ink,
    surfaceVariant=PianoPalette.Soft,onSurfaceVariant=PianoPalette.Muted,outline=PianoPalette.Muted,outlineVariant=PianoPalette.Line,
    surfaceTint=PianoPalette.Blue,surfaceDim=PianoPalette.Soft,surfaceBright=Color.White,
    surfaceContainerLowest=Color.White,surfaceContainerLow=PianoPalette.Background,surfaceContainer=PianoPalette.Background,
    surfaceContainerHigh=PianoPalette.Soft,surfaceContainerHighest=PianoPalette.Soft,
    error=PianoPalette.Ink,onError=Color.White,errorContainer=PianoPalette.Soft,onErrorContainer=PianoPalette.Ink,
    inverseSurface=PianoPalette.Ink,inverseOnSurface=Color.White,inversePrimary=PianoPalette.Heat[2]
)
private fun TextStyle.localized()=copy(fontFamily=FontFamily.SansSerif,localeList=LocaleList(local.pianopracticeplanner.i18n.I18n.language))
private fun localizedTypography()=Typography().let{t->t.copy(
    displayLarge=t.displayLarge.localized(),displayMedium=t.displayMedium.localized(),displaySmall=t.displaySmall.localized(),
    headlineLarge=t.headlineLarge.localized(),headlineMedium=t.headlineMedium.localized(),headlineSmall=t.headlineSmall.localized(),
    titleLarge=t.titleLarge.localized().copy(fontSize=24.sp,lineHeight=30.sp),titleMedium=t.titleMedium.localized().copy(fontSize=20.sp,lineHeight=26.sp),titleSmall=t.titleSmall.localized().copy(fontSize=18.sp,lineHeight=24.sp),
    bodyLarge=t.bodyLarge.localized().copy(fontSize=18.sp,lineHeight=26.sp),bodyMedium=t.bodyMedium.localized().copy(fontSize=18.sp,lineHeight=26.sp),bodySmall=t.bodySmall.localized().copy(fontSize=14.sp,lineHeight=20.sp),
    labelLarge=t.labelLarge.localized().copy(fontSize=16.sp,lineHeight=22.sp),labelMedium=t.labelMedium.localized().copy(fontSize=15.sp,lineHeight=20.sp),labelSmall=t.labelSmall.localized().copy(fontSize=14.sp,lineHeight=18.sp)
)}
@Composable internal fun PianoTheme(content:@Composable ()->Unit){MaterialTheme(colorScheme=colors,typography=localizedTypography(),content=content)}
