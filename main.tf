terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "5.93.0"
    }
  }
}

provider "aws" {
  region = "us-east-1"
}

# PostgreSQL Database
resource "aws_db_instance" "chatappdb" {
  allocated_storage    = 20
  engine               = "postgres"
  engine_version       = "17.2"
  instance_class       = "db.t3.micro"
  identifier           = "chatappdb"
  username             = "postgres"
  password             = "postgres"
  publicly_accessible  = true
  skip_final_snapshot  = true
  db_name              = "chatappdb"
  parameter_group_name = "default.postgres17"
}

resource "aws_cognito_user_pool" "chatapp_user_pool" {
  name = "chatapp-user-pool"

    username_configuration {
    case_sensitive = false
  }

  #username_attributes = ["email"]
  auto_verified_attributes = ["email"]

    admin_create_user_config {
    allow_admin_create_user_only = false
  }

  schema {
    attribute_data_type = "String"
    name               = "email"
    required           = true
    string_attribute_constraints {
      min_length = 5
      max_length = 50
    }
  }
}

resource "aws_cognito_user_pool_client" "chatapp_client" {
  name         = "chatapp-client"
  user_pool_id = aws_cognito_user_pool.chatapp_user_pool.id
  generate_secret = false
  explicit_auth_flows = [
    "ALLOW_USER_PASSWORD_AUTH",
    "ALLOW_REFRESH_TOKEN_AUTH",
    "ALLOW_USER_SRP_AUTH"
  ]
}

# S3 Bucket for Profile Pictures
resource "aws_s3_bucket" "profile_pictures" {
  bucket = "profilepicchatappbucket"

  # Prevent accidental deletion of the bucket
  lifecycle {
    prevent_destroy = false # Set to true in production
  }
}

# Optional: S3 Bucket Public Access Block (recommended for security)
resource "aws_s3_bucket_public_access_block" "profile_pictures" {
  bucket = aws_s3_bucket.profile_pictures.id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}
# Frontend Application (with required IAM role)
resource "aws_elastic_beanstalk_application" "frontend" {
  name = "chatappfrontend"
}

resource "aws_elastic_beanstalk_environment" "frontend" {
  name                = "chatappfrontend-env"
  application         = aws_elastic_beanstalk_application.frontend.name
  solution_stack_name = "64bit Amazon Linux 2023 v4.5.0 running Docker"

  setting {
    namespace = "aws:autoscaling:launchconfiguration"
    name      = "IamInstanceProfile"
    value     = "LabInstanceProfile"
  }
  setting {
    namespace = "aws:elasticbeanstalk:environment"
    name      = "ServiceRole"
    value     = "LabRole"
  }
  setting {
    namespace = "aws:autoscaling:launchconfiguration"
    name      = "EC2KeyName"
    value     = "vockey"
  }
  setting {
    namespace = "aws:elasticbeanstalk:environment"
    name      = "EnvironmentType"
    value     = "LoadBalanced" # <-- This enables ALB
  }

  setting {
    namespace = "aws:elasticbeanstalk:environment"
    name      = "LoadBalancerType"
    value     = "application" # <-- Explicitly set to ALB
  }
  setting {
    namespace = "aws:elasticbeanstalk:application:environment"
    name      = "VITE_API_BASE_URL"
    value     = ""
  }

  setting {
    namespace = "aws:elasticbeanstalk:application:environment"
    name      = "VITE_AWS_AUTHORITY"
    value     = ""
  }

  setting {
    namespace = "aws:elasticbeanstalk:application:environment"
    name      = "VITE_CLIENT_ID"
    value     = aws_cognito_user_pool_client.chatapp_client.id
  }

  setting {
    namespace = "aws:elasticbeanstalk:application:environment"
    name      = "VITE_REDIRECT_URL"
    value     = ""
  }

  setting {
    namespace = "aws:elasticbeanstalk:application:environment"
    name      = "VITE_COGNITO_DOMAIN"
    value     = ""
  }

  setting {
    namespace = "aws:elasticbeanstalk:application:environment"
    name      = "VITE_LOGOUT_URL"
    value     = ""
  }

}

# Backend Application (same fixes)
resource "aws_elastic_beanstalk_application" "backend" {
  name = "chatappbackend"
}

resource "aws_elastic_beanstalk_environment" "backend" {
  name                = "chatappbackend-env"
  application         = aws_elastic_beanstalk_application.backend.name
  solution_stack_name = "64bit Amazon Linux 2023 v4.5.0 running Docker"

  setting {
    namespace = "aws:autoscaling:launchconfiguration"
    name      = "IamInstanceProfile"
    value     = "LabInstanceProfile"
  }
  setting {
    namespace = "aws:elasticbeanstalk:environment"
    name      = "ServiceRole"
    value     = "LabRole"
  }
  setting {
    namespace = "aws:autoscaling:launchconfiguration"
    name      = "EC2KeyName"
    value     = "vockey"
  }
  setting {
    namespace = "aws:elasticbeanstalk:environment"
    name      = "EnvironmentType"
    value     = "LoadBalanced" # <-- This enables ALB
  }

  setting {
    namespace = "aws:elasticbeanstalk:environment"
    name      = "LoadBalancerType"
    value     = "application" # <-- Explicitly set to ALB
  }
  setting {
    namespace = "aws:elasticbeanstalk:application:environment"
    name      = "DB_ADDRESS"
    value     = "${aws_db_instance.chatappdb.address}:5432/chatappdb"
  }

  setting {
    namespace = "aws:elasticbeanstalk:application:environment"
    name      = "bucket_name"
    value     = aws_s3_bucket.profile_pictures.bucket
  }

}
resource "aws_cloudwatch_log_group" "chatapp_logs" {
  name = "ChatAppLogs"
}
