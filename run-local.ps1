$ErrorActionPreference = 'Stop'

Set-Location -Path $PSScriptRoot

Write-Host 'Starting backend with local Spring profile...' -ForegroundColor Cyan
& .\mvnw.cmd '-Dspring-boot.run.profiles=local' 'spring-boot:run'
