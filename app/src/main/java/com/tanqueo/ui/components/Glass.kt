package com.tanqueo.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
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
import androidx.compose.ui.unit.max
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.Text
import com.tanqueo.ui.theme.TanqueoColors

val GlassCardShape = RoundedCornerShape(26.dp)
private val PillShape = RoundedCornerShape(50)

/** Tamaño mínimo de toque de cualquier botón de la app. */
val MinTouchSize = 48.dp

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
            .fadeInOnAppear()
            .glass()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        content = content,
    )
}

/**
 * Botón circular con ícono. El círculo que se ve mide [size], pero el área de toque nunca baja de
 * [MinTouchSize]. [accent] lo pinta de violeta: solo para la acción principal.
 */
@Composable
fun RoundIconButton(
    @DrawableRes icon: Int,
    contentDescription: String,
    size: Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Boolean = false,
    /**
     * Si es verdadero ocupa solo lo que se ve y el área de toque sobresale sin empujar a sus vecinos.
     * Sirve donde el espacio es muy justo.
     */
    compact: Boolean = false,
) {
    val touch = max(size, MinTouchSize)
    Box(
        modifier = modifier
            .then(if (compact) Modifier.size(size).wrapContentSize(unbounded = true) else Modifier)
            .requiredSize(touch)
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
                modifier = Modifier.size(size * 0.6f),
            )
        }
    }
}

/**
 * Botón píldora de la acción principal: violeta y con resplandor. Si se ve más bajo de 48 dp, Compose
 * extiende su área de toque hasta ese mínimo.
 */
@Composable
fun PrimaryPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    horizontalPadding: Dp? = null,
) {
    val screen = rememberScreenSize()
    Box(
        modifier = modifier
            .then(if (enabled) Modifier.pillGlow(screen.dp(0.05f)) else Modifier)
            .heightIn(min = screen.dp(0.19f))
            .clip(PillShape)
            .background(TanqueoColors.Accent.copy(alpha = if (enabled) 1f else 0.4f))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = horizontalPadding ?: screen.dp(if (screen.isRound) 0.09f else 0.11f)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = TanqueoColors.White,
            fontSize = screen.sp(15f),
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
    }
}

/** Botón píldora secundario, de vidrio. */
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
            .heightIn(min = screen.dp(0.19f))
            .glass(PillShape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = screen.dp(if (screen.isRound) 0.05f else 0.08f), vertical = 2.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = null,
                    tint = TanqueoColors.White,
                    modifier = Modifier
                        .padding(end = 5.dp)
                        .size(screen.dp(0.1f)),
                )
            }
            Text(
                text = text,
                color = TanqueoColors.White,
                fontSize = screen.sp(13f),
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
            )
        }
    }
}
