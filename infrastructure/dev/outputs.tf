output "postgres_endpoint" {
  value       = module.omnisolve.postgres_endpoint
  description = "RDS PostgreSQL endpoint"
}

output "postgres_port" {
  value       = module.omnisolve.postgres_port
  description = "RDS PostgreSQL port"
}

output "postgres_jdbc_url" {
  value       = module.omnisolve.postgres_jdbc_url
  description = "JDBC connection string for OmniSolve API"
}

output "documents_bucket_name" {
  value       = module.omnisolve.documents_bucket_name
  description = "S3 bucket for document storage"
}

output "postgres_security_group_id" {
  value       = module.omnisolve.postgres_security_group_id
  description = "Security group ID attached to PostgreSQL"
}

output "beanstalk_application_name" {
  value       = module.omnisolve.beanstalk_application_name
  description = "Elastic Beanstalk application name"
}

output "beanstalk_environment_name" {
  value       = module.omnisolve.beanstalk_environment_name
  description = "Elastic Beanstalk environment name"
}

output "beanstalk_environment_url" {
  value       = module.omnisolve.beanstalk_environment_url
  description = "Elastic Beanstalk environment URL"
}

output "beanstalk_cname" {
  value       = module.omnisolve.beanstalk_cname
  description = "Elastic Beanstalk CNAME"
}

output "deployments_bucket_name" {
  value       = module.omnisolve.deployments_bucket_name
  description = "S3 bucket for Beanstalk deployment artifacts"
}

