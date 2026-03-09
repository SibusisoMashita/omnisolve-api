terraform {
  required_version = ">= 1.6.0"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }

  backend "s3" {
    bucket         = "omnisolve-terraform-state"
    key            = "prod/terraform.tfstate"
    region         = "us-east-1"
    dynamodb_table = "omnisolve-terraform-lock"
    encrypt        = true
  }
}

provider "aws" {
  region = "us-east-1"
}

module "omnisolve" {
  source = "../modules/omnisolve"

  project_name         = "omnisolve"
  environment          = "prod"
  vpc_id               = var.vpc_id
  private_subnet_ids   = var.private_subnet_ids
  allowed_cidr_blocks  = var.allowed_cidr_blocks
  db_name              = "omnisolve"
  db_username          = "omnisolve"
  db_password          = var.db_password
  db_instance_class    = "db.t3.micro"
  db_allocated_storage = 20
  s3_bucket_name       = var.s3_bucket_name
}

