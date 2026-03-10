variable "project_name" {
  type        = string
  description = "Project slug"
}

variable "environment" {
  type        = string
  description = "Deployment environment"
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
}

variable "db_name" {
  type        = string
  description = "Database name"
}

variable "db_username" {
  type        = string
  description = "Database username"
}

variable "db_password" {
  type        = string
  description = "Database password"
  sensitive   = true
}

variable "db_instance_class" {
  type        = string
  description = "RDS instance class"
}

variable "db_allocated_storage" {
  type        = number
  description = "RDS storage in GB"
}

variable "s3_bucket_name" {
  type        = string
  description = "S3 bucket name for document storage; leave empty to auto-generate from project/environment"
}

variable "public_subnet_ids" {
  type        = list(string)
  description = "Public subnet IDs for Elastic Beanstalk instances"
}

variable "aws_region" {
  type        = string
  description = "AWS region for deployment"
  default     = "us-east-1"
}

variable "beanstalk_solution_stack" {
  type        = string
  description = "Elastic Beanstalk solution stack name"
  default     = "64bit Amazon Linux 2023 v4.9.0 running Corretto 21"
}

variable "beanstalk_instance_type" {
  type        = string
  description = "EC2 instance type for Elastic Beanstalk"
  default     = "t3.micro"
}

variable "ssl_certificate_arn" {
  type        = string
  description = "ARN of SSL certificate in ACM for HTTPS (optional - if not provided, only HTTP will be enabled)"
  default     = ""
}

variable "enable_https" {
  type        = bool
  description = "Enable HTTPS with load balancer (requires ssl_certificate_arn)"
  default     = false
}

variable "enable_cloudfront" {
  type        = bool
  description = "Enable CloudFront for HTTPS (alternative to load balancer, no custom domain needed)"
  default     = false
}

