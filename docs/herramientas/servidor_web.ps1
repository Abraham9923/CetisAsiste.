# Abre la web de CetisAsiste (carpeta web\) en esta computadora, SIN instalar Node.js.
#
# Es un servidor web muy sencillo hecho con lo que ya trae Windows (PowerShell 5.1).
# Solo entrega los archivos de web\ tal cual; no guarda nada. La web habla directo con
# Firebase, así que usa la base real (o el emulador si la dirección trae ?emulador).
#
# Uso (PowerShell, desde la raíz del proyecto):
#   powershell -ExecutionPolicy Bypass -File docs\herramientas\servidor_web.ps1
#   y abre http://localhost:5000 en el navegador. Para apagarlo: Ctrl+C.
#   Agrega -Puerto 5050 para usar otro puerto.
#
# Vive fuera de web\ para que no se publique con Firebase Hosting.

param(
    [int]$Puerto = 5000
)

$ErrorActionPreference = 'Stop'

$carpeta = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..\..\web'))

# Tipo de cada archivo. Los .js deben ir como text/javascript porque son módulos
# (import/export): si no, el navegador se niega a cargarlos.
$tipos = @{
    '.html' = 'text/html; charset=utf-8'
    '.js'   = 'text/javascript; charset=utf-8'
    '.mjs'  = 'text/javascript; charset=utf-8'
    '.css'  = 'text/css; charset=utf-8'
    '.json' = 'application/json; charset=utf-8'
    '.png'  = 'image/png'
    '.jpg'  = 'image/jpeg'
    '.svg'  = 'image/svg+xml'
    '.ico'  = 'image/x-icon'
    '.ttf'  = 'font/ttf'
    '.woff2' = 'font/woff2'
}

$servidor = New-Object System.Net.HttpListener
$servidor.Prefixes.Add("http://localhost:$Puerto/")
$servidor.Start()
Write-Host "Web de CetisAsiste en http://localhost:$Puerto  (carpeta $carpeta)"
Write-Host 'Para apagarlo: Ctrl+C'

try {
    while ($servidor.IsListening) {
        # Se espera de medio en medio segundo para que Ctrl+C sí lo apague.
        $espera = $servidor.BeginGetContext($null, $null)
        while (-not $espera.AsyncWaitHandle.WaitOne(500)) { }
        $contexto = $servidor.EndGetContext($espera)
        $respuesta = $contexto.Response

        try {
            $ruta = [System.Uri]::UnescapeDataString($contexto.Request.Url.AbsolutePath)
            if ($ruta.EndsWith('/')) { $ruta += 'index.html' }
            $archivo = [System.IO.Path]::GetFullPath((Join-Path $carpeta $ruta.TrimStart('/')))

            # Nunca se entrega nada fuera de web\ (por ejemplo, con "..").
            $dentro = $archivo.StartsWith($carpeta + '\', [System.StringComparison]::OrdinalIgnoreCase)
            if ($dentro -and [System.IO.File]::Exists($archivo)) {
                $extension = [System.IO.Path]::GetExtension($archivo).ToLower()
                $tipo = $tipos[$extension]
                if (-not $tipo) { $tipo = 'application/octet-stream' }
                $bytes = [System.IO.File]::ReadAllBytes($archivo)
                $respuesta.StatusCode = 200
                $respuesta.ContentType = $tipo
                # Sin caché: al editar un archivo basta con recargar la página.
                $respuesta.Headers['Cache-Control'] = 'no-store'
                $codigo = 200
            } else {
                $bytes = [System.Text.Encoding]::UTF8.GetBytes('No existe ese archivo.')
                $respuesta.StatusCode = 404
                $respuesta.ContentType = 'text/plain; charset=utf-8'
                $codigo = 404
            }
            $respuesta.ContentLength64 = $bytes.Length
            # Una petición HEAD solo pide los encabezados, sin el archivo.
            if ($contexto.Request.HttpMethod -ne 'HEAD') {
                $respuesta.OutputStream.Write($bytes, 0, $bytes.Length)
            }
            Write-Host "$codigo $ruta"
        } catch {
            Write-Host "Error con $($contexto.Request.Url): $_"
            $respuesta.StatusCode = 500
        } finally {
            $respuesta.Close()
        }
    }
} finally {
    $servidor.Stop()
    $servidor.Close()
}
