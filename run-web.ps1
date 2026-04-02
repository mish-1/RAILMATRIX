param(
    [int]$Port = 8080
)

Set-Location $PSScriptRoot

$jarPaths = (Get-ChildItem . -Recurse -File -Filter *.jar | ForEach-Object { $_.FullName }) -join ';'
if ([string]::IsNullOrWhiteSpace($jarPaths)) {
    $cp = '.'
} else {
    $cp = '.;' + $jarPaths
}

Write-Host "Compiling sources..."
javac --add-modules jdk.httpserver -cp $cp main\RailMatrixWebServer.java main\RailMatrixApp.java model\*.java service\*.java service\dao\*.java
if ($LASTEXITCODE -ne 0) {
    Write-Error "Compilation failed."
    exit $LASTEXITCODE
}

Write-Host "Starting server on http://localhost:$Port"
java --add-modules jdk.httpserver -cp $cp main.RailMatrixWebServer
