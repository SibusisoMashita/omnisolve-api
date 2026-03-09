project_name = "omnisolve"
environment  = "dev"
aws_region   = "us-east-1"

vpc_id              = "vpc-075b3102672a9af26"
private_subnet_ids  = ["subnet-048004a221680c742", "subnet-015e73eb696f8577c"]
allowed_cidr_blocks = ["172.31.0.0/16"]

db_name              = "omnisolve"
db_username          = "omnisolve"
db_password          = "OmniSolve2026SecurePass!"
db_instance_class    = "db.t3.micro"
db_allocated_storage = 20
s3_bucket_name       = "omnisolve-documents-dev-861870144419"
