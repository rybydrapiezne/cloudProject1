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

  allowed_oauth_flows = ["code"]

  allowed_oauth_scopes = ["openid", "email", "phone"]

  supported_identity_providers = ["COGNITO"]

  allowed_oauth_flows_user_pool_client = true

  callback_urls = ["https://frontend-domain"]
  logout_urls   = ["https://frontend-domain"]

  explicit_auth_flows = [
    "ALLOW_USER_PASSWORD_AUTH",
    "ALLOW_REFRESH_TOKEN_AUTH",
    "ALLOW_USER_SRP_AUTH"
  ]
}

resource "aws_s3_bucket" "profile_pictures" {
  bucket = "profilepicchatappbucket"

  lifecycle {
    prevent_destroy = false 
  }
}

resource "aws_s3_bucket_public_access_block" "profile_pictures" {
  bucket = aws_s3_bucket.profile_pictures.id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}
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
    value     = "LoadBalanced"
  }

  setting {
    namespace = "aws:elasticbeanstalk:environment"
    name      = "LoadBalancerType"
    value     = "application" 
  }
  setting {
    namespace = "aws:elasticbeanstalk:application:environment"
    name      = "VITE_API_BASE_URL"
    value     = ""
  }


}

resource "aws_elastic_beanstalk_application" "backend" {
  name = "chatappbackend"
}

resource "aws_cognito_user_pool_domain" "chatapp_domain" {
  domain       = "chatapp-auth-domain"
  user_pool_id = aws_cognito_user_pool.chatapp_user_pool.id
}
resource "aws_cloudwatch_log_group" "chatapp_logs" {
  name = "ChatAppLogs"
}
resource "aws_sns_topic" "chatapp_notifications" {
  name = "chatapp-notifications-topic"
}

resource "aws_ecs_cluster" "chatapp_cluster" {
  name = "chatapp-cluster"
}

# Log group for ECS services
resource "aws_cloudwatch_log_group" "ecs_logs" {
  name              = "/ecs/chatapp"
  retention_in_days = 7
}



# Shared values for container definitions
locals {
  container_defaults = {
    cpu               = 256
    memory            = 512
    network_mode      = "awsvpc"
    requires_compatibilities = ["FARGATE"]
    execution_role_arn       = "arn:aws:iam::935391003181:role/LabRole"
    family_prefix            = "chatapp"
    log_configuration = {
      logDriver = "awslogs"
      options = {
        awslogs-group         = aws_cloudwatch_log_group.ecs_logs.name
        awslogs-region        = "us-east-1"
        awslogs-stream-prefix = "ecs"
      }
    }
  }
}


resource "aws_ecs_task_definition" "chat_service" {
  family                   = "${local.container_defaults.family_prefix}-chat"
  network_mode             = local.container_defaults.network_mode
  requires_compatibilities = local.container_defaults.requires_compatibilities
  cpu                      = local.container_defaults.cpu
  memory                   = local.container_defaults.memory
  task_role_arn      = "arn:aws:iam::935391003181:role/LabRole"
  execution_role_arn = "arn:aws:iam::935391003181:role/LabRole"

  container_definitions = jsonencode([{
    name      = "chat"
    image     = "935391003181.dkr.ecr.us-east-1.amazonaws.com/chat-service"
    essential = true
    portMappings = [{ containerPort = 80 }]
    environment = [
   {
    name  = "CHAT_DB"
    value = aws_db_instance.chatappdb.endpoint
    }
  ]

    logConfiguration = local.container_defaults.log_configuration
  }])
}

resource "aws_ecs_task_definition" "notification_service" {
  family                   = "${local.container_defaults.family_prefix}-notification"
  network_mode             = local.container_defaults.network_mode
  requires_compatibilities = local.container_defaults.requires_compatibilities
  cpu                      = local.container_defaults.cpu
  memory                   = local.container_defaults.memory
  task_role_arn      = "arn:aws:iam::935391003181:role/LabRole"
  execution_role_arn = "arn:aws:iam::935391003181:role/LabRole"

  container_definitions = jsonencode([{
    name      = "notification"
    image     = "935391003181.dkr.ecr.us-east-1.amazonaws.com/notification-service"
    essential = true
    portMappings = [{ containerPort = 80 }]
    environment = [
  {
    name  = "SNS_ARN"
    value = aws_sns_topic.chatapp_notifications.arn
  }
  ]

    logConfiguration = local.container_defaults.log_configuration
  }])
}

resource "aws_ecs_task_definition" "profile_service" {
  family                   = "${local.container_defaults.family_prefix}-profile"
  network_mode             = local.container_defaults.network_mode
  requires_compatibilities = local.container_defaults.requires_compatibilities
  cpu                      = local.container_defaults.cpu
  memory                   = local.container_defaults.memory
  task_role_arn      = "arn:aws:iam::935391003181:role/LabRole"
  execution_role_arn = "arn:aws:iam::935391003181:role/LabRole"

  container_definitions = jsonencode([{
    name      = "profile"
    image     = "935391003181.dkr.ecr.us-east-1.amazonaws.com/profile-service"
    essential = true
    portMappings = [{ containerPort = 80 }]
    environment = [
  {
    name  = "bucket_name"
    value = aws_s3_bucket.profile_pictures.bucket
  },
  {
    name  = "CHAT_DB"
    value = aws_db_instance.chatappdb.endpoint
  }
  ]
    logConfiguration = local.container_defaults.log_configuration
  }])
}

resource "aws_ecs_task_definition" "auth_service" {
  family                   = "${local.container_defaults.family_prefix}-auth"
  network_mode             = local.container_defaults.network_mode
  requires_compatibilities = local.container_defaults.requires_compatibilities
  cpu                      = local.container_defaults.cpu
  memory                   = local.container_defaults.memory
  task_role_arn      = "arn:aws:iam::935391003181:role/LabRole"
  execution_role_arn = "arn:aws:iam::935391003181:role/LabRole"
  container_definitions = jsonencode([{
    name      = "auth"
    image     = "935391003181.dkr.ecr.us-east-1.amazonaws.com/auth-service"
    essential = true
    portMappings = [{ containerPort = 80 }]
    environment = [
      { name = "auth_service",        value = "http://127.0.0.1" },
      { name = "chat_service",        value = "http://chat:80" },
      { name = "notification_service", value = "http://notification:80" },
      { name = "profile_service",     value = "http://profile:80" },
      { name = "COGNITO_USER_POOL",    value = aws_cognito_user_pool.chatapp_user_pool.id },
      { name = "COGNITO_USER_ID",      value = aws_cognito_user_pool_client.chatapp_client.id }
    ]
    logConfiguration = local.container_defaults.log_configuration
  }])
}

resource "aws_ecs_service" "chat" {
  name            = "chat-service"
  cluster         = aws_ecs_cluster.chatapp_cluster.id
  task_definition = aws_ecs_task_definition.chat_service.arn
  desired_count   = 2
  launch_type     = "FARGATE"

  network_configuration {
    subnets         = ["subnet-0605448cbd489a978", "subnet-0250ff5f1a54530df", "subnet-0606351734696e81f", "subnet-05aa3062a1853a3ae", "subnet-0663e117618197254", "subnet-03129c5dce6f4cf3e"] # Update to your subnet IDs
    security_groups = ["sg-0cd9f3212a4aee613"]                        
    assign_public_ip = true
  }
}

resource "aws_ecs_service" "notification" {
  name            = "notification-service"
  cluster         = aws_ecs_cluster.chatapp_cluster.id
  task_definition = aws_ecs_task_definition.notification_service.arn
  desired_count   = 2
  launch_type     = "FARGATE"

  network_configuration {
    subnets         = ["subnet-0605448cbd489a978", "subnet-0250ff5f1a54530df", "subnet-0606351734696e81f", "subnet-05aa3062a1853a3ae", "subnet-0663e117618197254", "subnet-03129c5dce6f4cf3e"] # Update to your subnet IDs
    security_groups = ["sg-0cd9f3212a4aee613"]
    assign_public_ip = true
  }
}

resource "aws_ecs_service" "profile" {
  name            = "profile-service"
  cluster         = aws_ecs_cluster.chatapp_cluster.id
  task_definition = aws_ecs_task_definition.profile_service.arn
  desired_count   = 2
  launch_type     = "FARGATE"

  network_configuration {
    subnets         = ["subnet-0605448cbd489a978", "subnet-0250ff5f1a54530df", "subnet-0606351734696e81f", "subnet-05aa3062a1853a3ae", "subnet-0663e117618197254", "subnet-03129c5dce6f4cf3e"] # Update to your subnet IDs
    security_groups = ["sg-0cd9f3212a4aee613"]
    assign_public_ip = true
  }
}

resource "aws_ecs_service" "auth" {
  name            = "auth-service"
  cluster         = aws_ecs_cluster.chatapp_cluster.id
  task_definition = aws_ecs_task_definition.auth_service.arn
  desired_count   = 2
  launch_type     = "FARGATE"

  network_configuration {
    subnets         = ["subnet-0605448cbd489a978", "subnet-0250ff5f1a54530df", "subnet-0606351734696e81f", "subnet-05aa3062a1853a3ae", "subnet-0663e117618197254", "subnet-03129c5dce6f4cf3e"] # Update to your subnet IDs
    security_groups = ["sg-0cd9f3212a4aee613"]
    assign_public_ip = true
  }
}
resource "aws_dynamodb_table" "notifications" {
  name           = "notifications"
  billing_mode   = "PROVISIONED"
  read_capacity  = 5
  write_capacity = 5
  hash_key       = "id"

  attribute {
    name = "id"
    type = "S"
  }

  tags = {
    Environment = "production"
    Application = "notification-service"
  }
}


resource "aws_appautoscaling_target" "ecs_target_auth" {
  max_capacity       = 10
  min_capacity       = 2
  resource_id        = "service/${aws_ecs_cluster.chatapp_cluster.name}/${aws_ecs_service.auth.name}"
  scalable_dimension = "ecs:service:DesiredCount"
  service_namespace  = "ecs"
}

resource "aws_appautoscaling_policy" "scale_out_auth" {
  name               = "scale-out-auth"
  policy_type        = "TargetTrackingScaling"
  resource_id        = aws_appautoscaling_target.ecs_target_auth.resource_id
  scalable_dimension = aws_appautoscaling_target.ecs_target_auth.scalable_dimension
  service_namespace  = aws_appautoscaling_target.ecs_target_auth.service_namespace

  target_tracking_scaling_policy_configuration {
    predefined_metric_specification {
      predefined_metric_type = "ECSServiceAverageCPUUtilization"
    }
    target_value       = 60.0
    scale_in_cooldown  = 60
    scale_out_cooldown = 60
  }
}
resource "aws_appautoscaling_target" "ecs_target_chat" {
  max_capacity       = 10
  min_capacity       = 2
  resource_id        = "service/${aws_ecs_cluster.chatapp_cluster.name}/${aws_ecs_service.chat.name}"
  scalable_dimension = "ecs:service:DesiredCount"
  service_namespace  = "ecs"
}

resource "aws_appautoscaling_policy" "scale_out_chat" {
  name               = "scale-out-auth"
  policy_type        = "TargetTrackingScaling"
  resource_id        = aws_appautoscaling_target.ecs_target_chat.resource_id
  scalable_dimension = aws_appautoscaling_target.ecs_target_chat.scalable_dimension
  service_namespace  = aws_appautoscaling_target.ecs_target_chat.service_namespace

  target_tracking_scaling_policy_configuration {
    predefined_metric_specification {
      predefined_metric_type = "ECSServiceAverageCPUUtilization"
    }
    target_value       = 60.0
    scale_in_cooldown  = 60
    scale_out_cooldown = 60
  }
}
resource "aws_appautoscaling_target" "ecs_target_profile" {
  max_capacity       = 10
  min_capacity       = 2
  resource_id        = "service/${aws_ecs_cluster.chatapp_cluster.name}/${aws_ecs_service.profile.name}"
  scalable_dimension = "ecs:service:DesiredCount"
  service_namespace  = "ecs"
}

resource "aws_appautoscaling_policy" "scale_out_profile" {
  name               = "scale-out-auth"
  policy_type        = "TargetTrackingScaling"
  resource_id        = aws_appautoscaling_target.ecs_target_profile.resource_id
  scalable_dimension = aws_appautoscaling_target.ecs_target_profile.scalable_dimension
  service_namespace  = aws_appautoscaling_target.ecs_target_profile.service_namespace

  target_tracking_scaling_policy_configuration {
    predefined_metric_specification {
      predefined_metric_type = "ECSServiceAverageCPUUtilization"
    }
    target_value       = 60.0
    scale_in_cooldown  = 60
    scale_out_cooldown = 60
  }
}
resource "aws_appautoscaling_target" "ecs_target_notification" {
  max_capacity       = 10
  min_capacity       = 2
  resource_id        = "service/${aws_ecs_cluster.chatapp_cluster.name}/${aws_ecs_service.notification.name}"
  scalable_dimension = "ecs:service:DesiredCount"
  service_namespace  = "ecs"
}

resource "aws_appautoscaling_policy" "scale_out_notification" {
  name               = "scale-out-auth"
  policy_type        = "TargetTrackingScaling"
  resource_id        = aws_appautoscaling_target.ecs_target_notification.resource_id
  scalable_dimension = aws_appautoscaling_target.ecs_target_notification.scalable_dimension
  service_namespace  = aws_appautoscaling_target.ecs_target_notification.service_namespace

  target_tracking_scaling_policy_configuration {
    predefined_metric_specification {
      predefined_metric_type = "ECSServiceAverageCPUUtilization"
    }
    target_value       = 60.0
    scale_in_cooldown  = 60
    scale_out_cooldown = 60
  }
}

