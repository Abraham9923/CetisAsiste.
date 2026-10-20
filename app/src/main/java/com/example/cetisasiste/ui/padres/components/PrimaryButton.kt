package com.example.cetisasiste.ui.padres.components

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.cetisasiste.ui.theme.Brand800
import com.example.cetisasiste.ui.theme.RadiusMd
import com.example.cetisasiste.ui.theme.TextOnBrand

/** Botón principal guinda (componente "PrimaryButton" de Figma). */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(52.dp),
        shape = RoundedCornerShape(RadiusMd),
        colors = ButtonDefaults.buttonColors(
            containerColor = Brand800,
            contentColor = TextOnBrand
        )
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleSmall
        )
    }
}
