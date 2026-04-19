@file:Suppress("UNUSED_PARAMETER")
package com.doey.DELTA.core

// ── DOEY COMPONENTS — Foundation-only UI kit ─────────────────────────────────
// Zero dependency on androidx.compose.material3.

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.paint
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.launch

// ── TEXT ──────────────────────────────────────────────────────────────────────

@Composable
fun Text(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontWeight: FontWeight? = null,
    fontStyle: FontStyle? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    style: TextStyle = TextStyle.Default
) {
    val ff = fontFamily ?: style.fontFamily ?: ProductSans
    val merged = style.copy(fontFamily = ff).merge(TextStyle(
        color = color, fontSize = fontSize, fontWeight = fontWeight,
        fontStyle = fontStyle, letterSpacing = letterSpacing,
        textDecoration = textDecoration, textAlign = textAlign ?: TextAlign.Unspecified, lineHeight = lineHeight,
    ))
    BasicText(
        text = text,
        modifier = modifier,
        style = merged,
        overflow = overflow,
        softWrap = softWrap,
        maxLines = maxLines,
        minLines = minLines
    )
}

@Composable
fun Text(
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontWeight: FontWeight? = null,
    fontStyle: FontStyle? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    style: TextStyle = TextStyle.Default
) {
    val ff = fontFamily ?: style.fontFamily ?: ProductSans
    val merged = style.copy(fontFamily = ff).merge(TextStyle(
        color = color, fontSize = fontSize, fontWeight = fontWeight,
        fontStyle = fontStyle, letterSpacing = letterSpacing,
        textDecoration = textDecoration, textAlign = textAlign ?: TextAlign.Unspecified, lineHeight = lineHeight,
    ))
    BasicText(
        text = text,
        modifier = modifier,
        style = merged,
        overflow = overflow,
        softWrap = softWrap,
        maxLines = maxLines,
        minLines = minLines
    )
}

// ── ICON ──────────────────────────────────────────────────────────────────────

@Composable
fun Icon(
    imageVector: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = Color.Unspecified
) {
    val painter = rememberVectorPainter(imageVector)
    val cf = if (tint != Color.Unspecified) ColorFilter.tint(tint) else null
    Box(modifier.paint(painter, colorFilter = cf, contentScale = ContentScale.Fit))
}

// ── ICON BUTTON ───────────────────────────────────────────────────────────────

@Composable
fun IconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .defaultMinSize(minWidth = 40.dp, minHeight = 40.dp)
            .clip(CircleShape)
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center
    ) { content() }
}

// ── SURFACE ───────────────────────────────────────────────────────────────────

@Composable
fun Surface(
    modifier: Modifier = Modifier,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(0.dp),
    color: Color = TauSurface1,
    border: BorderStroke? = null,
    shadowElevation: Dp = 0.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .then(if (shadowElevation > 0.dp) Modifier.shadow(shadowElevation, shape) else Modifier)
            .clip(shape)
            .background(color)
            .then(if (border != null) Modifier.border(border.width, border.brush, shape) else Modifier)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
    ) { content() }
}

// ── BUTTONS ───────────────────────────────────────────────────────────────────

data class DoeyButtonColors(
    val containerColor: Color = TauAccent,
    val contentColor: Color = Color.White,
    val disabledContainerColor: Color = TauSurface3,
    val disabledContentColor: Color = TauText3
)

object ButtonDefaults {
    fun buttonColors(containerColor: Color = TauAccent, contentColor: Color = Color.White) =
        DoeyButtonColors(containerColor = containerColor, contentColor = contentColor)
    fun outlinedButtonColors(containerColor: Color = Color.Transparent, contentColor: Color = TauAccent) =
        DoeyButtonColors(containerColor = containerColor, contentColor = contentColor)
}

@Composable
fun Button(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: DoeyButtonColors = DoeyButtonColors(),
    shape: androidx.compose.ui.graphics.Shape = CircleShape,
    contentPadding: PaddingValues = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
    content: @Composable RowScope.() -> Unit
) {
    val containerColor = if (enabled) colors.containerColor else colors.disabledContainerColor
    val gradient = Brush.verticalGradient(
        colors = listOf(containerColor, containerColor.copy(alpha = 0.9f))
    )
    
    Box(
        modifier = modifier
            .shadow(if (enabled) 4.dp else 0.dp, shape)
            .clip(shape)
            .background(brush = gradient)
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(contentPadding),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) { content() }
    }
}

@Composable
fun OutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: DoeyButtonColors = DoeyButtonColors(containerColor = Color.Transparent, contentColor = TauAccent),
    shape: androidx.compose.ui.graphics.Shape = CircleShape,
    border: BorderStroke? = null,
    content: @Composable RowScope.() -> Unit
) {
    val effectiveBorder = border ?: BorderStroke(1.5.dp, colors.contentColor.copy(alpha = if (enabled) 0.5f else 0.2f))
    Box(
        modifier = modifier
            .clip(shape)
            .background(colors.containerColor)
            .border(effectiveBorder.width, effectiveBorder.brush, shape)
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 24.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) { content() }
    }
}

@Composable
fun TextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) { content() }
}

@Composable
fun FloatingActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = TauAccent,
    contentColor: Color = Color.White,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .shadow(6.dp, CircleShape)
            .clip(CircleShape)
            .background(containerColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { content() }
}

// ── SWITCH ────────────────────────────────────────────────────────────────────

data class DoeySwitchColors(
    val checkedThumbColor: Color = Color.White,
    val checkedTrackColor: Color = TauAccent,
    val uncheckedThumbColor: Color = TauText3,
    val uncheckedTrackColor: Color = Color.Black.copy(alpha = 0.12f),
    val uncheckedBorderColor: Color = Color.Transparent
)

object SwitchDefaults {
    fun colors(
        checkedThumbColor: Color = Color.White,
        checkedTrackColor: Color = TauAccent,
        uncheckedThumbColor: Color = TauText3,
        uncheckedTrackColor: Color = Color.Black.copy(alpha = 0.12f),
        uncheckedBorderColor: Color = Color.Transparent
    ) = DoeySwitchColors(checkedThumbColor, checkedTrackColor, uncheckedThumbColor, uncheckedTrackColor, uncheckedBorderColor)
}

@Composable
fun Switch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: DoeySwitchColors = DoeySwitchColors()
) {
    val trackColor by animateColorAsState(if (checked) colors.checkedTrackColor else colors.uncheckedTrackColor, label = "t")
    val thumbColor by animateColorAsState(if (checked) colors.checkedThumbColor else colors.uncheckedThumbColor, label = "th")
    val offset by animateFloatAsState(if (checked) 24f else 2f, tween(150), label = "o")
    Box(
        modifier = modifier.width(52.dp).height(28.dp)
            .clip(RoundedCornerShape(14.dp)).background(trackColor)
            .border(1.dp, colors.uncheckedBorderColor, RoundedCornerShape(14.dp))
            .then(if (enabled) Modifier.clickable { onCheckedChange(!checked) } else Modifier),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(Modifier.padding(start = offset.dp).size(24.dp).clip(CircleShape).background(thumbColor))
    }
}

// ── TEXT FIELDS ───────────────────────────────────────────────────────────────

data class DoeyTextFieldColors(
    val focusedBorderColor: Color = TauAccent,
    val unfocusedBorderColor: Color = TauSurface3,
    val focusedTextColor: Color = TauText1,
    val unfocusedTextColor: Color = TauText1,
    val focusedLabelColor: Color = TauAccent,
    val unfocusedLabelColor: Color = TauText3,
    val cursorColor: Color = TauAccent,
    val focusedPlaceholderColor: Color = TauText3,
    val unfocusedPlaceholderColor: Color = TauText3,
    val focusedContainerColor: Color = Color(0xB3FFFFFF), // 70% opacidad
    val unfocusedContainerColor: Color = Color(0x8CFFFFFF), // 55% opacidad
    val focusedIndicatorColor: Color = Color.Transparent,
    val unfocusedIndicatorColor: Color = Color.Transparent
)

object TextFieldDefaults {
    fun colors(
        focusedContainerColor: Color = Color.Transparent,
        unfocusedContainerColor: Color = Color.Transparent,
        focusedIndicatorColor: Color = Color.Transparent,
        unfocusedIndicatorColor: Color = Color.Transparent,
        focusedTextColor: Color = TauText1,
        unfocusedTextColor: Color = TauText1,
        focusedBorderColor: Color = TauAccent,
        unfocusedBorderColor: Color = TauSurface3
    ) = DoeyTextFieldColors(
        focusedContainerColor = focusedContainerColor,
        unfocusedContainerColor = unfocusedContainerColor,
        focusedIndicatorColor = focusedIndicatorColor,
        unfocusedIndicatorColor = unfocusedIndicatorColor,
        focusedTextColor = focusedTextColor,
        unfocusedTextColor = unfocusedTextColor,
        focusedBorderColor = focusedBorderColor,
        unfocusedBorderColor = unfocusedBorderColor
    )
}

object OutlinedTextFieldDefaults {
    fun colors(
        focusedBorderColor: Color = TauAccent,
        unfocusedBorderColor: Color = Color(0xD9FFFFFF),
        focusedTextColor: Color = TauText1,
        unfocusedTextColor: Color = TauText1,
        focusedLabelColor: Color = TauAccent,
        unfocusedLabelColor: Color = TauText3,
        cursorColor: Color = TauAccent,
        focusedPlaceholderColor: Color = TauText3,
        unfocusedPlaceholderColor: Color = TauText3,
        focusedContainerColor: Color = Color(0x8CFFFFFF),
        unfocusedContainerColor: Color = Color(0x8CFFFFFF)
    ) = DoeyTextFieldColors(
        focusedBorderColor, unfocusedBorderColor, focusedTextColor, unfocusedTextColor,
        focusedLabelColor, unfocusedLabelColor, cursorColor,
        focusedPlaceholderColor, unfocusedPlaceholderColor, focusedContainerColor, unfocusedContainerColor
    )
}

@Composable
private fun BaseTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier,
    placeholder: @Composable (() -> Unit)?,
    label: @Composable (() -> Unit)?,
    leadingIcon: @Composable (() -> Unit)?,
    trailingIcon: @Composable (() -> Unit)?,
    singleLine: Boolean,
    maxLines: Int,
    enabled: Boolean,
    readOnly: Boolean,
    visualTransformation: VisualTransformation,
    textStyle: TextStyle?,
    keyboardOptions: KeyboardOptions,
    keyboardActions: KeyboardActions,
    colors: DoeyTextFieldColors,
    shape: androidx.compose.ui.graphics.Shape
) {
    var focused by remember { mutableStateOf(false) }
    val borderColor by animateColorAsState(if (focused) colors.focusedBorderColor else colors.unfocusedBorderColor, label = "b")
    val containerColor by animateColorAsState(if (focused) colors.focusedContainerColor else colors.unfocusedContainerColor, label = "c")
    val textColor = if (focused) colors.focusedTextColor else colors.unfocusedTextColor
    val resolvedStyle = textStyle ?: TextStyle(color = textColor, fontFamily = ProductSans, fontSize = 14.sp)

    Column(modifier) {
        if (label != null) Box(Modifier.padding(start = 4.dp, bottom = 4.dp)) { label() }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth()
                .clip(shape).background(containerColor)
                .border(1.5.dp, borderColor, shape)
                .onFocusChanged { focused = it.isFocused },
            singleLine = singleLine,
            maxLines = maxLines,
            enabled = enabled,
            readOnly = readOnly,
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            textStyle = resolvedStyle,
            cursorBrush = SolidColor(colors.cursorColor),
            decorationBox = { inner ->
                Row(
                    Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (leadingIcon != null) { leadingIcon(); Spacer(Modifier.width(8.dp)) }
                    Box(Modifier.weight(1f)) {
                        if (value.isEmpty() && placeholder != null) placeholder()
                        inner()
                    }
                    if (trailingIcon != null) { Spacer(Modifier.width(8.dp)); trailingIcon() }
                }
            }
        )
    }
}

@Composable
fun TextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    label: @Composable (() -> Unit)? = null,
    singleLine: Boolean = false,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    colors: DoeyTextFieldColors = DoeyTextFieldColors(),
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(12.dp)
) = BaseTextField(value, onValueChange, modifier, placeholder, label, leadingIcon, trailingIcon,
    singleLine, maxLines, enabled, readOnly, visualTransformation, textStyle, keyboardOptions, keyboardActions, colors, shape)

@Composable
fun OutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: @Composable (() -> Unit)? = null,
    label: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    singleLine: Boolean = false,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    colors: DoeyTextFieldColors = DoeyTextFieldColors(),
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(12.dp)
) = BaseTextField(value, onValueChange, modifier, placeholder, label, leadingIcon, trailingIcon,
    singleLine, maxLines, enabled, readOnly, visualTransformation, textStyle, keyboardOptions, keyboardActions, colors, shape)

// ── SCAFFOLD ──────────────────────────────────────────────────────────────────

@Composable
fun Scaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    containerColor: Color = TauBg,
    useGlassBackground: Boolean = true,
    content: @Composable (PaddingValues) -> Unit
) {
    Box(modifier.fillMaxSize()) {
        if (useGlassBackground) {
            GlassBackground()
        } else {
            Box(Modifier.fillMaxSize().background(containerColor))
        }
        
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            topBar()
            Box(Modifier.weight(1f)) { content(PaddingValues(0.dp)) }
            bottomBar()
        }
        Box(Modifier.align(Alignment.BottomEnd).padding(16.dp).navigationBarsPadding()) { floatingActionButton() }
    }
}

// ── TOP APP BAR ───────────────────────────────────────────────────────────────

data class DoeyTopBarColors(val containerColor: Color = Color.Transparent)

object TopAppBarDefaults {
    fun topAppBarColors(containerColor: Color = Color.Transparent) = DoeyTopBarColors(containerColor)
}

@Composable
fun TopAppBar(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    colors: DoeyTopBarColors = DoeyTopBarColors()
) {
    Row(
        modifier = modifier.fillMaxWidth().background(colors.containerColor).height(56.dp).padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        navigationIcon()
        Box(Modifier.weight(1f)) { title() }
        Row { actions() }
    }
}

// ── NAVIGATION DRAWER ─────────────────────────────────────────────────────────

enum class DrawerValue { Closed, Open }

class DrawerState(initialValue: DrawerValue = DrawerValue.Closed) {
    var currentValue by mutableStateOf(initialValue)
    val isOpen  get() = currentValue == DrawerValue.Open
    val isClosed get() = currentValue == DrawerValue.Closed
    suspend fun open()  { currentValue = DrawerValue.Open }
    suspend fun close() { currentValue = DrawerValue.Closed }
}

@Composable
fun rememberDrawerState(initialValue: DrawerValue = DrawerValue.Closed): DrawerState =
    remember { DrawerState(initialValue) }

@Composable
fun ModalNavigationDrawer(
    drawerContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    drawerState: DrawerState = rememberDrawerState(DrawerValue.Closed),
    gesturesEnabled: Boolean = true,
    content: @Composable () -> Unit
) {
    val offset by animateFloatAsState(if (drawerState.isOpen) 0f else -1f, tween(300), label = "d")
    val scope = rememberCoroutineScope()
    Box(modifier.fillMaxSize()) {
        content()
        if (drawerState.isOpen) {
            Box(
                Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f))
                    .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {
                        scope.launch { drawerState.close() }
                    }
            )
        }
        Box(Modifier.fillMaxHeight().width(300.dp).offset(x = (300 * offset).dp)) { drawerContent() }
    }
}

@Composable
fun ModalDrawerSheet(
    modifier: Modifier = Modifier,
    drawerContainerColor: Color = TauBg,
    drawerContentColor: Color = TauText1,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier.fillMaxHeight().background(drawerContainerColor)) { content() }
}

// ── ALERT DIALOG ──────────────────────────────────────────────────────────────

@Composable
fun AlertDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: @Composable (() -> Unit)? = null,
    title: @Composable (() -> Unit)? = null,
    text: @Composable (() -> Unit)? = null,
    containerColor: Color = TauSurface1,
    properties: DialogProperties = DialogProperties()
) {
    Dialog(onDismissRequest = onDismissRequest, properties = properties) {
        Box(modifier.shadow(8.dp, RoundedCornerShape(24.dp)).clip(RoundedCornerShape(24.dp))
            .background(containerColor).padding(24.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (title != null) title()
                if (text  != null) text()
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically) {
                    if (dismissButton != null) dismissButton()
                    Spacer(Modifier.width(8.dp))
                    confirmButton()
                }
            }
        }
    }
}

// ── TAB ROW ───────────────────────────────────────────────────────────────────

@Composable
fun TabRow(
    selectedTabIndex: Int,
    modifier: Modifier = Modifier,
    containerColor: Color = TauSurface1,
    contentColor: Color = TauAccent,
    indicator: @Composable () -> Unit = {},
    divider: @Composable () -> Unit = { HorizontalDivider() },
    tabs: @Composable () -> Unit
) {
    Column(modifier.fillMaxWidth().background(containerColor)) {
        Row(Modifier.fillMaxWidth()) { tabs() }
        divider()
    }
}

@Composable
fun RowScope.Tab(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    text: @Composable (() -> Unit)? = null,
    icon: @Composable (() -> Unit)? = null
) {
    Column(
        modifier = modifier.weight(1f).clickable(onClick = onClick).padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (icon != null) icon()
        if (text != null) text()
        Box(Modifier.padding(top = 4.dp).fillMaxWidth().height(2.dp)
            .background(if (selected) TauAccent else Color.Transparent))
    }
}

@Composable
fun Tab(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    text: @Composable (() -> Unit)? = null,
    icon: @Composable (() -> Unit)? = null
) {
    Column(
        modifier = modifier.clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (icon != null) icon()
        if (text != null) text()
        Box(Modifier.padding(top = 4.dp).fillMaxWidth().height(2.dp)
            .background(if (selected) TauAccent else Color.Transparent))
    }
}

// ── CHIP ──────────────────────────────────────────────────────────────────────

data class DoeyChipColors(
    val containerColor: Color = TauSurface1,
    val selectedContainerColor: Color = TauAccent.copy(alpha = 0.15f),
    val borderColor: Color = TauSurface3,
    val selectedBorderColor: Color = TauAccent.copy(alpha = 0.4f),
    val labelColor: Color = TauText1,
    val selectedLabelColor: Color = TauAccent,
    val leadingIconColor: Color = TauText2,
    val selectedLeadingIconColor: Color = TauAccent
)

object FilterChipDefaults {
    fun filterChipColors(
        containerColor: Color = TauSurface1,
        selectedContainerColor: Color = TauAccent.copy(alpha = 0.15f),
        labelColor: Color = TauText1,
        selectedLabelColor: Color = TauAccent,
        leadingIconColor: Color = TauText2,
        selectedLeadingIconColor: Color = TauAccent
    ) = DoeyChipColors(containerColor, selectedContainerColor,
        TauSurface3, TauAccent.copy(alpha = 0.4f),
        labelColor, selectedLabelColor, leadingIconColor, selectedLeadingIconColor)

    fun filterChipBorder(
        borderColor: Color = TauSurface3,
        selectedBorderColor: Color = TauAccent.copy(alpha = 0.4f),
        borderWidth: Dp = 1.dp,
        selectedBorderWidth: Dp = 1.dp,
        enabled: Boolean = true,
        selected: Boolean = false
    ) = BorderStroke(if (selected) selectedBorderWidth else borderWidth,
                     if (selected) selectedBorderColor else borderColor)
}

@Composable
fun FilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: @Composable (() -> Unit)? = null,
    colors: DoeyChipColors = DoeyChipColors(),
    border: BorderStroke? = null
) {
    val bg = if (selected) colors.selectedContainerColor else colors.containerColor
    val bStroke = border ?: BorderStroke(1.dp, if (selected) colors.selectedBorderColor else colors.borderColor)
    Row(
        modifier = modifier.clip(CircleShape).background(bg)
            .border(bStroke.width, bStroke.brush, CircleShape)
            .clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (leadingIcon != null) leadingIcon()
        label()
    }
}

// ── DIVIDER ───────────────────────────────────────────────────────────────────

@Composable
fun HorizontalDivider(modifier: Modifier = Modifier, thickness: Dp = 1.dp, color: Color = TauSurface3) =
    Box(modifier.fillMaxWidth().height(thickness).background(color))

@Composable
fun Divider(modifier: Modifier = Modifier, thickness: Dp = 1.dp, color: Color = TauSurface3) =
    HorizontalDivider(modifier, thickness, color)

// ── BADGE ─────────────────────────────────────────────────────────────────────

@Composable
fun Badge(
    modifier: Modifier = Modifier,
    containerColor: Color = TauRed,
    contentColor: Color = Color.White,
    content: @Composable (() -> Unit)? = null
) {
    Box(
        modifier = modifier.defaultMinSize(16.dp, 16.dp).clip(CircleShape).background(containerColor).padding(horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) { content?.invoke() }
}

// ── SLIDER ────────────────────────────────────────────────────────────────────

data class DoeySliderColors(
    val thumbColor: Color = TauAccent,
    val activeTrackColor: Color = TauAccent,
    val inactiveTrackColor: Color = TauSurface3
)

object SliderDefaults {
    fun colors(thumbColor: Color = TauAccent, activeTrackColor: Color = TauAccent, inactiveTrackColor: Color = TauSurface3) =
        DoeySliderColors(thumbColor, activeTrackColor, inactiveTrackColor)
}

@Composable
fun Slider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    enabled: Boolean = true,
    colors: DoeySliderColors = DoeySliderColors(),
    onValueChangeFinished: (() -> Unit)? = null
) {
    val fraction = ((value - valueRange.start) / (valueRange.endInclusive - valueRange.start)).coerceIn(0f, 1f)
    BoxWithConstraints(modifier.fillMaxWidth().height(40.dp), contentAlignment = Alignment.Center) {
        val trackWidthPx = with(LocalDensity.current) { maxWidth.toPx() }
        Box(
            Modifier.fillMaxWidth().height(4.dp).align(Alignment.Center)
                .clip(RoundedCornerShape(2.dp)).background(colors.inactiveTrackColor)
                .then(if (enabled) Modifier.pointerInput(valueRange) {
                    detectHorizontalDragGestures(onDragEnd = { onValueChangeFinished?.invoke() }) { change, _ ->
                        val f = (change.position.x / trackWidthPx).coerceIn(0f, 1f)
                        onValueChange(valueRange.start + f * (valueRange.endInclusive - valueRange.start))
                    }
                } else Modifier)
        ) {
            Box(Modifier.fillMaxWidth(fraction).fillMaxHeight().background(colors.activeTrackColor))
        }
        Box(
            Modifier.align(Alignment.CenterStart).offset(x = (fraction * maxWidth.value - 10).dp)
                .size(20.dp).shadow(2.dp, CircleShape).clip(CircleShape).background(colors.thumbColor)
        )
    }
}

// ── CARD ──────────────────────────────────────────────────────────────────────

@Composable
fun Card(
    modifier: Modifier = Modifier,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(16.dp),
    color: Color = TauSurface1,
    border: BorderStroke? = null,
    shadowElevation: Dp = 2.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) = Surface(modifier, shape, color, border, shadowElevation, onClick, content)

object CardDefaults {
    fun cardColors(containerColor: Color = TauSurface1) = containerColor
}

// ── EXPOSED DROPDOWN MENU ─────────────────────────────────────────────────────

@Composable
fun ExposedDropdownMenuBox(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(modifier.clickable { onExpandedChange(!expanded) }) { content() }
}

object ExposedDropdownMenuDefaults {
    fun menuAnchor(): Modifier = Modifier
    @Composable
    fun TrailingIcon(expanded: Boolean) {
        Icon(
            imageVector = if (expanded) CustomIcons.ExpandLess else CustomIcons.ExpandMore,
            contentDescription = null, tint = TauText2
        )
    }
}

@Composable
fun ExposedDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    if (expanded) {
        Box(modifier.shadow(8.dp, RoundedCornerShape(12.dp)).clip(RoundedCornerShape(12.dp)).background(TauSurface1)) {
            Column { content() }
        }
    }
}

@Composable
fun DropdownMenuItem(
    text: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leadingIcon != null) { leadingIcon(); Spacer(Modifier.width(12.dp)) }
        text()
    }
}

fun Modifier.menuAnchor(): Modifier = this

// ── NO-OP ANNOTATION ─────────────────────────────────────────────────────────

@RequiresOptIn(message = "Doey experimental API")
annotation class ExperimentalMaterial3Api
