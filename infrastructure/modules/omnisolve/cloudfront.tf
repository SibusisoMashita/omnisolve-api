# CloudFront distribution for HTTPS without custom domain
# This is an alternative to using Application Load Balancer
# Provides HTTPS via CloudFront's certificate
#
# IMPORTANT: CORS Configuration
# ------------------------------
# CloudFront MUST forward the following headers to the backend:
# - Origin: Required for backend to identify the requesting domain
# - Access-Control-Request-Headers: Required for CORS preflight
# - Access-Control-Request-Method: Required for CORS preflight
#
# Without these headers, the backend cannot return proper CORS headers,
# and browsers will block API requests from the frontend.
#
# The backend (Spring Boot) handles CORS via CorsConfig.java which allows:
# - Origins: https://omnisolve.africa, https://d3s7bt9q3x42ay.cloudfront.net
# - Methods: GET, POST, PUT, DELETE, OPTIONS, PATCH
# - Headers: All (*)
# - Credentials: Enabled
#
# If CORS errors occur after deployment:
# 1. Verify CloudFront forwards Origin header
# 2. Create cache invalidation: aws cloudfront create-invalidation --distribution-id <ID> --paths "/*"
# 3. Check backend CORS config in src/main/java/com/omnisolve/config/CorsConfig.java

resource "aws_cloudfront_distribution" "api" {
  count   = var.enable_cloudfront ? 1 : 0
  enabled = true
  comment = "${local.name_prefix} API CloudFront Distribution"

  origin {
    domain_name = aws_elastic_beanstalk_environment.api.cname
    origin_id   = "beanstalk"

    custom_origin_config {
      http_port              = 80
      https_port             = 443
      origin_protocol_policy = "http-only"
      origin_ssl_protocols   = ["TLSv1.2"]
    }
  }

  default_cache_behavior {
    allowed_methods        = ["DELETE", "GET", "HEAD", "OPTIONS", "PATCH", "POST", "PUT"]
    cached_methods         = ["GET", "HEAD", "OPTIONS"]
    target_origin_id       = "beanstalk"
    viewer_protocol_policy = "redirect-to-https"

    forwarded_values {
      query_string = true
      # Forward CORS headers to backend so it can return proper CORS responses
      headers = [
        "Origin",
        "Access-Control-Request-Headers",
        "Access-Control-Request-Method",
        "Authorization",
        "Accept",
        "Content-Type"
      ]

      cookies {
        forward = "all"
      }
    }

    # Disable caching for API responses to ensure fresh data
    # and proper CORS header handling
    min_ttl     = 0
    default_ttl = 0
    max_ttl     = 0
  }

  restrictions {
    geo_restriction {
      restriction_type = "none"
    }
  }

  viewer_certificate {
    cloudfront_default_certificate = true
  }

  tags = merge(local.common_tags, {
    Name = "${local.name_prefix}-api-cdn"
  })
}
