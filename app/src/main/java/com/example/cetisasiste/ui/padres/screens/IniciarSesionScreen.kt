package com.example.cetisasiste.ui.padres.screens

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.cetisasiste.R
import com.example.cetisasiste.ui.padres.components.AppIcon
import com.example.cetisasiste.ui.padres.components.PrimaryButton
import com.example.cetisasiste.ui.padres.components.SchoolLogo
import com.example.cetisasiste.ui.theme.Brand800
import com.example.cetisasiste.ui.theme.Brand900
import com.example.cetisasiste.ui.theme.BrandAccent
import com.example.cetisasiste.ui.theme.CetisAsisteTheme
import com.example.cetisasiste.ui.theme.RadiusMd
import com.example.cetisasiste.ui.theme.Space12
import com.example.cetisasiste.ui.theme.Space16
import com.example.cetisasiste.ui.theme.SurfaceBackground
import com.example.cetisasiste.ui.theme.SurfacePill
import com.example.cetisasiste.ui.theme.TextOnBrand
import com.example.cetisasiste.ui.theme.TextOnBrandMuted
import com.example.cetisasiste.ui.theme.TextSecondary

/**
 * Pantalla de inicio de sesión (frame "Iniciar sesion" de Figma).
 * Sirve para todos los roles: el alumno escribe su No. de control; el personal y los padres,
 * el usuario que les dio Control Escolar. La pantalla no valida nada: entrega lo escrito a
 * [onLoginClick] y muestra [cargando] y [error] según le diga quien la usa (CetisApp).
 */
@Composable
fun IniciarSesionScreen(
    onLoginClick: (usuario: String, contrasena: String) -> Unit,
    modifier: Modifier = Modifier,
    cargando: Boolean = false,
    error: String? = null
) {
    var noControl by rememberSaveable { mutableStateOf("") }
    // La contraseña no se guarda en el estado que Android conserva al girar la pantalla.
    var password by remember { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SurfaceBackground)
            .imePadding()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column {
            LoginHero()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 32.dp, end = 32.dp, top = 28.dp),
                verticalArrangement = Arrangement.spacedBy(Space16)
            ) {
                LoginField(
                    label = stringResource(R.string.login_usuario_label),
                    value = noControl,
                    onValueChange = { noControl = it },
                    placeholder = stringResource(R.string.login_no_control_placeholder),
                    leadingIcon = R.drawable.ic_user_field
                )
                LoginField(
                    label = stringResource(R.string.password_label),
                    value = password,
                    onValueChange = { password = it },
                    placeholder = stringResource(R.string.login_password_placeholder),
                    leadingIcon = R.drawable.ic_lock_field,
                    keyboardType = KeyboardType.Password,
                    visualTransformation = if (passwordVisible) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    trailingIcon = {
                        Image(
                            painter = painterResource(R.drawable.ic_eye_field),
                            contentDescription = stringResource(R.string.login_toggle_password),
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .clickable { passwordVisible = !passwordVisible }
                        )
                    }
                )
                Text(
                    text = stringResource(R.string.login_forgot_password),
                    style = MaterialTheme.typography.labelMedium,
                    color = BrandAccent,
                    textDecoration = TextDecoration.Underline,
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth()
                )
                if (error != null) {
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                PrimaryButton(
                    text = stringResource(if (cargando) R.string.login_cargando else R.string.login_submit),
                    onClick = { if (!cargando) onLoginClick(noControl, password) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        SchoolLogo(
            width = 140.dp,
            modifier = Modifier
                .navigationBarsPadding()
                .padding(top = 24.dp, bottom = Space12)
        )
    }
}

@Composable
private fun LoginHero(modifier: Modifier = Modifier) {
    // La hora y los íconos de batería/señal los dibuja Android; aquí solo se les deja su espacio.
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
            .background(Brand800)
    ) {
        Box(modifier = Modifier.matchParentSize()) {
            Image(
                painter = painterResource(R.drawable.bg_login_accent),
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .wrapContentHeight(align = Alignment.Top, unbounded = true)
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = Space16, end = Space16, bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Spacer(modifier = Modifier.height(maxOf(statusBarHeight, 28.dp)))
            Spacer(modifier = Modifier.height(28.dp))
            AppIcon(
                icon = R.drawable.ic_log_in_brand_36,
                size = 72.dp,
                cornerRadius = 22.dp
            )
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineLarge,
                color = TextOnBrand
            )
            Text(
                text = stringResource(R.string.login_hero_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = TextOnBrandMuted
            )
        }
    }
}

// Campo de texto con etiqueta arriba e ícono a la izquierda (componente "Campo" de Figma).
@Composable
private fun LoginField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    @DrawableRes leadingIcon: Int,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: (@Composable () -> Unit)? = null
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = Brand900
        )
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = Brand900),
            cursorBrush = SolidColor(Brand800),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            visualTransformation = visualTransformation,
            decorationBox = { innerTextField ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .clip(RoundedCornerShape(RadiusMd))
                        .background(TextOnBrand)
                        .border(1.5.dp, SurfacePill, RoundedCornerShape(RadiusMd))
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Space12)
                ) {
                    Image(
                        painter = painterResource(leadingIcon),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Box(modifier = Modifier.weight(1f)) {
                        if (value.isEmpty()) {
                            Text(
                                text = placeholder,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                        innerTextField()
                    }
                    trailingIcon?.invoke()
                }
            }
        )
    }
}

@Preview(widthDp = 360, heightDp = 800)
@Composable
private fun IniciarSesionScreenPreview() {
    CetisAsisteTheme {
        IniciarSesionScreen(onLoginClick = { _, _ -> })
    }
}
