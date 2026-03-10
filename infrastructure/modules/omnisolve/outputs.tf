output "postgres_endpoint" {
  value       = aws_db_instance.postgres.address
  description = "RDS PostgreSQL endpoint"
}

output "postgres_port" {
  value       = aws_db_instance.postgres.port
  description = "RDS PostgreSQL port"
}

output "postgres_jdbc_url" {
  value       = "jdbc:postgresql://${aws_db_instance.postgres.address}:${aws_db_instance.postgres.port}/${var.db_name}"
  description = "JDBC connection string for OmniSolve API"
}

output "documents_bucket_name" {
  value       = aws_s3_bucket.documents.bucket
  description = "S3 bucket for document storage"
}

output "postgres_security_group_id" {
  value       = aws_security_group.postgres.id
  description = "Security group ID attached to PostgreSQL"
}

output "beanstalk_application_name" {
  value       = aws_elastic_beanstalk_application.api.name
  description = "Elastic Beanstalk application name"
}

output "beanstalk_environment_name" {
  value       = aws_elastic_beanstalk_environment.api.name
  description = "Elastic Beanstalk environment name"
}

output "beanstalk_environment_url" {
  value       = var.enable_cloudfront && length(aws_cloudfront_distribution.api) > 0 ? "https://${aws_cloudfront_distribution.api[0].domain_name}" : (var.enable_https ? "https://${aws_elastic_beanstalk_environment.api.cname}" : "http://${aws_elastic_beanstalk_environment.api.cname}")
  description = "Elastic Beanstalk environment URL (with CloudFront or direct)"
}

output "cloudfront_domain_name" {
  value       = var.enable_cloudfront && length(aws_cloudfront_distribution.api) > 0 ? aws_cloudfront_distribution.api[0].domain_name : null
  description = "CloudFront distribution domain name (if enabled)"
}

output "beanstalk_cname" {
  value       = aws_elastic_beanstalk_environment.api.cname
  description = "Elastic Beanstalk CNAME"
}

output "deployments_bucket_name" {
  value       = aws_s3_bucket.deployments.bucket
  description = "S3 bucket for Beanstalk deployment artifacts"
}

