# Une la documentación en un solo Word: docs\DOCUMENTACION_COMPLETA.docx
#
# Toma los .md de docs\ (no los .docx), así que siempre sale con lo más nuevo. Tiene portada,
# índice, un capítulo por documento y números de página. Quedan aparte, en su propio Word:
# el manual técnico, los pendientes y Figma.
#
# Uso (PowerShell, desde la raíz del proyecto):
#   powershell -ExecutionPolicy Bypass -File docs\herramientas\unir_documentacion.ps1
#   Agrega -Pdf para generar también un .pdf (sirve para revisar cómo quedó).
#
# Si se agrega un documento nuevo, se suma a la lista $capitulos de abajo.

param([switch]$Pdf)

$ErrorActionPreference = 'Stop'
$generarPdf = $Pdf.IsPresent     # se guarda antes: cargar md_a_word.ps1 reinicia $Pdf
. (Join-Path $PSScriptRoot 'md_a_word.ps1')
$docs = Split-Path -Parent $PSScriptRoot
$salida = Join-Path $docs 'DOCUMENTACION_COMPLETA'

# ---------- Qué entra y en qué orden ----------
# Cada parte: archivo, título (vacío = el que trae), quitar (título de sección → texto que la
# reemplaza; '' = se quita completa) y sinCita (quita la cita > del principio, que habla del
# archivo .md y no aplica aquí).

$capitulos = @(
    @{ titulo = 'Estado del proyecto'
       resumen = 'Qué es la app, qué se le hizo, cómo está hoy, el mapa del código y cómo trabajar en ella'
       partes = @(@{ archivo = 'TRASPASO_AGENTE.md'
                     quitar = [ordered]@{ '9. Documentos de referencia' = '' } }) },
    @{ titulo = 'Pantallas por rol'
       resumen = 'El documento original de roles: pantallas de cada rol, alertas, estatus, preguntas abiertas y diagramas UML'
       partes = @(@{ archivo = 'referencias\PANTALLAS_POR_ROL.md'; sinCita = $true
                     intro = 'Copia del documento original **"CetisAsiste — Pantallas por rol"** (7 de octubre de 2026). El texto y las tablas están tal cual; solo la última sección, *Comparación con la app*, se agregó después.' }) },
    @{ titulo = 'Vistas de la app'
       resumen = 'Las vistas XML de cada rol y cómo se llenan con los datos de Firestore'
       intro = 'Las pantallas de la app son vistas XML de Android. Primero las del alumno, que fueron las primeras; después las de padre, prefecto, profesor y las que comparten varios roles.'
       partes = @(@{ archivo = 'VISTAS_ALUMNO.md'; titulo = 'Alumno'; clave = 'la sección *Alumno* del capítulo 3' },
                  @{ archivo = 'VISTAS_POR_ROL.md'; titulo = 'Padre, prefecto, profesor y vistas comunes'
                     clave = 'la sección *Padre, prefecto, profesor y vistas comunes* del capítulo 3' }) },
    @{ titulo = 'Base de datos'
       resumen = 'Cada colección, campo, regla e índice de Firestore, y las recetas para dar de alta datos'
       partes = @(@{ archivo = 'BASE_DE_DATOS.md' }) },
    @{ titulo = 'Decisiones de diseño'
       resumen = 'El porqué de cada decisión de las pantallas'
       partes = @(@{ archivo = 'DECISIONES.md' }) },
    @{ titulo = 'Registro de decisiones (ADR)'
       resumen = 'Las decisiones grandes, con sus opciones y consecuencias'
       intro = 'Un ADR (registro de decisión de arquitectura) explica una decisión importante: el problema, las opciones que había, cuál se eligió y qué consecuencias tiene. Las acciones que faltan de cada uno también están en el documento *Pendientes* (aparte).'
       partes = @(Get-ChildItem (Join-Path $docs 'adr') -Filter 'ADR-*.md' | Sort-Object Name |
                  ForEach-Object { @{ archivo = "adr\$($_.Name)" } }) },
    @{ titulo = 'Bitácora de cambios'
       resumen = 'Todo lo que se hizo, en orden, con archivos y cómo se comprobó'
       partes = @(@{ archivo = 'BITACORA.md'; sinCita = $true }) }
)

# Cómo se nombra, dentro del Word, un enlace a otro documento.
$script:etiquetas = @{
    MANUAL_TECNICO = 'el *Manual técnico* (documento aparte)'
    FIGMA          = 'el documento *Figma* (aparte)'
    PENDIENTES     = 'el documento *Pendientes* (aparte)'
}
for ($c = 0; $c -lt $capitulos.Count; $c++) {
    $cap = $capitulos[$c]
    $cap.numero = $c + 1
    $general = "el capítulo $($c + 1) (*$($cap.titulo)*)"
    foreach ($p in $cap.partes) {
        $nombre = [System.IO.Path]::GetFileNameWithoutExtension($p.archivo)
        if ($nombre -like 'ADR-*') { $nombre = 'ADR' }
        $script:etiquetas[$nombre] = if ($p.clave) { $p.clave } else { $general }
    }
}

# ---------- Ajustes a cada .md ----------

# Los enlaces a otros .md no sirven dentro del Word: se cambian por el capítulo donde quedó.
function CambiarEnlaces([string]$l) {
    $l = [regex]::Replace($l, '(?<!!)\[([^\]]+)\]\(([^)\s]+)\)', {
        param($m)
        $texto = $m.Groups[1].Value
        $destino = $m.Groups[2].Value
        if ($destino -match '^(https?|mailto):') { return $m.Value }
        if ($destino.StartsWith('#')) {
            if ($destino -match 'pendientes') { return $script:etiquetas.PENDIENTES }
            return $texto
        }
        $archivo = ($destino -split '#')[0].TrimEnd('/')
        $nombre = [System.IO.Path]::GetFileNameWithoutExtension($archivo)
        if ($nombre -like 'ADR-*' -or $nombre -eq 'adr') { $nombre = 'ADR' }
        # Solo cuando el texto es el nombre del archivo; "[ADR-006](...)" se queda como texto.
        if ($texto.Trim('`') -match '\.md$|/$' -and $script:etiquetas.ContainsKey($nombre)) {
            return $script:etiquetas[$nombre]
        }
        return $texto
    })
    return $l.Replace('de el capítulo', 'del capítulo').Replace('de el documento', 'del documento').Replace(' a el ', ' al ')
}

function Preparar($parte, [int]$desplazar, [string]$tituloCapitulo) {
    $ruta = Join-Path $docs $parte.archivo
    $carpeta = Split-Path $ruta
    $quitar = if ($parte.quitar) { $parte.quitar } else { @{} }
    $lineas = [System.IO.File]::ReadAllLines($ruta, [System.Text.Encoding]::UTF8)
    $res = New-Object System.Collections.Generic.List[string]
    $enCodigo = $false
    $saltando = 0          # nivel del título de la sección que se está quitando (0 = ninguna)
    $primero = $true
    $preambulo = $true     # antes del primer título ##

    foreach ($l in $lineas) {
        if ($l -match '^\s*```') {
            $enCodigo = -not $enCodigo
            if (-not $saltando) { $res.Add($l) }
            continue
        }
        if (-not $enCodigo -and $l -match '^(#{1,6})\s+(.*)$') {
            $n = $Matches[1].Length
            $texto = $Matches[2]
            if ($saltando -and $n -le $saltando) { $saltando = 0 }
            if ($saltando) { continue }
            $nuevo = [Math]::Min($n + $desplazar, 4)
            if ($primero) {
                $primero = $false
                $t = if ($tituloCapitulo) { $tituloCapitulo } elseif ($parte.titulo) { $parte.titulo } else { $texto }
                $res.Add(('#' * $nuevo) + ' ' + $t)
                if ($parte.intro) { $res.Add(''); $res.Add($parte.intro) }
                continue
            }
            $preambulo = $false
            $clave = @($quitar.Keys | Where-Object { $texto.StartsWith($_) })[0]
            if ($clave) {
                $saltando = $n
                if ($quitar[$clave]) { $res.Add(('#' * $nuevo) + ' ' + $texto); $res.Add(''); $res.Add($quitar[$clave]); $res.Add('') }
                continue
            }
            $res.Add(('#' * $nuevo) + ' ' + (CambiarEnlaces $texto))
            continue
        }
        if ($saltando) { continue }
        if (-not $enCodigo) {
            if ($parte.sinCita -and $preambulo -and $l -match '^\s*>') { continue }
            if ($l -match '^(\s*)!\[([^\]]*)\]\(([^)]+)\)\s*$') {
                $img = [System.IO.Path]::GetFullPath((Join-Path $carpeta $Matches[3]))
                $l = "$($Matches[1])![$($Matches[2])]($img)"
            } else {
                $l = CambiarEnlaces $l
            }
        }
        $res.Add($l)
    }
    $res.Add('')
    return , $res
}

# ---------- El documento unido, en Markdown ----------

$hoy = (Get-Date).ToString("d 'de' MMMM 'de' yyyy", [System.Globalization.CultureInfo]'es-MX')
$md = New-Object System.Collections.Generic.List[string]
$md.Add('# Cómo leer este documento')
$md.Add('')
$md.Add('Aquí está reunida en un solo lugar la documentación de **CetisAsiste**, la app del CETis 61 (Cd. Juárez) para controlar las entradas y salidas de los alumnos. Cada capítulo era antes un documento aparte; el contenido es el mismo, solo se ordenó y se cambiaron los enlaces entre documentos por el capítulo donde quedó cada uno.')
$md.Add('')
$md.Add('| Capítulo | Qué encuentras | Documento de origen |')
$md.Add('|---|---|---|')
foreach ($cap in $capitulos) {
    $nombres = @($cap.partes | ForEach-Object { [System.IO.Path]::GetFileNameWithoutExtension($_.archivo) })
    $origen = if ($nombres[0] -like 'ADR-*') { "$($nombres[0].Substring(0, 7)) a $($nombres[-1].Substring(0, 7))" } else { $nombres -join ', ' }
    $md.Add("| **$($cap.numero). $($cap.titulo)** | $($cap.resumen) | $origen |")
}
$md.Add('')
$md.Add('**Documentos aparte** (cada uno en su propio Word):')
$md.Add('')
$md.Add('- **Manual técnico:** dónde está cada cosa del código y cómo cambiarla.')
$md.Add('- **Pendientes:** todo lo que falta, junto y sin repetir, con quién tiene que actuar.')
$md.Add('- **Figma:** los enlaces de cada página del diseño.')
$md.Add('')

foreach ($cap in $capitulos) {
    $titulo = "$($cap.numero). $($cap.titulo)"
    if ($cap.partes.Count -eq 1) {
        $md.AddRange((Preparar $cap.partes[0] 0 $titulo))
    } else {
        $md.Add("# $titulo")
        $md.Add('')
        if ($cap.intro) { $md.Add($cap.intro); $md.Add('') }
        foreach ($p in $cap.partes) { $md.AddRange((Preparar $p 1 $null)) }
    }
}

# ---------- Portada e índice ----------

$estilo += @'
<style>
  p.portada-sup { font-size: 11pt; color: #7B4331; margin: 150pt 0 6pt 0; letter-spacing: 1pt; }
  p.portada-titulo { font-size: 40pt; font-weight: bold; color: #5A1E24; margin: 0; }
  p.portada-sub { font-size: 20pt; color: #461419; margin: 0 0 18pt 0; }
  p.portada-texto { font-size: 12pt; color: #1f1a1a; margin: 0 0 60pt 0; }
  p.portada-dato { font-size: 10.5pt; color: #6b5a58; margin: 0 0 3pt 0; }
  p.indice-titulo { font-size: 20pt; color: #5A1E24; margin: 0 0 12pt 0; }
</style>
'@

$portada = @"
<p class="portada-sup">CETis 61 · Cd. Juárez</p>
<p class="portada-titulo">CetisAsiste</p>
<p class="portada-sub">Documentación del proyecto</p>
<p class="portada-texto">App para controlar las entradas y salidas de los alumnos</p>
<p class="portada-dato"><b>Actualizada:</b> $hoy</p>
<p class="portada-dato"><b>Documentos aparte:</b> Manual técnico · Pendientes · Figma</p>
<br clear="all" style="page-break-before:always">
<p class="indice-titulo">Índice</p>
<p>@@INDICE@@</p>
"@

# ---------- Word ----------

$word = New-Object -ComObject Word.Application
$word.Visible = $false
$word.DisplayAlerts = 0
try {
    $cuerpo = $portada + (ConvertirMarkdown $md.ToArray() $docs)
    $abierto = AbrirHtml $word 'CetisAsiste — Documentación del proyecto' $cuerpo
    $doc = $abierto.doc
    $doc.ActiveWindow.View.Type = 3                                  # vista Diseño de impresión

    # Cada capítulo empieza en hoja nueva.
    $doc.Styles.Item(-2).ParagraphFormat.PageBreakBefore = $true    # -2 = Título 1

    # Encabezado y número de página (no en la portada).
    $gris = 0x58 * 65536 + 0x5A * 256 + 0x6B                         # #6B5A58 (Word usa BGR)
    $doc.PageSetup.DifferentFirstPageHeaderFooter = -1
    $enc = $doc.Sections.Item(1).Headers.Item(1).Range
    $enc.Text = 'CetisAsiste — Documentación del proyecto'
    $enc.Font.Size = 8.5
    $enc.Font.Color = $gris
    $enc.ParagraphFormat.Alignment = 2                               # derecha
    [void]$doc.Sections.Item(1).Footers.Item(1).PageNumbers.Add(1, $false)   # centrado

    # Índice con capítulos y secciones.
    $r = $doc.Content
    if ($r.Find.Execute('@@INDICE@@')) {
        $r.Text = ''
        [void]$doc.TablesOfContents.Add($r, $true, 1, 2)
        $doc.TablesOfContents.Item(1).Update()
    }

    $doc.SaveAs2("$salida.docx", 16)
    if ($generarPdf) { $doc.SaveAs2("$salida.pdf", 17) }
    $doc.Close($false)
    Remove-Item $abierto.temporal
    Write-Host "OK  $salida.docx"
} finally {
    $word.Quit()
    [void][System.Runtime.InteropServices.Marshal]::ReleaseComObject($word)
}
