$raiz = Split-Path -Parent $MyInvocation.MyCommand.Path
$arquivo = Join-Path $raiz 'dados/processos.json'
if (-not (Test-Path $arquivo)) { Write-Host 'Nenhum processo registrado.'; exit }
foreach ($item in @(Get-Content $arquivo -Raw | ConvertFrom-Json)) {
    $processo = Get-CimInstance Win32_Process -Filter "ProcessId=$($item.id)" -ErrorAction SilentlyContinue
    if ($processo -and $processo.CommandLine -like "*$($item.jar)*") {
        Stop-Process -Id $item.id
        Write-Host "$($item.nome) encerrado"
    }
}
Remove-Item $arquivo
