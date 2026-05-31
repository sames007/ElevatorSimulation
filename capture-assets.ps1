$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $MyInvocation.MyCommand.Path
Push-Location $root
try {
& (Join-Path $root "build.ps1")

$classes = Join-Path $root "out\classes"
$javaFxVersion = "24"
$openJfxRoot = Join-Path $env:USERPROFILE ".m2\repository\org\openjfx"
$javaFxModules = @("javafx-base", "javafx-graphics", "javafx-controls", "javafx-fxml")
$moduleJars = @()

foreach ($moduleName in $javaFxModules) {
    $jarName = "$moduleName-$javaFxVersion-win.jar"
    $moduleJars += Join-Path $openJfxRoot "$moduleName\$javaFxVersion\$jarName"
}

$modulePath = (@($classes) + $moduleJars) -join ";"
& java --enable-native-access=javafx.graphics --module-path $modulePath --module elevator.simulation/elevatorsim.tools.WalkthroughRecorder
if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
}

& python (Join-Path $root "tools\create_walkthrough_video.py")
if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
}
} finally {
    Pop-Location
}
