provider "aws" {
  region  = "us-east-1"
  profile = "default"
}

# S3
variable "bucket_name" {type = string}

resource "aws_s3_bucket" "this" {
  bucket = "lambda-bucket-${var.bucket_name}"
}

resource "aws_s3_bucket_versioning" "versioning" {
  bucket = aws_s3_bucket.this.id

  versioning_configuration {
    status = "Enabled"
  }
}

resource "aws_s3_bucket_public_access_block" "this" {
  bucket = aws_s3_bucket.this.id

  block_public_acls       = false
  block_public_policy     = false
  ignore_public_acls      = false
  restrict_public_buckets = false
}

# Dynamo Module
module "dynamodb" {
  source = "./modules/dynamodb"

  table_name = "${var.bucket_name}-table"
  hash_key_name = "pk"
  hash_key_type = "S"
  range_key_name = "sk"
  range_key_type = "S"

  tags = {
    project = "TODOLambdaJava"
    ManagedBy   = "Terraform"
  }
}

resource "aws_iam_policy" "lambda_dynamodb_write_policy" {
  name = "lambda-dynamodb-tasks-write-policy"
  description = "Policy to allow Lambda functions to access DynamoDB table Tasks"
  policy = jsonencode({
    "Version": "2012-10-17",
    "Statement": [
      {
        "Effect": "Allow",
        "Action": [
          "dynamodb:PutItem",
          "dynamodb:GetItem",
          "dynamodb:UpdateItem",
          "dynamodb:DeleteItem"
        ],
        "Resource": module.dynamodb.table_arn
      }
    ]
  })
}

resource "aws_iam_policy" "lambda_dynamodb_read_policy" {
  name = "lambda-list-tasks-dynamodb-policy"
  description = "Policy to allow Lambda functions to read from DynamoDB table Tasks"
  policy = jsonencode({
    "Version": "2012-10-17",
    "Statement": [
      {
        "Effect": "Allow",
        "Action": [
          "dynamodb:Scan",
          "dynamodb:Query",
          "dynamodb:GetItem"
        ],
        "Resource": module.dynamodb.table_arn
      }
    ]
  })
}

# CreateTask Module
module "CreateTask" {
  source = "./modules/lambda"

  function_name     = "CreateTask"
  handler           = "controller.CreateTask::handleRequest"
  runtime           = "java21"
  source_code_path  = "../target/TODOLambdaJava-1.0-SNAPSHOT.jar"
  memory_size       = 1024
  timeout           = 60
  tasks_table_name = module.dynamodb.table_name
  tags = {
    Project   = "TODOLambdaJava"
    ManagedBy = "Terraform"
  }
}

resource "aws_iam_role_policy_attachment" "create_lambda_dynamodb_access" {
  role = module.CreateTask.iam_role_name
  policy_arn = aws_iam_policy.lambda_dynamodb_write_policy.arn
}
output "nome_da_tabela" {
  value = module.dynamodb.table_name
}
output "arn_da_create_lambda" {
  description = "O ARN da função Lambda de criação de tarefas"
  value = module.CreateTask.lambda_function_arn
}

# ListTasks Module