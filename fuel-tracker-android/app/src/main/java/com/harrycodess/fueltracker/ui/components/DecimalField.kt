package com.harrycodess.fueltracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.harrycodess.fueltracker.domain.isValidDecimalInput

/**
 * Outlined numeric field with a tonal leading icon and a unit label on the right.
 * When [onUnitClick] is set, the unit becomes a button (used to toggle km/L <-> L/100km).
 */
@Composable
fun DecimalField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    unit: String,
    modifier: Modifier = Modifier,
    imeAction: ImeAction = ImeAction.Next,
    onImeDone: () -> Unit = {},
    onUnitClick: (() -> Unit)? = null,
    isError: Boolean = false,
    supportingText: String? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { if (isValidDecimalInput(it)) onValueChange(it) },
        modifier = modifier.fillMaxWidth(),
        label = { Text(label) },
        leadingIcon = {
            Box(
                modifier = Modifier
                    .padding(start = 8.dp, end = 4.dp)
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
            }
        },
        trailingIcon = {
            if (onUnitClick != null) {
                TextButton(onClick = onUnitClick) {
                    Text(unit)
                    Icon(
                        Icons.Rounded.SwapHoriz,
                        contentDescription = "Switch unit",
                        modifier = Modifier
                            .padding(start = 4.dp)
                            .size(18.dp),
                    )
                }
            } else {
                Text(
                    text = unit,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(end = 16.dp),
                )
            }
        },
        isError = isError,
        supportingText = supportingText?.let { { Text(it) } },
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
        ),
        textStyle = MaterialTheme.typography.titleMedium,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = imeAction),
        keyboardActions = KeyboardActions(onDone = { onImeDone() }),
    )
}
