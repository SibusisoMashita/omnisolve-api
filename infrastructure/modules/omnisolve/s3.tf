# S3 Bucket for Document Storage
# This bucket stores all document versions uploaded through the system.
# Each document version is stored with the path pattern:
# documents/{documentId}/v{versionNumber}/{filename}
#
# Security features enabled:
# - Versioning: Enabled (preserves all versions of objects)
# - Public Access: Blocked (all public access denied)
# - Encryption: AES256 server-side encryption
#
# The bucket name follows the pattern: {environment}-omnisolve-documents
# Examples: dev-omnisolve-documents, prod-omnisolve-documents
resource "aws_s3_bucket" "documents" {
  bucket = var.s3_bucket_name != "" ? var.s3_bucket_name : "${local.name_prefix}-documents"

  tags = merge(local.common_tags, {
    Name = var.s3_bucket_name != "" ? var.s3_bucket_name : "${local.name_prefix}-documents"
  })
}

# Enable versioning to preserve document history
resource "aws_s3_bucket_versioning" "documents" {
  bucket = aws_s3_bucket.documents.id

  versioning_configuration {
    status = "Enabled"
  }
}

# Block all public access for security
resource "aws_s3_bucket_public_access_block" "documents" {
  bucket = aws_s3_bucket.documents.id

  block_public_acls       = true
  ignore_public_acls      = true
  block_public_policy     = true
  restrict_public_buckets = true
}

# Enable server-side encryption
resource "aws_s3_bucket_server_side_encryption_configuration" "documents" {
  bucket = aws_s3_bucket.documents.id

  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
  }
}

