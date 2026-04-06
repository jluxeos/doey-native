package com.doey.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Renderiza texto con soporte básico de Markdown:
 * - **negrita**
 * - *cursiva* o _cursiva_
 * - ~~tachado~~
 * - `código inline`
 * - # Encabezados (h1-h3)
 * - - listas con guión
 * - * listas con asterisco
 * - Líneas en blanco como separadores
 */
@Composable
fun MarkdownText(
    text: String,
    color: Color = Label1Light,
    fontSize: TextUnit = 14.sp,
    lineHeight: TextUnit = 20.sp,
    modifier: Modifier = Modifier
) {
    val lines = text.lines()
    val blocks = parseMarkdownBlocks(lines)

    Column(modifier = modifier) {
        blocks.forEach { block ->
            when (block) {
                is MarkdownBlock.Heading -> {
                    Text(
                        text       = parseInlineMarkdown(block.text, color),
                        color      = color,
                        fontSize   = when (block.level) {
                            1 -> (fontSize.value + 6).sp
                            2 -> (fontSize.value + 4).sp
                            else -> (fontSize.value + 2).sp
                        },
                        fontWeight = FontWeight.Bold,
                        lineHeight = lineHeight
                    )
                    Spacer(Modifier.height(4.dp))
                }
                is MarkdownBlock.ListItem -> {
                    Row {
                        Text(
                            text     = "•",
                            color    = color,
                            fontSize = fontSize,
                            modifier = Modifier.padding(end = 6.dp, top = 1.dp)
                        )
                        Text(
                            text       = parseInlineMarkdown(block.text, color),
                            color      = color,
                            fontSize   = fontSize,
                            lineHeight = lineHeight
                        )
                    }
                }
                is MarkdownBlock.Paragraph -> {
                    Text(
                        text       = parseInlineMarkdown(block.text, color),
                        color      = color,
                        fontSize   = fontSize,
                        lineHeight = lineHeight
                    )
                }
                is MarkdownBlock.Spacer -> {
                    Spacer(Modifier.height(6.dp))
                }
            }
        }
    }
}

// ── Bloques de Markdown ───────────────────────────────────────────────────────
sealed class MarkdownBlock {
    data class Heading(val level: Int, val text: String) : MarkdownBlock()
    data class ListItem(val text: String) : MarkdownBlock()
    data class Paragraph(val text: String) : MarkdownBlock()
    object Spacer : MarkdownBlock()
}

fun parseMarkdownBlocks(lines: List<String>): List<MarkdownBlock> {
    val blocks = mutableListOf<MarkdownBlock>()
    var i = 0
    while (i < lines.size) {
        val line = lines[i]
        when {
            // Encabezados
            line.startsWith("### ") -> blocks.add(MarkdownBlock.Heading(3, line.removePrefix("### ")))
            line.startsWith("## ")  -> blocks.add(MarkdownBlock.Heading(2, line.removePrefix("## ")))
            line.startsWith("# ")   -> blocks.add(MarkdownBlock.Heading(1, line.removePrefix("# ")))
            // Listas
            line.matches(Regex("^[-*+]\\s+.*")) -> {
                val text = line.replaceFirst(Regex("^[-*+]\\s+"), "")
                blocks.add(MarkdownBlock.ListItem(text))
            }
            line.matches(Regex("^\\d+\\.\\s+.*")) -> {
                val text = line.replaceFirst(Regex("^\\d+\\.\\s+"), "")
                blocks.add(MarkdownBlock.ListItem(text))
            }
            // Líneas en blanco
            line.isBlank() -> {
                if (blocks.lastOrNull() !is MarkdownBlock.Spacer) {
                    blocks.add(MarkdownBlock.Spacer)
                }
            }
            // Párrafo normal
            else -> {
                // Acumular líneas consecutivas de párrafo
                val sb = StringBuilder(line)
                while (i + 1 < lines.size) {
                    val next = lines[i + 1]
                    if (next.isBlank() ||
                        next.startsWith("#") ||
                        next.matches(Regex("^[-*+]\\s+.*")) ||
                        next.matches(Regex("^\\d+\\.\\s+.*"))) break
                    i++
                    sb.append(" ").append(next)
                }
                blocks.add(MarkdownBlock.Paragraph(sb.toString()))
            }
        }
        i++
    }
    return blocks
}

// ── Inline Markdown (negrita, cursiva, código, tachado) ───────────────────────
fun parseInlineMarkdown(text: String, baseColor: Color): AnnotatedString {
    return buildAnnotatedString {
        var i = 0
        while (i < text.length) {
            when {
                // Negrita + cursiva: ***texto***
                text.startsWith("***", i) -> {
                    val end = text.indexOf("***", i + 3)
                    if (end != -1) {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic)) {
                            append(text.substring(i + 3, end))
                        }
                        i = end + 3
                    } else { append(text[i]); i++ }
                }
                // Negrita: **texto**
                text.startsWith("**", i) -> {
                    val end = text.indexOf("**", i + 2)
                    if (end != -1) {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(text.substring(i + 2, end))
                        }
                        i = end + 2
                    } else { append(text[i]); i++ }
                }
                // Cursiva: *texto* o _texto_
                (text[i] == '*' || text[i] == '_') && i + 1 < text.length -> {
                    val marker = text[i]
                    val end = text.indexOf(marker, i + 1)
                    if (end != -1 && end > i + 1) {
                        withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                            append(text.substring(i + 1, end))
                        }
                        i = end + 1
                    } else { append(text[i]); i++ }
                }
                // Tachado: ~~texto~~
                text.startsWith("~~", i) -> {
                    val end = text.indexOf("~~", i + 2)
                    if (end != -1) {
                        withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) {
                            append(text.substring(i + 2, end))
                        }
                        i = end + 2
                    } else { append(text[i]); i++ }
                }
                // Código inline: `texto`
                text[i] == '`' -> {
                    val end = text.indexOf('`', i + 1)
                    if (end != -1) {
                        withStyle(SpanStyle(
                            background = baseColor.copy(alpha = 0.1f),
                            color      = baseColor.copy(alpha = 0.85f),
                            fontStyle  = FontStyle.Normal
                        )) {
                            append(text.substring(i + 1, end))
                        }
                        i = end + 1
                    } else { append(text[i]); i++ }
                }
                else -> { append(text[i]); i++ }
            }
        }
    }
}
