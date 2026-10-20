package com.example.cetisasiste.ui.padres.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import com.example.cetisasiste.R

/** Logotipo del CETis 61. Solo se indica el ancho; el alto sale de la proporción del diseño. */
@Composable
fun SchoolLogo(
    width: Dp,
    modifier: Modifier = Modifier
) {
    // El PNG trae margen arriba y abajo; el diseño lo recorta a esta proporción (150 x 51.9).
    Image(
        painter = painterResource(R.drawable.logo_cetis61),
        contentDescription = stringResource(R.string.school_logo_description),
        contentScale = ContentScale.Crop,
        modifier = modifier
            .width(width)
            .aspectRatio(150f / 51.892f)
    )
}
