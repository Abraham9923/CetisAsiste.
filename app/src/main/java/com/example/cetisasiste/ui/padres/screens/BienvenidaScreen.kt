package com.example.cetisasiste.ui.padres.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.cetisasiste.R
import com.example.cetisasiste.ui.padres.components.AppIcon
import com.example.cetisasiste.ui.theme.Brand800
import com.example.cetisasiste.ui.theme.CetisAsisteTheme
import com.example.cetisasiste.ui.theme.Space16
import com.example.cetisasiste.ui.theme.TextOnBrand
import com.example.cetisasiste.ui.theme.TextOnBrandMuted

/** Pantalla de bienvenida que se ve al abrir la app (frame "Bienvenida - CetisAsiste" de Figma). */
@Composable
fun BienvenidaScreen(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brand800)
    ) {
        Image(
            painter = painterResource(R.drawable.bg_splash_accent),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier.fillMaxSize()
        )
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Space16)
        ) {
            AppIcon(
                icon = R.drawable.ic_log_in_brand_48,
                size = 96.dp,
                cornerRadius = 28.dp
            )
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineLarge,
                color = TextOnBrand
            )
            Text(
                text = stringResource(R.string.splash_tagline),
                style = MaterialTheme.typography.bodyMedium,
                color = TextOnBrandMuted
            )
        }
        Text(
            text = stringResource(R.string.school_name),
            style = MaterialTheme.typography.labelMedium,
            color = TextOnBrandMuted,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = Space16)
        )
    }
}

@Preview(widthDp = 360, heightDp = 800)
@Composable
private fun BienvenidaScreenPreview() {
    CetisAsisteTheme {
        BienvenidaScreen()
    }
}
