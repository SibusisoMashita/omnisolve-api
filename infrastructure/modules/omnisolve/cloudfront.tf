# CloudFront distribution for HTTPS without custom domain
# This is an alternative to using Application Load Balancer
# Provides HTTPS via CloudFront's certificate
#
# IMPORTANT: Auth + CORS forwarding
# -------------------------------
# CloudFront MUST forward the viewer Authorization header to the backend
# for Cognito JWT bearer authentication, along with the CORS preflight headers.
#
# The managed policies below keep API caching disabled and forward all viewer
# headers, query strings, and cookies to the origin.

data "aws_cloudfront_cache_policy" "caching_disabled" {
  name = "Managed-CachingDisabled"
}

data "aws_cloudfront_origin_request_policy" "all_viewer" {
  name = "Managed-AllViewer"
}

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
    allowed_methods          = ["DELETE", "GET", "HEAD", "OPTIONS", "PATCH", "POST", "PUT"]
    cached_methods           = ["GET", "HEAD", "OPTIONS"]
    target_origin_id         = "beanstalk"
    viewer_protocol_policy   = "redirect-to-https"
    cache_policy_id          = data.aws_cloudfront_cache_policy.caching_disabled.id
    origin_request_policy_id = data.aws_cloudfront_origin_request_policy.all_viewer.id
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
