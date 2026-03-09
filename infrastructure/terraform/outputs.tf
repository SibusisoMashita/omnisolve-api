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

