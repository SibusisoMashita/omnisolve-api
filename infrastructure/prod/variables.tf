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
}

variable "db_password" {
  type        = string
  description = "Database password"
  sensitive   = true
}

variable "s3_bucket_name" {
  type        = string
  description = "S3 bucket name for document storage"
}

