package com.pntapps.ekchhoan.ui.common

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.layout.layout
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp

/** Elder-facing text never goes below this (spec §3). */
private val MinSize = 15.sp

/**
 * A label that wraps between words but never splits a word: while a line break falls inside a word, the font
 * steps down one sp (not below 15sp) and lays out again; the text is drawn only once it fits, so there is no
 * flicker. "Messages" next to a big icon stays whole instead of breaking into "Message / s".
 *
 * Built on plain Text (not BoxWithConstraints) so it works inside rows that ask for intrinsic heights.
 */
@Composable
fun FitText(
    text: String,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
) {
    // Width available to the label; when it changes (columns, icon size), fitting starts again from full size.
    var maxWidth by remember { mutableIntStateOf(-1) }
    var size by remember(text, style.fontSize, maxWidth) { mutableStateOf(style.fontSize) }
    var ready by remember(text, style.fontSize, maxWidth) { mutableStateOf(false) }
    Text(
        text = text,
        style = style.copy(fontSize = size),
        color = color,
        textAlign = textAlign,
        modifier = modifier
            .layout { measurable, constraints ->
                if (constraints.maxWidth != maxWidth) maxWidth = constraints.maxWidth
                val placeable = measurable.measure(constraints)
                layout(placeable.width, placeable.height) { placeable.place(0, 0) }
            }
            .drawWithContent { if (ready) drawContent() },
        onTextLayout = { layout ->
            if (splitsAWord(layout, text) && size > MinSize) size = (size.value - 1f).sp else ready = true
        },
    )
}

/** True when some line ends in the middle of a word (a soft wrap with letters on both sides). */
private fun splitsAWord(layout: TextLayoutResult, text: String): Boolean =
    (0 until layout.lineCount - 1).any { line ->
        val end = layout.getLineEnd(line)
        end in 1 until text.length && !text[end - 1].isWhitespace() && !text[end].isWhitespace()
    }
