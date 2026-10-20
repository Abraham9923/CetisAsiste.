# Convierte la documentación en Markdown (.md) a Word (.docx).
#
# Usa el Microsoft Word instalado en la computadora (no necesita internet ni otros
# programas). Cada archivo X.md genera X.docx en la misma carpeta, reemplazando el anterior.
#
# Uso (PowerShell, desde la raíz del proyecto):
#   powershell -ExecutionPolicy Bypass -File docs\herramientas\md_a_word.ps1
#       → convierte todos los .md de docs\ y docs\adr\
#   powershell -ExecutionPolicy Bypass -File docs\herramientas\md_a_word.ps1 docs\BITACORA.md
#       → convierte solo ese archivo
#   Agrega -Pdf para generar también un .pdf (sirve para revisar cómo quedó).
#
# Entiende lo que usan estos documentos: títulos (#), párrafos, listas (- y 1.),
# tablas (|), bloques de código (```), citas (>), líneas (---), **negritas**,
# *cursivas*, `código`, [enlaces](url) e imágenes solas en su renglón (![pie](ruta)).
#
# unir_documentacion.ps1 carga este archivo con ". md_a_word.ps1" para usar sus funciones;
# en ese caso no convierte nada.

param(
    [string[]]$Archivos,
    [switch]$Pdf
)

$ErrorActionPreference = 'Stop'

# ---------- Markdown → HTML ----------

function Escapar([string]$t) {
    return $t.Replace('&', '&amp;').Replace('<', '&lt;').Replace('>', '&gt;')
}

function EnLinea([string]$t) {
    # El código entre comillas invertidas se aparta primero para no tocar lo que tiene adentro.
    $codigos = New-Object System.Collections.Generic.List[string]
    $t = [regex]::Replace($t, '`([^`]+)`', {
        param($m)
        $codigos.Add($m.Groups[1].Value)
        return "@@C$($codigos.Count - 1)@@"
    })
    $t = Escapar $t
    $t = [regex]::Replace($t, '&lt;(https?://[^\s&]+)&gt;', '<a href="$1">$1</a>')
    $t = $t.Replace('[x]', '&#9745;').Replace('[ ]', '&#9744;')
    $t = [regex]::Replace($t, '\[([^\]]+)\]\(([^)]+)\)', '<a href="$2">$1</a>')
    $t = [regex]::Replace($t, '\*\*([^*]+)\*\*', '<b>$1</b>')
    $t = [regex]::Replace($t, '(?<![\w*])\*([^*\s][^*]*)\*(?![\w*])', '<i>$1</i>')
    for ($i = 0; $i -lt $codigos.Count; $i++) {
        $t = $t.Replace("@@C$i@@", "<code>$(Escapar $codigos[$i])</code>")
    }
    return $t
}

# $carpeta es desde donde se buscan las imágenes con ruta relativa (la carpeta del .md).
function ConvertirMarkdown([string[]]$lineas, [string]$carpeta = (Get-Location).Path) {
    $html = New-Object System.Text.StringBuilder
    $parrafo = New-Object System.Collections.Generic.List[string]
    $listas = New-Object System.Collections.Generic.List[object]   # pila: @{tipo; sangria}
    # Texto del elemento de lista actual: se junta con sus renglones de continuación antes de
    # darle formato, para que una **negrita** partida en dos renglones salga bien.
    $item = New-Object System.Collections.Generic.List[string]
    $i = 0

    function SoltarItem {
        if ($item.Count -gt 0) {
            [void]$html.Append((EnLinea ($item -join ' ')))
            $item.Clear()
        }
    }

    function CerrarParrafo {
        if ($parrafo.Count -gt 0) {
            [void]$html.Append("<p>$(EnLinea ($parrafo -join ' '))</p>`n")
            $parrafo.Clear()
        }
    }
    function CerrarListas([int]$hasta) {
        SoltarItem
        while ($listas.Count -gt $hasta) {
            $l = $listas[$listas.Count - 1]
            [void]$html.Append("</li></$($l.tipo)>`n")
            $listas.RemoveAt($listas.Count - 1)
        }
    }

    while ($i -lt $lineas.Count) {
        $linea = $lineas[$i]

        # Bloque de código
        if ($linea -match '^\s*```') {
            CerrarParrafo; CerrarListas 0
            $codigo = New-Object System.Collections.Generic.List[string]
            $i++
            while ($i -lt $lineas.Count -and $lineas[$i] -notmatch '^\s*```') { $codigo.Add($lineas[$i]); $i++ }
            # Word separa los renglones de <pre> con mucho espacio; un párrafo con saltos (<br>) queda compacto.
            $renglones = $codigo | ForEach-Object { (Escapar $_).Replace(' ', '&nbsp;') }
            [void]$html.Append("<p class=`"codigo`">$($renglones -join '<br>')</p>`n")
            $i++; continue
        }

        # Tabla
        if ($linea -match '^\s*\|' -and $i + 1 -lt $lineas.Count -and $lineas[$i + 1] -match '^\s*\|[\s:|-]+\|\s*$') {
            CerrarParrafo; CerrarListas 0
            $celdas = { param($l) ($l.Trim().Trim('|') -split '(?<!\\)\|') | ForEach-Object { $_.Trim() } }
            [void]$html.Append("<table><tr>")
            foreach ($c in (& $celdas $linea)) { [void]$html.Append("<th><p class=`"celda`">$(EnLinea $c)</p></th>") }
            [void]$html.Append("</tr>`n")
            $i += 2
            while ($i -lt $lineas.Count -and $lineas[$i] -match '^\s*\|') {
                [void]$html.Append("<tr>")
                foreach ($c in (& $celdas $lineas[$i])) { [void]$html.Append("<td><p class=`"celda`">$(EnLinea $c)</p></td>") }
                [void]$html.Append("</tr>`n")
                $i++
            }
            [void]$html.Append("</table>`n")
            continue
        }

        # Título
        if ($linea -match '^(#{1,4})\s+(.*)$') {
            CerrarParrafo; CerrarListas 0
            $n = $Matches[1].Length
            [void]$html.Append("<h$n>$(EnLinea $Matches[2])</h$n>`n")
            $i++; continue
        }

        # Imagen sola en su renglón; el texto entre corchetes va debajo como pie.
        if ($linea -match '^\s*!\[([^\]]*)\]\(([^)]+)\)\s*$') {
            CerrarParrafo; CerrarListas 0
            $pie = $Matches[1]
            $ruta = $Matches[2]
            if (-not [System.IO.Path]::IsPathRooted($ruta)) { $ruta = Join-Path $carpeta $ruta }
            $src = ([System.Uri][System.IO.Path]::GetFullPath($ruta)).AbsoluteUri
            [void]$html.Append("<p class=`"figura`"><img src=`"$src`"></p>`n")
            if ($pie) { [void]$html.Append("<p class=`"pie`">$(EnLinea $pie)</p>`n") }
            $i++; continue
        }

        # Línea horizontal
        if ($linea -match '^\s*---+\s*$') {
            CerrarParrafo; CerrarListas 0
            [void]$html.Append("<hr/>`n")
            $i++; continue
        }

        # Cita
        if ($linea -match '^\s*>\s?(.*)$') {
            CerrarParrafo; CerrarListas 0
            $cita = New-Object System.Collections.Generic.List[string]
            while ($i -lt $lineas.Count -and $lineas[$i] -match '^\s*>\s?(.*)$') { $cita.Add($Matches[1]); $i++ }
            [void]$html.Append("<blockquote>$(EnLinea ($cita -join ' '))</blockquote>`n")
            continue
        }

        # Elemento de lista
        if ($linea -match '^(\s*)([-*]|\d+\.)\s+(.*)$') {
            CerrarParrafo
            SoltarItem
            # Se copian los grupos antes de usar otro -match (que reemplazaría $Matches).
            $sangria = $Matches[1].Length
            $vineta = $Matches[2]
            $texto = $Matches[3]
            $tipo = if ($vineta -match '\d') { 'ol' } else { 'ul' }
            # Una lista numerada que empieza en 4 (por ejemplo, después de un bloque de código) sigue en 4.
            $abrir = if ($tipo -eq 'ol') { "<ol start=`"$($vineta.TrimEnd('.'))`"><li>" } else { '<ul><li>' }
            while ($listas.Count -gt 0 -and $listas[$listas.Count - 1].sangria -gt $sangria) { CerrarListas ($listas.Count - 1) }
            if ($listas.Count -gt 0 -and $listas[$listas.Count - 1].sangria -eq $sangria) {
                if ($listas[$listas.Count - 1].tipo -ne $tipo) {
                    CerrarListas ($listas.Count - 1)
                    [void]$html.Append($abrir)
                    $listas.Add(@{ tipo = $tipo; sangria = $sangria })
                } else {
                    [void]$html.Append("</li><li>")
                }
            } else {
                [void]$html.Append($abrir)
                $listas.Add(@{ tipo = $tipo; sangria = $sangria })
            }
            $item.Add($texto)
            $i++; continue
        }

        # Línea vacía
        if ($linea.Trim() -eq '') {
            CerrarParrafo
            # Una línea vacía cierra las listas, salvo que siga otro elemento o una continuación.
            $sig = if ($i + 1 -lt $lineas.Count) { $lineas[$i + 1] } else { '' }
            if ($listas.Count -gt 0 -and $sig -notmatch '^\s+\S' -and $sig -notmatch '^([-*]|\d+\.)\s') { CerrarListas 0 }
            $i++; continue
        }

        # Continuación de un elemento de lista (línea con sangría)
        if ($listas.Count -gt 0 -and $linea -match '^\s+\S') {
            $item.Add($linea.Trim())
            $i++; continue
        }

        # Texto normal: se junta con las líneas siguientes en un párrafo, salvo los renglones
        # de datos como "**Fecha:** …", que van cada uno en su propio renglón.
        CerrarListas 0
        if ($linea -match '^\*\*[^*]+:\*\*') { CerrarParrafo }
        $parrafo.Add($linea.Trim())
        $i++
    }
    CerrarParrafo; CerrarListas 0
    return $html.ToString()
}

$estilo = @'
<style>
  body { font-family: Calibri, sans-serif; font-size: 11pt; color: #1f1a1a; }
  h1 { font-size: 20pt; color: #5A1E24; margin: 0 0 10pt 0; }
  h2 { font-size: 15pt; color: #5A1E24; margin: 16pt 0 6pt 0; border-bottom: 1px solid #E4D5D2; }
  h3 { font-size: 12.5pt; color: #461419; margin: 12pt 0 4pt 0; }
  h4 { font-size: 11.5pt; color: #461419; margin: 10pt 0 4pt 0; }
  p, li { margin: 0 0 6pt 0; line-height: 1.3; }
  p.celda { margin: 0; line-height: 1.15; }
  table { border-collapse: collapse; margin: 4pt 0 10pt 0; width: 100%; }
  th { background: #EDE5E1; color: #461419; text-align: left; }
  th, td { border: 1px solid #D9C9C5; padding: 3pt 5pt; font-size: 9.5pt; vertical-align: top; }
  code { font-family: Consolas, monospace; font-size: 9pt; color: #5A1E24; }
  p.codigo { font-family: Consolas, monospace; font-size: 8.5pt; background: #F7F5F4; line-height: 1.15;
        border: 1px solid #E4D5D2; padding: 6pt; margin: 4pt 0 10pt 0; }
  blockquote { margin: 6pt 0 10pt 0; padding: 4pt 8pt; background: #F7F5F4;
               border-left: 3px solid #7B4331; }
  a { color: #7B4331; }
  hr { border: 0; border-top: 1px solid #E4D5D2; }
  p.figura { text-align: center; margin: 8pt 0 2pt 0; }
  p.pie { text-align: center; font-size: 9pt; font-style: italic; color: #6b5a58; margin: 0 0 12pt 0; }
</style>
'@

# ---------- HTML → Word ----------

# Abre en Word un HTML ya armado y le pone hoja carta y márgenes.
function AbrirHtml($word, [string]$titulo, [string]$cuerpo) {
    $html = "<html><head><meta charset=`"utf-8`"><title>$titulo</title>$estilo</head><body>$cuerpo</body></html>"
    $temporal = Join-Path ([System.IO.Path]::GetTempPath()) "$titulo-$([guid]::NewGuid()).html"
    [System.IO.File]::WriteAllText($temporal, $html, (New-Object System.Text.UTF8Encoding($true)))
    $doc = $word.Documents.Open($temporal, $false, $true)
    $doc.PageSetup.PageWidth = 612      # Carta: 21.59 × 27.94 cm
    $doc.PageSetup.PageHeight = 792
    foreach ($m in 'TopMargin', 'BottomMargin', 'LeftMargin', 'RightMargin') { $doc.PageSetup.$m = 64 }
    # Un título nunca se queda solo al final de la hoja (-2 a -5 = Título 1 a 4).
    foreach ($e in -2, -3, -4, -5) { $doc.Styles.Item($e).ParagraphFormat.KeepWithNext = $true }
    IncrustarImagenes $doc
    return @{ doc = $doc; temporal = $temporal }
}

# Word deja las imágenes de un HTML como vínculos al archivo; se guardan dentro del .docx
# y las que no caben se achican al ancho de la hoja.
function IncrustarImagenes($doc) {
    $ancho = $doc.PageSetup.PageWidth - $doc.PageSetup.LeftMargin - $doc.PageSetup.RightMargin
    for ($k = 1; $k -le $doc.InlineShapes.Count; $k++) {
        $s = $doc.InlineShapes.Item($k)
        if ($s.LinkFormat) {
            $s.LinkFormat.SavePictureWithDocument = $true
            $s.LinkFormat.BreakLink()
            $s = $doc.InlineShapes.Item($k)
        }
        if ($s.Width -gt $ancho) {
            $s.LockAspectRatio = -1
            $s.Width = $ancho
        }
    }
}

if ($MyInvocation.InvocationName -eq '.') { return }

$docs = Split-Path -Parent $PSScriptRoot
if (-not $Archivos) {
    $Archivos = @(Get-ChildItem -Path $docs, (Join-Path $docs 'adr') -Filter *.md | ForEach-Object FullName)
}

$word = New-Object -ComObject Word.Application
$word.Visible = $false
$word.DisplayAlerts = 0
try {
    foreach ($archivo in $Archivos) {
        $md = (Resolve-Path $archivo).Path
        $base = [System.IO.Path]::ChangeExtension($md, $null).TrimEnd('.')
        $titulo = [System.IO.Path]::GetFileNameWithoutExtension($md)
        $cuerpo = ConvertirMarkdown ([System.IO.File]::ReadAllLines($md, [System.Text.Encoding]::UTF8)) (Split-Path $md)
        $abierto = AbrirHtml $word $titulo $cuerpo
        $doc = $abierto.doc
        $doc.SaveAs2("$base.docx", 16)      # 16 = documento de Word
        if ($Pdf) { $doc.SaveAs2("$base.pdf", 17) }   # 17 = PDF
        $doc.Close($false)
        Remove-Item $abierto.temporal
        Write-Host "OK  $base.docx"
    }
} finally {
    $word.Quit()
    [void][System.Runtime.InteropServices.Marshal]::ReleaseComObject($word)
}
