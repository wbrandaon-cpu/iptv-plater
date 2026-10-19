package com.example.iptvplayer.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * Borda verde no item focado. Essencial na TV Box, onde se navega pelo
 * controle remoto (D-pad) e é preciso ver qual item está selecionado.
 */
@Composable
fun Modifier.focusHighlight(shape: Shape = RoundedCornerShape(10.dp)): Modifier {
    var focused by remember { mutableStateOf(false) }
    val color = MaterialTheme.colorScheme.primary
    return this
        .onFocusChanged { focused = it.hasFocus }
        .border(
            width = if (focused) 3.dp else 0.dp,
            color = if (focused) color else Color.Transparent,
            shape = shape
        )
}
