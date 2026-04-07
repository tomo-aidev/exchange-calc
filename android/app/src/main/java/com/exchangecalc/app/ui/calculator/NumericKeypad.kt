package com.exchangecalc.app.ui.calculator

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.exchangecalc.app.R
import com.exchangecalc.app.ui.theme.KeypadDark
import com.exchangecalc.app.ui.theme.KeypadDisabled
import com.exchangecalc.app.ui.theme.KeypadGray
import com.exchangecalc.app.ui.theme.Primary

enum class KeypadButtonStyle { DIGIT, FUNCTION, PRIMARY, DISABLED }

@Composable
fun NumericKeypad(
    modifier: Modifier = Modifier,
    onDigit: (String) -> Unit,
    onDoubleZero: () -> Unit,
    onDecimalPoint: () -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit,
    onSave: () -> Unit,
    decimalEnabled: Boolean
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        // Row 1: ⌫, C, .
        Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            KeypadButton(modifier = Modifier.weight(1f), style = KeypadButtonStyle.FUNCTION, onClick = onBackspace) {
                Icon(Icons.AutoMirrored.Filled.Backspace, contentDescription = "Delete", tint = Color.Black)
            }
            KeypadButton(modifier = Modifier.weight(1f), label = "C", style = KeypadButtonStyle.FUNCTION, onClick = onClear)
            KeypadButton(
                modifier = Modifier.weight(1f),
                label = ".",
                style = if (decimalEnabled) KeypadButtonStyle.FUNCTION else KeypadButtonStyle.DISABLED,
                onClick = onDecimalPoint
            )
        }

        // Row 2-4: digits
        for (row in listOf(listOf("7","8","9"), listOf("4","5","6"), listOf("1","2","3"))) {
            Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                for (digit in row) {
                    KeypadButton(modifier = Modifier.weight(1f), label = digit, style = KeypadButtonStyle.DIGIT, onClick = { onDigit(digit) })
                }
            }
        }

        // Row 5: Save, 0, 00
        Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            KeypadButton(modifier = Modifier.weight(1f), style = KeypadButtonStyle.PRIMARY, onClick = onSave) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Backspace, // placeholder
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(stringResource(R.string.save), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
            KeypadButton(modifier = Modifier.weight(1f), label = "0", style = KeypadButtonStyle.DIGIT, onClick = { onDigit("0") })
            KeypadButton(modifier = Modifier.weight(1f), label = "00", style = KeypadButtonStyle.FUNCTION, onClick = onDoubleZero)
        }
    }
}

@Composable
fun KeypadButton(
    modifier: Modifier = Modifier,
    label: String? = null,
    style: KeypadButtonStyle,
    onClick: () -> Unit,
    content: @Composable (() -> Unit)? = null
) {
    val bgColor = when (style) {
        KeypadButtonStyle.DIGIT -> KeypadDark
        KeypadButtonStyle.FUNCTION -> KeypadGray
        KeypadButtonStyle.PRIMARY -> Primary
        KeypadButtonStyle.DISABLED -> KeypadDisabled
    }
    val fgColor = when (style) {
        KeypadButtonStyle.DIGIT -> Color.White
        KeypadButtonStyle.FUNCTION -> Color.Black
        KeypadButtonStyle.PRIMARY -> Color.White
        KeypadButtonStyle.DISABLED -> Color.Gray
    }

    Button(
        onClick = onClick,
        modifier = modifier.fillMaxHeight(),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(containerColor = bgColor),
        contentPadding = PaddingValues(0.dp),
        enabled = style != KeypadButtonStyle.DISABLED
    ) {
        if (content != null) {
            content()
        } else if (label != null) {
            Text(label, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = fgColor)
        }
    }
}
