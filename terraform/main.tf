provider "aws" {
  region  = "us-east-1"
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

  table_name = "${var.bucket_name}-table-2"
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
  name = "lambda-dynamodb-tasks-write-policy-2"
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
  name = "lambda-list-tasks-dynamodb-policy-2"
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

  function_name     = "CreateTask-2"
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
module "ListTasks" {
  source = "./modules/lambda"

  function_name = "ListTasks-2"
  handler = "controller.ListTasks::handleRequest"
  runtime = "java21"
  source_code_path = "../target/TODOLambdaJava-1.0-SNAPSHOT.jar"
  memory_size = 1024
  timeout = 60
  tasks_table_name = module.dynamodb.table_name
  tags = {
    Project   = "TODOLambdaJava"
    ManagedBy = "Terraform"
  }
}

resource "aws_iam_role_policy_attachment" "list_lambda_dynamodb_read_access" {
  role = module.ListTasks.iam_role_name

  policy_arn = aws_iam_policy.lambda_dynamodb_read_policy.arn
}

output "arn_da_list_lambda" {
  description = "O ARN da função Lambda de listagem de tarefas"
  value = module.ListTasks.lambda_function_arn
}


# UpdateTask Module
module "UpdateTask" {
  source = "./modules/lambda"

  function_name = "UpdateTask-2"
  handler = "controller.UpdateTask::handleRequest"
  runtime = "java21"
  source_code_path = "../target/TODOLambdaJava-1.0-SNAPSHOT.jar"
  memory_size = 1024
  timeout = 60
  tasks_table_name = module.dynamodb.table_name
  tags = {
    Project   = "TODOLambdaJava"
    ManagedBy = "Terraform"
  }
}

resource "aws_iam_role_policy_attachment" "update_lambda_dynamodb_write_access" {
  role       = module.UpdateTask.iam_role_name
  policy_arn = aws_iam_policy.lambda_dynamodb_write_policy.arn
}

resource "aws_iam_role_policy_attachment" "update_lambda_dynamodb_read_access" {
  role = module.UpdateTask.iam_role_name

  policy_arn = aws_iam_policy.lambda_dynamodb_read_policy.arn
}

output "arn_da_update_lambda" {
  description = "O ARN da função Lambda de atualização de tarefas"
  value = module.UpdateTask.lambda_function_arn
}


# ApiGateway
module "ApiRest" {
  source = "./modules/apigateway"
  bucket_name = "${var.bucket_name}-api"

  uri_create_task = module.CreateTask.lambda_function_arn
  uri_list_tasks = module.ListTasks.lambda_function_arn
  uri_updtae_task = module.UpdateTask.lambda_function_arn

  function_create_task = module.CreateTask.lambda_function_name
  function_list_tasks = module.ListTasks.lambda_function_name
  function_updtae_task = module.UpdateTask.lambda_function_name

  cognito_user_pool_arn = module.Cognito.user_pool_arn
  redeployment_trigger = timestamp()
}

# Cognito
module "Cognito" {
  source = "./modules/cognito"

  user_pool_name = "${var.bucket_name}-user-pool"
  user_pool_client = "${var.bucket_name}-user-pool-client"
}

output "cognito_user_pool_client_id" {
  description = "ID do Cliente do User Pool do Cognito para usar na autenticação."
  value = module.Cognito.cognito_user_pool_client_id
}