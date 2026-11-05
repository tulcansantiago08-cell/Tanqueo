package com.tanqueo.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.Text
import com.tanqueo.ui.theme.TanqueoColors

val GlassCardShape = RoundedCornerShape(26.dp)

/** Vidrio oscuro: gris translúcido con un borde fino de 1 dp más claro. */
fun Modifier.glass(shape: Shape = GlassCardShape): Modifier = this
    .clip(shape)
    .background(Brush.verticalGradient(listOf(TanqueoColors.GlassTop, TanqueoColors.GlassBottom)), shape)
    .border(1.dp, TanqueoColors.GlassBorder, shape)

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .glass()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        content = content,
    )
}

/**
 * Botón circular con ícono. [touchSize] puede ser mayor que [size] para que sea fácil de tocar.
 * [accent] lo pinta de violeta; úsalo solo para la acción principal de la pantalla.
 */
@Composable
fun RoundIconButton(
    @DrawableRes icon: Int,
    contentDescription: String,
    size: Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    touchSize: Dp = size,
    accent: Boolean = false,
) {
    Box(
        modifier = modifier
            .size(touchSize)
            .clip(CircleShape)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .then(
                    if (accent) Modifier.background(TanqueoColors.Accent, CircleShape)
                    else Modifier.glass(CircleShape),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = contentDescription,
                tint = TanqueoColors.White,
                modifier = Modifier.size(size * 0.56f),
            )
        }
    }
}

/** Botón ancho de la acción principal de la pantalla, en violeta. */
@Composable
fun PrimaryPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val screen = rememberScreenSize()
    Box(
        modifier = modifier
            .heightIn(min = screen.dp(0.2f))
            .clip(RoundedCornerShape(50))
            .background(TanqueoColors.Accent.copy(alpha = if (enabled) 1f else 0.4f))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 18.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = TanqueoColors.White,
            fontSize = screen.sp(14f),
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
    }
}

/** Botón ancho secundario, de vidrio. */
@Composable
fun GlassPillButton(
    text: String,
    @DrawableRes icon: Int?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val screen = rememberScreenSize()
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = screen.dp(0.24f))
            .glass(RoundedCornerShape(50))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = null,
                    tint = TanqueoColors.White,
                    modifier = Modifier
                        .size(screen.dp(0.1f))
                        .padding(end = 4.dp),
                )
            }
            Text(text = text, color = TanqueoColors.White, fontSize = screen.sp(13f), maxLines = 2)
        }
    }
}
