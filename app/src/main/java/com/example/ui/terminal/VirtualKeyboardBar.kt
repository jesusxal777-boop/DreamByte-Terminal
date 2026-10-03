package com.example.ui.terminal

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LocalDreamByteColors

@Composable
fun VirtualKeyboardBar(
    onKeyPress: (String) -> Unit,
    onHistoryUp: () -> Unit,
    onHistoryDown: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalDreamByteColors.current
    val scrollState = rememberScrollState()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(vertical = 4.dp, horizontal = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AccessoryKey(label = "TAB", onClick = { onKeyPress("TAB") })
        AccessoryKey(label = "ESC", onClick = { onKeyPress("ESC") })
        AccessoryKey(label = "↑", onClick = onHistoryUp)
        AccessoryKey(label = "↓", onClick = onHistoryDown)
        AccessoryKey(label = "CLEAR", onClick = { onKeyPress("CLEAR") })
        AccessoryKey(label = "|", onClick = { onKeyPress(" | ") })
        AccessoryKey(label = ">", onClick = { onKeyPress(" > ") })
        AccessoryKey(label = ">>", onClick = { onKeyPress(" >> ") })
        AccessoryKey(label = "&&", onClick = { onKeyPress(" && ") })
        AccessoryKey(label = "pkg", onClick = { onKeyPress("pkg ") })
        AccessoryKey(label = "wget", onClick = { onKeyPress("wget ") })
        AccessoryKey(label = "python", onClick = { onKeyPress("python ") })
        AccessoryKey(label = "files", onClick = { onKeyPress("files ") })
        AccessoryKey(label = "help", onClick = { onKeyPress("help ") })
        AccessoryKey(label = "/", onClick = { onKeyPress("/") })
        AccessoryKey(label = "-", onClick = { onKeyPress("-") })
    }
}

@Composable
private fun AccessoryKey(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalDreamByteColors.current

    Box(
        modifier = modifier
            .heightIn(min = 36.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(colors.accessoryKeyBackground)
            .border(0.75.dp, colors.glassBorder, RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .testTag("key_$label"),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = colors.accessoryKeyContent
        )
    }
}
