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

variable "public_subnet_ids" {
  type        = list(string)
  description = "Public subnet IDs for Elastic Beanstalk instances"
}

variable "aws_region" {
  type        = string
  description = "AWS region for deployment"
  default     = "us-east-1"
}

variable "ssl_certificate_arn" {
  type        = string
  description = "ARN of SSL certificate in ACM for HTTPS"
  default     = ""
}

variable "enable_https" {
  type        = bool
  description = "Enable HTTPS with load balancer"
  default     = false
}

variable "enable_cloudfront" {
  type        = bool
  description = "Enable CloudFront for HTTPS (no custom domain needed)"
  default     = false
}

