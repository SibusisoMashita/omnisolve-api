# OmniSolve Elastic Beanstalk Deployment Script (PowerShell)
# Usage: .\scripts\deploy-to-beanstalk.ps1 -Environment dev|prod [-VersionLabel "v1.0.0"]

param(
    [Parameter(Mandatory=$true)]
    [ValidateSet("dev", "prod")]
    [string]$Environment,
    
    [Parameter(Mandatory=$false)]
    [string]$VersionLabel = (Get-Date -Format "yyyyMMdd-HHmmss")
)

$ErrorActionPreference = "Stop"

Write-Host "=== OmniSolve Deployment ===" -ForegroundColor Green
Write-Host "Environment: $Environment"
Write-Host "Version: $VersionLabel"
Write-Host ""

# Get Terraform outputs
Write-Host "Getting infrastructure details..." -ForegroundColor Yellow
Push-Location "infrastructure\$Environment"

try {
    $AppName = terraform output -raw beanstalk_application_name 2>$null
    $EnvName = terraform output -raw beanstalk_environment_name 2>$null
    $DeployBucket = terraform output -raw deployments_bucket_name 2>$null
    
    if (-not $AppName -or -not $EnvName -or -not $DeployBucket) {
        throw "Could not get Terraform outputs. Has infrastructure been deployed?"
    }
    
    Write-Host "Application: $AppName"
    Write-Host "Environment: $EnvName"
    Write-Host "Bucket: $DeployBucket"
    Write-Host ""
}
finally {
    Pop-Location
}

# Build application
Write-Host "Building application..." -ForegroundColor Yellow
& .\mvnw.cmd clean package -DskipTests

if (-not (Test-Path "target\omnisolve-api.jar")) {
    throw "JAR file not found after build"
}

Write-Host "✓ Build successful" -ForegroundColor Green
Write-Host ""

# Upload to S3
Write-Host "Uploading to S3..." -ForegroundColor Yellow
$JarKey = "omnisolve-api-$VersionLabel.jar"
aws s3 cp target\omnisolve-api.jar "s3://$DeployBucket/$JarKey"

Write-Host "✓ Upload successful" -ForegroundColor Green
Write-Host ""

# Create application version
Write-Host "Creating application version..." -ForegroundColor Yellow
aws elasticbeanstalk create-application-version `
    --application-name $AppName `
    --version-label $VersionLabel `
    --source-bundle S3Bucket="$DeployBucket",S3Key="$JarKey" `
    --description "Deployed via script on $(Get-Date)" `
    --no-auto-create-application

Write-Host "✓ Application version created" -ForegroundColor Green
Write-Host ""

# Deploy to environment
Write-Host "Deploying to environment..." -ForegroundColor Yellow
aws elasticbeanstalk update-environment `
    --application-name $AppName `
    --environment-name $EnvName `
    --version-label $VersionLabel

Write-Host "✓ Deployment initiated" -ForegroundColor Green
Write-Host ""

# Wait for deployment
Write-Host "Waiting for deployment to complete..." -ForegroundColor Yellow
Write-Host "This may take several minutes..."

aws elasticbeanstalk wait environment-updated `
    --environment-names $EnvName `
    --no-cli-pager

Write-Host "✓ Deployment complete" -ForegroundColor Green
Write-Host ""

# Get environment URL
$EnvUrl = aws elasticbeanstalk describe-environments `
    --environment-names $EnvName `
    --query 'Environments[0].CNAME' `
    --output text

Write-Host "=== Deployment Successful ===" -ForegroundColor Green
Write-Host "Environment URL: http://$EnvUrl"
Write-Host "Health Check: http://$EnvUrl/api/health"
Write-Host ""

# Test health endpoint
Write-Host "Testing health endpoint..." -ForegroundColor Yellow
Start-Sleep -Seconds 10  # Wait for application to start

try {
    $Response = Invoke-WebRequest -Uri "http://$EnvUrl/api/health" -UseBasicParsing -TimeoutSec 30
    $HttpCode = $Response.StatusCode
}
catch {
    $HttpCode = 0
}

if ($HttpCode -eq 200) {
    Write-Host "✓ Health check passed!" -ForegroundColor Green
    Write-Host ""
    Write-Host "🚀 Deployment completed successfully!" -ForegroundColor Green
}
else {
    Write-Host "⚠ Health check returned: $HttpCode" -ForegroundColor Yellow
    Write-Host "The application may still be starting up."
    Write-Host "Check the environment status in AWS Console or run:"
    Write-Host "  aws elasticbeanstalk describe-environments --environment-names $EnvName"
}

Write-Host ""
Write-Host "To view logs:"
Write-Host "  aws elasticbeanstalk retrieve-environment-info --environment-name $EnvName --info-type tail"
