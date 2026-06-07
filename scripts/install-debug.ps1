$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
$javaHome = "C:\Program Files\Android\Android Studio\jbr"

if (Test-Path $javaHome) {
    $env:JAVA_HOME = $javaHome
    $env:PATH = "$javaHome\bin;$env:PATH"
}

Push-Location $repoRoot
try {
    .\gradlew.bat installDebug
} finally {
    Pop-Location
}
