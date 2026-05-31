$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$classes = Join-Path $root "out\classes"
$sourceList = Join-Path $root "out\sources.txt"
$javaFxVersion = "24"
$openJfxRoot = Join-Path $env:USERPROFILE ".m2\repository\org\openjfx"
$javaFxModules = @("javafx-base", "javafx-graphics", "javafx-controls", "javafx-fxml")
$moduleJars = @()

foreach ($moduleName in $javaFxModules) {
    $jarName = "$moduleName-$javaFxVersion-win.jar"
    $jarPath = Join-Path $openJfxRoot "$moduleName\$javaFxVersion\$jarName"
    if (-not (Test-Path $jarPath)) {
        throw "Missing JavaFX module jar: $jarPath"
    }
    $moduleJars += $jarPath
}

$modulePath = ($moduleJars -join ";")

if (Test-Path $classes) {
    $resolvedClasses = (Resolve-Path $classes).Path
    $resolvedRoot = (Resolve-Path $root).Path
    if (-not $resolvedClasses.StartsWith($resolvedRoot)) {
        throw "Refusing to clean classes outside workspace: $resolvedClasses"
    }
    Remove-Item -LiteralPath $classes -Recurse -Force
}

New-Item -ItemType Directory -Path $classes -Force | Out-Null
New-Item -ItemType Directory -Path (Split-Path -Parent $sourceList) -Force | Out-Null

Get-ChildItem -Path (Join-Path $root "src\main\java") -Recurse -Filter "*.java" |
    Sort-Object FullName |
    ForEach-Object { $_.FullName } |
    Set-Content -Path $sourceList -Encoding ASCII

& javac --module-path $modulePath -d $classes "@$sourceList"

$resources = Join-Path $root "src\main\resources"
if (Test-Path $resources) {
    Copy-Item -Path (Join-Path $resources "*") -Destination $classes -Recurse -Force
}

Write-Host "Build completed: $classes"
