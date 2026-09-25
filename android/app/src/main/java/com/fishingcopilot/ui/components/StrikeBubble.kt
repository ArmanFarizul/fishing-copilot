package com.fishingcopilot.ui.components

import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.fishingcopilot.R
import com.fishingcopilot.ui.theme.NauticalCyan
import com.fishingcopilot.ui.theme.OceanMidnight
import com.fishingcopilot.ui.theme.StrikePosition
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val BubbleWidth = 76.dp
private val BubbleHeight = 76.dp
private val EdgeMargin = 8.dp

/**
 * The Strike button as a bubble the angler can drag anywhere, so it never sits on text they need.
 * On release it glides to the nearer side edge and remembers where it was parked. Tapping logs a catch.
 */
@Composable
fun StrikeBubble(
    position: StrikePosition,
    onMoved: (StrikePosition) -> Unit,
    onStrike: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val animationsOff = remember {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val scope = rememberCoroutineScope()
        val (minX, maxX, maxY) = with(density) {
            Triple(EdgeMargin.toPx(), (maxWidth - BubbleWidth - EdgeMargin).toPx(), (maxHeight - BubbleHeight - EdgeMargin).toPx())
        }
        val minY = with(density) { EdgeMargin.toPx() }
        fun target(p: StrikePosition) = Offset(if (p.right) maxX else minX, minY + (maxY - minY) * p.fraction)

        val offset = remember { Animatable(target(position), Offset.VectorConverter) }
        var dragging by remember { mutableStateOf(false) }
        // Follow a saved or rotated position, but never fight the finger.
        LaunchedEffect(position, maxX, maxY) { if (!dragging) offset.snapTo(target(position)) }

        fun settle() {
            val right = offset.value.x + with(density) { BubbleWidth.toPx() } / 2 > (minX + maxX + with(density) { BubbleWidth.toPx() }) / 2
            val fraction = if (maxY > minY) ((offset.value.y - minY) / (maxY - minY)).coerceIn(0f, 1f) else 0f
            val parked = StrikePosition(right, fraction)
            scope.launch {
                if (animationsOff) offset.snapTo(target(parked))
                else offset.animateTo(target(parked), spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow))
                onMoved(parked)
            }
        }

        val label = stringResource(R.string.strike_button)
        val description = stringResource(R.string.strike_button_description)
        val moveLeft = stringResource(R.string.strike_move_left)
        val moveRight = stringResource(R.string.strike_move_right)
        val interaction = remember { MutableInteractionSource() }
        val pressed by interaction.collectIsPressedAsState()
        val scale by animateFloatAsState(
            targetValue = if (pressed || dragging) 0.9f else 1f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
            label = "strikeScale"
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .offset { IntOffset(offset.value.x.roundToInt(), offset.value.y.roundToInt()) }
                .size(BubbleWidth, BubbleHeight)
                .graphicsLayer { scaleX = scale; scaleY = scale }
                .pointerInput(minX, maxX, maxY) {
                    detectDragGestures(
                        onDragStart = { dragging = true },
                        onDragEnd = { dragging = false; settle() },
                        onDragCancel = { dragging = false; settle() }
                    ) { change, amount ->
                        change.consume()
                        val next = offset.value + amount
                        scope.launch { offset.snapTo(Offset(next.x.coerceIn(minX, maxX), next.y.coerceIn(minY, maxY))) }
                    }
                }
                .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onStrike)
                .semantics {
                    contentDescription = description
                    // Screen-reader users cannot drag, so offer the same move as actions.
                    customActions = listOf(
                        CustomAccessibilityAction(if (position.right) moveLeft else moveRight) {
                            onMoved(position.copy(right = !position.right)); true
                        }
                    )
                }
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(BubbleWidth, 54.dp)) {
                StrikeGlow(animationsOff)
                Image(
                    painter = painterResource(R.drawable.ic_strike_3d),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(46.dp)
                )
            }
            Surface(shape = RoundedCornerShape(8.dp), color = NauticalCyan) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Black,
                    color = OceanMidnight,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun StrikeGlow(animationsOff: Boolean) {
    val alpha = if (animationsOff) 0.2f else {
        val transition = rememberInfiniteTransition(label = "strikePulse")
        val pulse by transition.animateFloat(
            initialValue = 0.10f,
            targetValue = 0.32f,
            animationSpec = infiniteRepeatable(tween(1400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "strikePulseAlpha"
        )
        pulse
    }
    Canvas(modifier = Modifier.size(54.dp)) {
        val radius = size.minDimension / 2f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(NauticalCyan.copy(alpha = alpha), NauticalCyan.copy(alpha = alpha * 0.3f), Color.Transparent),
                center = center,
                radius = radius
            ),
            radius = radius
        )
    }
}
