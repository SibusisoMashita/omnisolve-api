# Infrastructure Migration Guide

## Problem

The original setup used a single Terraform root with environment-specific `.tfvars` files. This caused state conflicts where applying one environment (e.g., prod) would try to destroy resources from another environment (e.g., dev).

## Solution

Separate Terraform roots for each environment, each with its own state file, using a shared module for common infrastructure code.

## New Structure

```
infrastructure/
├── modules/
│   └── omnisolve/              # Shared module with all resource definitions
│       ├── main.tf
│       ├── postgres.tf
│       ├── s3.tf
│       ├── variables.tf
│       └── outputs.tf
├── dev/                        # Dev environment root
│   ├── main.tf                 # Uses module with environment="dev"
│   ├── variables.tf
│   ├── outputs.tf
│   └── terraform.tfvars        # Dev-specific values
├── prod/                       # Prod environment root
│   ├── main.tf                 # Uses module with environment="prod"
│   ├── variables.tf
│   ├── outputs.tf
│   └── terraform.tfvars.example
└── terraform/                  # OLD - to be removed after migration
```

## State File Separation

- **Dev state**: `s3://omnisolve-terraform-state/dev/terraform.tfstate`
- **Prod state**: `s3://omnisolve-terraform-state/prod/terraform.tfstate`

Each environment manages its own state independently.

## Migration Steps

### 1. Import Existing Dev Resources

The existing dev infrastructure is managed by the old state file at `infra/terraform.tfstate`. We need to import it into the new dev state.

```powershell
cd C:\forge\omnisolve-api\infrastructure\dev

# Initialize new dev environment
terraform init

# Import existing resources (one by one)
terraform import module.omnisolve.aws_s3_bucket.documents omnisolve-documents-dev-861870144419
terraform import module.omnisolve.aws_s3_bucket_versioning.documents omnisolve-documents-dev-861870144419
terraform import module.omnisolve.aws_s3_bucket_public_access_block.documents omnisolve-documents-dev-861870144419
terraform import module.omnisolve.aws_s3_bucket_server_side_encryption_configuration.documents omnisolve-documents-dev-861870144419
terraform import module.omnisolve.aws_db_subnet_group.postgres dev-omnisolve-postgres-subnets
terraform import module.omnisolve.aws_security_group.postgres sg-07645d93532139819
terraform import module.omnisolve.aws_db_instance.postgres dev-omnisolve-postgres

# Verify no changes needed
terraform plan
```

### 2. Create Prod Environment (Fresh)

```powershell
cd C:\forge\omnisolve-api\infrastructure\prod

# Create prod tfvars from example
cp terraform.tfvars.example terraform.tfvars
# Edit terraform.tfvars with prod password

# Initialize prod environment
terraform init

# Create prod infrastructure
terraform plan
terraform apply
```

### 3. Clean Up Old Structure

Once migration is complete:

```powershell
# Remove old terraform directory
Remove-Item -Recurse -Force C:\forge\omnisolve-api\infrastructure\terraform

# Remove old environments directory (values are now in dev/prod roots)
Remove-Item -Recurse -Force C:\forge\omnisolve-api\infrastructure\environments
```

## Updated Commands

### Dev Environment

```powershell
cd C:\forge\omnisolve-api\infrastructure\dev

terraform init
terraform plan
terraform apply
terraform destroy
```

### Prod Environment

```powershell
cd C:\forge\omnisolve-api\infrastructure\prod

terraform init
terraform plan
terraform apply
terraform destroy
```

## Benefits

✅ **No state conflicts**: Each environment has its own state file
✅ **DRY code**: Shared module avoids duplication
✅ **Safe operations**: Can't accidentally destroy dev when working with prod
✅ **Clear separation**: Directory structure makes environment boundaries explicit
✅ **Easy to extend**: Add staging, qa, etc. by creating new environment roots

## Next Steps

1. Follow migration steps above to import dev resources
2. Create prod infrastructure using new structure
3. Update CI/CD pipelines to use new paths
4. Clean up old terraform directory

