package com.example.ui.terminal

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.TerminalFontSize
import com.example.model.TerminalSettings
import com.example.ui.theme.LocalDreamByteColors

@Composable
fun TerminalInputRow(
    input: String,
    onInputChange: (String) -> Unit,
    onSubmit: () -> Unit,
    settings: TerminalSettings,
    isExecuting: Boolean,
    promptPrefix: String = settings.promptPrefix,
    modifier: Modifier = Modifier
) {
    val colors = LocalDreamByteColors.current
    val fontSize = settings.fontSize.spValue.sp

    // Blinking cursor
    val infiniteTransition = rememberInfiniteTransition(label = "cursor_blink")
    val cursorAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(550),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cursor_alpha"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(colors.glassSurface)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Prompt
        val userPart = promptPrefix.substringBefore("@")
        val restPart = promptPrefix.substringAfter("@")

        val annotatedPrompt = buildAnnotatedString {
            withStyle(SpanStyle(color = colors.promptUser, fontWeight = FontWeight.Bold)) {
                append(userPart)
            }
            withStyle(SpanStyle(color = colors.promptSymbol)) {
                append("@")
            }
            withStyle(SpanStyle(color = colors.promptHost, fontWeight = FontWeight.SemiBold)) {
                append(restPart)
            }
        }

        Text(
            text = annotatedPrompt,
            fontFamily = FontFamily.Monospace,
            fontSize = fontSize,
            modifier = Modifier.padding(end = 4.dp)
        )

        // Text field
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 4.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            BasicTextField(
                value = input,
                onValueChange = onInputChange,
                textStyle = TextStyle(
                    color = colors.commandText,
                    fontFamily = FontFamily.Monospace,
                    fontSize = fontSize
                ),
                cursorBrush = SolidColor(colors.cursorColor),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(onGo = { onSubmit() }),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("terminal_input_field")
            )

            // Custom blinking block cursor if input is empty
            if (input.isEmpty()) {
                Text(
                    text = "█",
                    color = colors.cursorColor,
                    fontSize = fontSize,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.alpha(cursorAlpha)
                )
            }
        }

        // Action button
        if (isExecuting) {
            CircularProgressIndicator(
                modifier = Modifier
                    .size(24.dp)
                    .padding(2.dp),
                color = colors.primaryElectric,
                strokeWidth = 2.dp
            )
        } else {
            IconButton(
                onClick = onSubmit,
                modifier = Modifier
                    .size(36.dp)
                    .testTag("submit_command_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Execute Command",
                    tint = colors.primaryElectric,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
