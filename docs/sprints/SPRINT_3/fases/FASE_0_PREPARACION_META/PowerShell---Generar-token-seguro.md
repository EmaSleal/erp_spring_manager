  # PowerShell - Generar token seguro
  $token = [Convert]::ToBase64String([System.Text.Encoding]::UTF8.GetBytes([Guid]::NewGuid().ToString()))
  Write-Host "Tu Webhook Verify Token: $token"
  
