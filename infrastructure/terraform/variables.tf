variable "project_name" {
  type        = string
  description = "Project slug"
  default     = "omnisolve"
}

variable "environment" {
  type        = string
  description = "Deployment environment"
  default     = "dev"
}

variable "aws_region" {
  type        = string
  description = "AWS region"
  default     = "us-east-1"
}

variable "vpc_id" {
  type        = string
  description = "VPC ID where RDS will be deployed"
}

variable "private_subnet_ids" {
  type        = list(string)
  description = "Private subnet IDs for RDS subnet group"
}

variable "allowed_cidr_blocks" {
  type        = list(string)
  description = "CIDR blocks allowed to connect to PostgreSQL"
  default     = ["10.0.0.0/16"]
}

variable "db_name" {
  type        = string
  description = "Database name"
  default     = "omnisolve"
}

variable "db_username" {
  type        = string
  description = "Database username"
  default     = "omnisolve"
}

variable "db_password" {
  type        = string
  description = "Database password"
  sensitive   = true
}

variable "db_instance_class" {
  type        = string
  description = "RDS instance class"
  default     = "db.t3.micro"
}

variable "db_allocated_storage" {
  type        = number
  description = "RDS storage in GB"
  default     = 20
}

variable "s3_bucket_name" {
  type        = string
  description = "S3 bucket name for document storage; leave empty to auto-generate from project/environment"
  default     = ""
}
