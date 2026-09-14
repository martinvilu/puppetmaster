package com.sounddeck.ui

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.FiberSmartRecord
import androidx.compose.material.icons.filled.Http
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sounddeck.core.model.DynamicPadState
import com.sounddeck.core.model.PadConfig

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun PadView(
    pad: PadConfig,
    dynamicState: DynamicPadState?,
    isEditMode: Boolean,
    isLocked: Boolean = false,
    cooldownRemainingFraction: Float = 0f,
    multiStateIndex: Int = 0,
    isSwapSource: Boolean = false,
    hasCopiedStyle: Boolean = false,
    macroDelayProgress: Float = 0f,
    isLoopingActive: Boolean = false,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
    onEditClick: () -> Unit,
    onDuplicateClick: (() -> Unit)? = null,
    onSelectForSwap: (() -> Unit)? = null,
    onCopyStyle: (() -> Unit)? = null,
    onPasteStyle: (() -> Unit)? = null,
    onRelease: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // QoL 5.5: Hold to Play release detection
    androidx.compose.runtime.LaunchedEffect(isPressed) {
        if (!isPressed && pad.holdToPlay && !isEditMode) {
            onRelease?.invoke()
        }
    }

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1.0f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 600f),
        label = "pad_scale"
    )

    // A2: Multi-state resolution
    val currentStep = if (pad.multiStates.isNotEmpty()) {
        pad.multiStates.getOrNull(multiStateIndex % pad.multiStates.size)
    } else null

    val effectiveLabel = currentStep?.label?.takeIf { it.isNotBlank() } ?: pad.visual.label
    val rawBgColor = currentStep?.backgroundColor?.takeIf { it.isNotBlank() } ?: pad.visual.backgroundColor
    val effectiveIconAsset = currentStep?.iconAsset?.takeIf { it.isNotBlank() } ?: pad.visual.iconAsset

    val bgColor = remember(rawBgColor) {
        parseHexColor(rawBgColor, Color(0xFF1E1E1E))
    }

    val secColor = remember(pad.visual.secondaryColor) {
        pad.visual.secondaryColor?.let { parseHexColor(it, bgColor) }
    }

    val customBorderColor = remember(pad.visual.borderColor) {
        pad.visual.borderColor?.let { parseHexColor(it, Color.Transparent) }
    }

    val textColor = remember(pad.visual.textColor) {
        parseHexColor(pad.visual.textColor, Color.White)
    }

    val percentage = dynamicState?.percentage
    val textBadge = dynamicState?.textBadge

    // Background styling (Gradient or Solid)
    val backgroundModifier = if (secColor != null) {
        Modifier.background(
            Brush.verticalGradient(
                colors = listOf(bgColor, secColor)
            )
        )
    } else {
        Modifier.background(bgColor)
    }

    val effectiveBorderColor = if (isSwapSource) {
        Color(0xFF00E5FF)
    } else if (isEditMode) {
        Color(0xFFFFD600)
    } else if (isLocked) {
        Color(0xFFFF5252).copy(alpha = 0.6f)
    } else {
        customBorderColor ?: Color.White.copy(alpha = 0.15f)
    }

    val effectiveBorderWidth = if (isSwapSource) 3.dp else if (isEditMode) 2.dp else (pad.visual.borderWidthDp.coerceIn(1, 6)).dp

    Box(
        modifier = modifier
            .padding(3.dp)
            .scale(scale)
            .clip(RoundedCornerShape(12.dp))
            .then(backgroundModifier)
            .border(
                width = effectiveBorderWidth,
                color = effectiveBorderColor,
                shape = RoundedCornerShape(12.dp)
            )
            .testTag("pad_${pad.id}")
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    if (isEditMode) {
                        if (onSelectForSwap != null && isSwapSource) {
                            onSelectForSwap()
                        } else {
                            onEditClick()
                        }
                    } else {
                        if (pad.visual.hapticFeedback) triggerHaptic(context)
                        onTap()
                    }
                },
                onLongClick = {
                    if (isEditMode) {
                        onEditClick()
                    } else {
                        if (pad.visual.hapticFeedback) triggerHaptic(context, strong = true)
                        onLongPress()
                    }
                }
            )
    ) {
        // Custom Image / Background Art if imageUri is set
        if (!pad.visual.imageUri.isNullOrBlank()) {
            coil.compose.AsyncImage(
                model = pad.visual.imageUri,
                contentDescription = pad.visual.label,
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(0.35f)
            )
        }

        // Dynamic percentage background gauge (0-100%)
        if (percentage != null && percentage > 0f) {
            val fraction = (percentage / 100f).coerceIn(0f, 1f)
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.12f),
                                Color.White.copy(alpha = 0.28f)
                            )
                        )
                    )
            )
        }

        // Visual Cooldown Sweep Overlay (1.4 Cooldown Visual)
        if (cooldownRemainingFraction > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(cooldownRemainingFraction)
                    .align(Alignment.BottomCenter)
                    .background(Color.Black.copy(alpha = 0.55f))
            )
        }

        // QoL 4.2: Macro Delay Visual Progress
        if (macroDelayProgress > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .align(Alignment.TopCenter)
                    .background(Color(0xFF232834))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(macroDelayProgress.coerceIn(0f, 1f))
                        .background(Color(0xFF00E5FF))
                )
            }
        }

        // QoL 1.1: Swap selected banner
        if (isSwapSource) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .clip(RoundedCornerShape(bottomStart = 4.dp, bottomEnd = 4.dp))
                    .background(Color(0xFF00E5FF))
                    .padding(horizontal = 4.dp, vertical = 1.dp)
            ) {
                Text(
                    text = "MOVER",
                    color = Color.Black,
                    fontSize = 7.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }

        // Tactile bevel shine on top edge
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(Color.White.copy(alpha = 0.25f))
        )

        // Main Pad Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header Row: Indicators & dynamic text badge & lock
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Pipeline action indicators (Audio, OBS, HTTP)
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (pad.onTap?.audio != null) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = "Audio Action",
                            tint = textColor.copy(alpha = 0.6f),
                            modifier = Modifier.size(11.dp)
                        )
                    }
                    if (pad.onTap?.obsAction != null) {
                        Icon(
                            imageVector = Icons.Default.Videocam,
                            contentDescription = "OBS Action",
                            tint = Color(0xFF64B5F6),
                            modifier = Modifier.size(11.dp)
                        )
                    }
                    if (pad.onTap?.httpAction != null) {
                        Icon(
                            imageVector = Icons.Default.Http,
                            contentDescription = "HTTP Action",
                            tint = Color(0xFF81C784),
                            modifier = Modifier.size(11.dp)
                        )
                    }
                    if (pad.polling?.enabled == true) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF00E676))
                        )
                    }
                }

                // Dynamic text badge from JSONPath, MultiState step, or Edit / Lock Icon
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (isLocked) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Locked Pad",
                            tint = Color(0xFFFF5252),
                            modifier = Modifier.size(12.dp)
                        )
                    }

                    if (isLoopingActive) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF00E5FF))
                                .padding(horizontal = 3.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "LOOP",
                                color = Color.Black,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }

                    if (!pad.keyShortcut.isNullOrBlank()) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF1F2430))
                                .padding(horizontal = 3.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "[${pad.keyShortcut}]",
                                color = Color(0xFF00E5FF),
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (pad.multiStates.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF00E5FF).copy(alpha = 0.25f))
                                .padding(horizontal = 3.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "${(multiStateIndex % pad.multiStates.size) + 1}/${pad.multiStates.size}",
                                color = Color(0xFF00E5FF),
                                fontSize = 8.sp,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1
                            )
                        }
                    }

                    if (textBadge != null && textBadge.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.Black.copy(alpha = 0.6f))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = textBadge,
                                color = Color(0xFF00E5FF),
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    } else if (isEditMode) {
                        Row(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
                            if (onSelectForSwap != null) {
                                Icon(
                                    imageVector = Icons.Default.SwapHoriz,
                                    contentDescription = "Mover pad",
                                    tint = if (isSwapSource) Color(0xFF00E5FF) else Color.White.copy(alpha = 0.7f),
                                    modifier = Modifier
                                        .size(13.dp)
                                        .clickable { onSelectForSwap() }
                                )
                            }
                            if (onCopyStyle != null) {
                                Icon(
                                    imageVector = Icons.Default.Palette,
                                    contentDescription = "Copiar estilo",
                                    tint = Color(0xFFFFD600),
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clickable { onCopyStyle() }
                                )
                            }
                            if (onPasteStyle != null && hasCopiedStyle) {
                                Icon(
                                    imageVector = Icons.Default.ContentPaste,
                                    contentDescription = "Pegar estilo",
                                    tint = Color(0xFF00E676),
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clickable { onPasteStyle() }
                                )
                            }
                            if (onDuplicateClick != null) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Duplicate pad",
                                    tint = Color(0xFF80D8FF),
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clickable { onDuplicateClick() }
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit pad",
                                tint = Color(0xFFFFD600),
                                modifier = Modifier
                                    .size(12.dp)
                                    .clickable { onEditClick() }
                            )
                        }
                    }
                }
            }

            val iconVector = resolveIcon(effectiveIconAsset)
            val customFontSize = (pad.visual.labelFontSizeSp.coerceIn(8, 20)).sp
            val customLineHeight = (pad.visual.labelFontSizeSp + 2).coerceIn(10, 24).sp

            val labelComposable: @Composable () -> Unit = {
                Text(
                    text = effectiveLabel,
                    color = textColor,
                    fontSize = customFontSize,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    lineHeight = customLineHeight,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            val iconComposable: @Composable () -> Unit = {
                if (iconVector != null) {
                    Icon(
                        imageVector = iconVector,
                        contentDescription = effectiveLabel,
                        tint = textColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // D3: Typography and Alignment placement (TOP, CENTER, BOTTOM)
            when (pad.visual.labelAlignment) {
                com.sounddeck.core.model.LabelAlignment.TOP -> {
                    labelComposable()
                    iconComposable()
                }
                com.sounddeck.core.model.LabelAlignment.CENTER -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        iconComposable()
                        labelComposable()
                    }
                }
                com.sounddeck.core.model.LabelAlignment.BOTTOM -> {
                    iconComposable()
                    labelComposable()
                }
            }

            // Bottom Progress Gauge Level Number (if percentage active)
            if (percentage != null) {
                Text(
                    text = "${percentage.toInt()}%",
                    color = textColor.copy(alpha = 0.7f),
                    fontSize = 8.sp,
                    fontWeight = FontWeight.SemiBold
                )
            } else {
                Spacer(modifier = Modifier.height(2.dp))
            }
        }
    }
}

private fun resolveIcon(name: String?): ImageVector? {
    return IconRegistry.resolveIcon(name)
}

private fun parseHexColor(hex: String?, fallback: Color): Color {
    if (hex.isNullOrBlank()) return fallback
    return try {
        val clean = hex.removePrefix("#")
        val colorInt = when (clean.length) {
            6 -> (0xFF000000 or clean.toLong(16)).toInt()
            8 -> clean.toLong(16).toInt()
            else -> return fallback
        }
        Color(colorInt)
    } catch (e: Exception) {
        fallback
    }
}

private fun triggerHaptic(context: Context, strong: Boolean = false) {
    try {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val amplitude = if (strong) VibrationEffect.DEFAULT_AMPLITUDE else 80
            val duration = if (strong) 50L else 20L
            vibrator.vibrate(VibrationEffect.createOneShot(duration, amplitude))
        } else {
            vibrator.vibrate(if (strong) 50L else 20L)
        }
    } catch (e: Exception) {
        // Ignore haptic failures
    }
}
