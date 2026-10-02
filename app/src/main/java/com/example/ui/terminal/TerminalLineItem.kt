package com.example.ui.terminal

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.LineType
import com.example.model.TerminalFontSize
import com.example.model.TerminalLine
import com.example.ui.theme.LocalDreamByteColors

@Composable
fun TerminalLineItem(
    line: TerminalLine,
    fontSize: TerminalFontSize,
    showTimestamps: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = LocalDreamByteColors.current
    val clipboardManager = LocalClipboardManager.current
    val textSp = fontSize.spValue.sp

    SelectionContainer {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 1.dp),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.Top
        ) {
            if (showTimestamps) {
                Text(
                    text = "[${line.formattedTime}] ",
                    fontFamily = FontFamily.Monospace,
                    fontSize = (fontSize.spValue * 0.85f).sp,
                    color = colors.glassBorder.copy(alpha = 0.8f),
                    modifier = Modifier.padding(end = 4.dp)
                )
            }

            when (line.type) {
                LineType.PROMPT -> {
                    val promptText = buildAnnotatedString {
                        val prefix = line.promptPrefix ?: "jake@dreambyte:~$ "
                        val userPart = prefix.substringBefore("@")
                        val restPart = prefix.substringAfter("@")

                        withStyle(SpanStyle(color = colors.promptUser, fontWeight = FontWeight.Bold)) {
                            append(userPart)
                        }
                        withStyle(SpanStyle(color = colors.promptSymbol)) {
                            append("@")
                        }
                        withStyle(SpanStyle(color = colors.promptHost, fontWeight = FontWeight.SemiBold)) {
                            append(restPart)
                        }
                        withStyle(SpanStyle(color = colors.commandText)) {
                            append(line.text)
                        }
                    }
                    Text(
                        text = promptText,
                        fontFamily = FontFamily.Monospace,
                        fontSize = textSp,
                        lineHeight = (fontSize.spValue * 1.35f).sp
                    )
                }

                LineType.HEADER_BANNER -> {
                    Text(
                        text = line.text,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = (fontSize.spValue * 1.25f).sp,
                        color = colors.primaryElectric
                    )
                }

                LineType.SUCCESS -> {
                    Text(
                        text = line.text,
                        fontFamily = FontFamily.Monospace,
                        fontSize = textSp,
                        color = colors.successText
                    )
                }

                LineType.ERROR -> {
                    Text(
                        text = line.text,
                        fontFamily = FontFamily.Monospace,
                        fontSize = textSp,
                        color = colors.errorText
                    )
                }

                LineType.WARNING -> {
                    Text(
                        text = line.text,
                        fontFamily = FontFamily.Monospace,
                        fontSize = textSp,
                        color = colors.warningText
                    )
                }

                LineType.SYSTEM_NOTICE -> {
                    Text(
                        text = line.text,
                        fontFamily = FontFamily.Monospace,
                        fontSize = textSp,
                        color = colors.systemNoticeText
                    )
                }

                LineType.OUTPUT -> {
                    Text(
                        text = line.text,
                        fontFamily = FontFamily.Monospace,
                        fontSize = textSp,
                        color = colors.terminalText,
                        lineHeight = (fontSize.spValue * 1.35f).sp
                    )
                }
            }
        }
    }
}
