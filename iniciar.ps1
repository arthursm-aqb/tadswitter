$ErrorActionPreference = 'Stop'
$raiz = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $raiz

$java = if ($env:JAVA_HOME -and (Test-Path (Join-Path $env:JAVA_HOME 'bin/java.exe'))) {
    Join-Path $env:JAVA_HOME 'bin/java.exe'
} else {
    (Get-Command java -ErrorAction Stop).Source
}

$apps = @(
    @{ nome = 'usuarios'; jar = 'usuarios-api/target/usuarios-api-1.0.0.jar' },
    @{ nome = 'board'; jar = 'board-api/target/board-api-1.0.0.jar' },
    @{ nome = 'gateway'; jar = 'gateway/target/gateway-1.0.0.jar' }
)
foreach ($app in $apps) {
    if (-not (Test-Path $app.jar)) { throw "Arquivo $($app.jar) ausente. Rode .\mvnw.cmd -DskipTests package primeiro." }
}
New-Item -ItemType Directory -Force (Join-Path $raiz 'dados') | Out-Null

$processos = foreach ($app in $apps) {
    $p = Start-Process -FilePath $java -ArgumentList '-jar', $app.jar `
        -WorkingDirectory $raiz -WindowStyle Hidden -PassThru `
        -RedirectStandardOutput "dados/$($app.nome).log" `
        -RedirectStandardError "dados/$($app.nome)-erro.log"
    [pscustomobject]@{ nome = $app.nome; id = $p.Id; jar = $app.jar }
}
$processos | ConvertTo-Json | Set-Content (Join-Path $raiz 'dados/processos.json')
Write-Host 'Serviços iniciados. Aguarde cerca de 15 segundos e abra http://localhost:8080'
Write-Host 'Logs: pasta dados/ | Encerrar: .\parar.ps1'
