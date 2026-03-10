# Script to request an ACM certificate for your domain
# Usage: .\create-acm-certificate.ps1 -Domain "api.yourdomain.com"

param(
    [Parameter(Mandatory=$true)]
    [string]$Domain,
    
    [string]$Region = "us-east-1"
)

Write-Host "Requesting ACM certificate for domain: $Domain" -ForegroundColor Green
Write-Host "Region: $Region" -ForegroundColor Green
Write-Host ""

try {
    # Request certificate
    $certArn = aws acm request-certificate `
        --domain-name $Domain `
        --validation-method DNS `
        --region $Region `
        --query 'CertificateArn' `
        --output text

    if ($LASTEXITCODE -ne 0) {
        throw "Failed to request certificate"
    }

    Write-Host "Certificate requested successfully!" -ForegroundColor Green
    Write-Host "Certificate ARN: $certArn" -ForegroundColor Yellow
    Write-Host ""

    # Wait for validation records
    Write-Host "Waiting for validation records..." -ForegroundColor Cyan
    Start-Sleep -Seconds 5

    # Get validation records
    Write-Host ""
    Write-Host "DNS Validation Records:" -ForegroundColor Green
    Write-Host "=======================" -ForegroundColor Green
    
    aws acm describe-certificate `
        --certificate-arn $certArn `
        --region $Region `
        --query 'Certificate.DomainValidationOptions[0].ResourceRecord' `
        --output table

    Write-Host ""
    Write-Host "Next steps:" -ForegroundColor Cyan
    Write-Host "1. Add the CNAME record shown above to your DNS provider"
    Write-Host "2. Wait for validation (5-30 minutes)"
    Write-Host "3. Check status with: aws acm describe-certificate --certificate-arn $certArn --region $Region"
    Write-Host "4. Once validated, add this ARN to your terraform.tfvars:"
    Write-Host "   ssl_certificate_arn = `"$certArn`"" -ForegroundColor Yellow

} catch {
    Write-Host "Error: $_" -ForegroundColor Red
    exit 1
}
