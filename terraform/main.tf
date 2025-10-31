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
  name = "lambda-dynamodb-lists-write-policy"
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
  name = "lambda-list-lists-dynamodb-policy"
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

# CreateList Module
module "CreateList" {
  source = "./modules/lambda"

  function_name     = "CreateList"
  handler           = "controller.CreateList::handleRequest"
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
  role = module.CreateList.iam_role_name
  policy_arn = aws_iam_policy.lambda_dynamodb_write_policy.arn
}
output "nome_da_tabela" {
  value = module.dynamodb.table_name
}
output "arn_da_create_lambda" {
  description = "O ARN da função Lambda de criação de tarefas"
  value = module.CreateList.lambda_function_arn
}

# ListLists Module
module "ListLists" {
  source = "./modules/lambda"

  function_name = "ListLists"
  handler = "controller.ListLists::handleRequest"
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
  role = module.ListLists.iam_role_name

  policy_arn = aws_iam_policy.lambda_dynamodb_read_policy.arn
}

output "arn_da_list_lambda" {
  description = "O ARN da função Lambda de listagem de tarefas"
  value = module.ListLists.lambda_function_arn
}


# UpdateList Module
module "UpdateList" {
  source = "./modules/lambda"

  function_name = "UpdateList"
  handler = "controller.UpdateList::handleRequest"
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
  role       = module.UpdateList.iam_role_name
  policy_arn = aws_iam_policy.lambda_dynamodb_write_policy.arn
}

resource "aws_iam_role_policy_attachment" "update_lambda_dynamodb_read_access" {
  role = module.UpdateList.iam_role_name

  policy_arn = aws_iam_policy.lambda_dynamodb_read_policy.arn
}

output "arn_da_update_lambda" {
  description = "O ARN da função Lambda de atualização de tarefas"
  value = module.UpdateList.lambda_function_arn
}

#GetListById Module
module "ListById" {
  source = "./modules/lambda"

  function_name = "ListById"
  handler = "controller.GetListById::handleRequest"
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

resource "aws_iam_role_policy_attachment" "list_by_id_lambda_dynamodb_read_access" {
  role = module.ListById.iam_role_name

  policy_arn = aws_iam_policy.lambda_dynamodb_read_policy.arn
}

output "arn_da_list_by_id_lambda" {
  description = "O ARN da função Lambda de listagem de tarefas"
  value = module.ListById.lambda_function_arn
}

module "CreateItemList" {
  source = "./modules/lambda"

  function_name = "CreateItemList"
  handler = "controller.item.CreateItemList::handleRequest"
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

resource "aws_iam_role_policy_attachment" "create_item_lambda_dynamodb_access" {
  role = module.CreateItemList.iam_role_name
  policy_arn = aws_iam_policy.lambda_dynamodb_write_policy.arn
}

output "arn_da_create_item_list_lambda" {
  description = "O ARN da função Lambda de criação de item da lista"
  value = module.CreateItemList.lambda_function_arn
}

# ListItemsLists Module
module "ListItemsList" {
  source = "./modules/lambda"

  function_name = "ListItemsList"
  handler = "controller.item.ListItemsList::handleRequest"
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

resource "aws_iam_role_policy_attachment" "list_items_list_lambda_dynamodb_read_access" {
  role = module.ListItemsList.iam_role_name

  policy_arn = aws_iam_policy.lambda_dynamodb_read_policy.arn
}

output "arn_da_list_items_lambda" {
  description = "O ARN da função Lambda de listagem dos itens da lista"
  value = module.ListItemsList.lambda_function_arn
}

# UpdateItemList Module
module "UpdateItemList" {
  source = "./modules/lambda"

  function_name = "UpdateItemList"
  handler = "controller.item.UpdateItemList::handleRequest"
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

resource "aws_iam_role_policy_attachment" "update_item_list_lambda_dynamodb_write_access" {
  role       = module.UpdateItemList.iam_role_name
  policy_arn = aws_iam_policy.lambda_dynamodb_write_policy.arn
}

resource "aws_iam_role_policy_attachment" "update_item_list_lambda_dynamodb_read_access" {
  role = module.UpdateItemList.iam_role_name

  policy_arn = aws_iam_policy.lambda_dynamodb_read_policy.arn
}

output "arn_da_update_item_list_lambda" {
  description = "O ARN da função Lambda de atualização de tarefas"
  value = module.UpdateItemList.lambda_function_arn
}

# DeleteItemList Module
module "DeleteItemList" {
  source = "./modules/lambda"

  function_name = "DeleteItemList"
  handler = "controller.item.DeleteItemList::handleRequest"
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

resource "aws_iam_role_policy_attachment" "delete_item_list_lambda_dynamodb_write_access" {
  role = module.DeleteItemList.iam_role_name

  policy_arn = aws_iam_policy.lambda_dynamodb_write_policy.arn
}

output "arn_da_delete_item_list_lambda" {
  description = "O ARN da função Lambda de atualização de tarefas"
  value = module.DeleteItemList.lambda_function_arn
}

# ApiGateway
module "ApiRest" {
  source = "./modules/apigateway"
  bucket_name = "${var.bucket_name}-api"

  uri_create_list = module.CreateList.lambda_function_arn
  uri_list_lists = module.ListLists.lambda_function_arn
  uri_update_list = module.UpdateList.lambda_function_arn
  uri_get_list_by_id = module.ListById.lambda_function_arn

  uri_create_item_list = module.CreateItemList.lambda_function_arn
  uri_list_items_list = module.ListItemsList.lambda_function_arn
  uri_update_item_list = module.UpdateItemList.lambda_function_arn
  uri_delete_item_list = module.DeleteItemList.lambda_function_arn

  uri_export_request_list = module.ExportRequest.lambda_function_arn

  function_create_list = module.CreateList.lambda_function_name
  function_list_lists = module.ListLists.lambda_function_name
  function_update_list = module.UpdateList.lambda_function_name
  function_get_list_by_id = module.ListById.lambda_function_name

  function_create_item_list = module.CreateItemList.lambda_function_name
  function_list_items_list = module.ListItemsList.lambda_function_name
  function_update_item_list = module.UpdateItemList.lambda_function_name
  function_delete_item_list = module.DeleteItemList.lambda_function_name

  function_export_request_list = module.ExportRequest.lambda_function_name

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

# Variável para o e-mail remetente
variable "ses_from_email" {
  type        = string
  description = "O e-mail verificado no SES para usar como remetente."
  # Você pode definir um 'default' ou passar via .tfvars
  # default     = "seu-email@example.com"
}

# Recurso para verificar a identidade do e-mail no SES
resource "aws_ses_email_identity" "from_email" {
  email = var.ses_from_email
}

# Fila SQS para processamento assíncrono dos relatórios
resource "aws_sqs_queue" "report_queue" {
  name                       = "${var.bucket_name}-report-queue" # Usa sua variável de nome existente
  visibility_timeout_seconds = 300 # 5 minutos (deve ser >= timeout da LambdaGet)
}

# Política para a LambdaPost (ExportRequest) enviar mensagens ao SQS
resource "aws_iam_policy" "lambda_sqs_send_policy" {
  name        = "lambda-export-sqs-send-policy"
  description = "Permite que a Lambda envie mensagens para a fila de relatórios"
  policy = jsonencode({
    Version = "2012-10-17",
    Statement = [
      {
        Effect   = "Allow",
        Action   = "sqs:SendMessage",
        Resource = aws_sqs_queue.report_queue.arn
      }
    ]
  })
}

# Política para a LambdaGet (ExportProcess) ler do S3 e enviar pelo SES
resource "aws_iam_policy" "lambda_export_process_policy" {
  name        = "lambda-export-process-policy"
  description = "Permite que a Lambda escreva no S3 e envie e-mail pelo SES"
  policy = jsonencode({
    Version = "2012-10-17",
    Statement = [
      {
        Effect   = "Allow",
        Action   = "ses:SendRawEmail",
        Resource = "*" # O envio de e-mail geralmente é '*' ou no ARN da identidade
        # Para maior segurança, restrinja ao ARN da identidade SES:
        # Resource = aws_ses_email_identity.from_email.arn
      },
      {
        Effect   = "Allow",
        Action   = "s3:PutObject",
        # Concede acesso de escrita apenas na pasta 'exports/' do seu bucket
        Resource = "${aws_s3_bucket.this.arn}/exports/*"
      }
    ]
  })
}

# Lambda 1: LambdaPostFunction (API -> SQS)
module "ExportRequest" {
  source = "./modules/lambda"

  function_name = "ExportRequest"
  handler       = "controller.export.LambdaPostFunction::handleRequest"
  runtime       = "java21"
  source_code_path = "../target/TODOLambdaJava-1.0-SNAPSHOT.jar"
  memory_size   = 1024
  timeout       = 60

  # Passa a URL da fila SQS como variável de ambiente
  environment_variables = {
    SQS_QUEUE_URL = aws_sqs_queue.report_queue.id # .id retorna a URL da fila
  }

  tasks_table_name = module.dynamodb.table_name # Necessário para o módulo
  tags = {
    Project   = "TODOLambdaJava"
    ManagedBy = "Terraform"
  }
}

# Anexa a política de envio ao SQS
resource "aws_iam_role_policy_attachment" "export_request_sqs_access" {
  role       = module.ExportRequest.iam_role_name
  policy_arn = aws_iam_policy.lambda_sqs_send_policy.arn
}

# Lambda 2: LambdaGetFunction (SQS -> DDB -> S3 -> SES)
module "ExportProcess" {
  source = "./modules/lambda"

  function_name = "ExportProcess"
  handler       = "controller.export.LambdaGetFunction::handleRequest"
  runtime       = "java21"
  source_code_path = "../target/TODOLambdaJava-1.0-SNAPSHOT.jar"
  memory_size   = 1024
  timeout       = 300 # 5 minutos (mesmo tempo do SQS visibility timeout)

  # Passa todas as variáveis de ambiente necessárias
  environment_variables = {
    # TASKS_TABLE é adicionada automaticamente pelo módulo
    S3_BUCKET_NAME = aws_s3_bucket.this.bucket # Pega o nome do bucket já criado
    SES_FROM_EMAIL = var.ses_from_email
  }

  tasks_table_name = module.dynamodb.table_name
  tags = {
    Project   = "TODOLambdaJava"
    ManagedBy = "Terraform"
  }
}

# Anexa as políticas necessárias à LambdaGet
resource "aws_iam_role_policy_attachment" "export_process_dynamodb_access" {
  role       = module.ExportProcess.iam_role_name
  # Reutiliza sua política de leitura do DynamoDB existente
  policy_arn = aws_iam_policy.lambda_dynamodb_read_policy.arn
}

resource "aws_iam_role_policy_attachment" "export_process_s3_ses_access" {
  role       = module.ExportProcess.iam_role_name
  policy_arn = aws_iam_policy.lambda_export_process_policy.arn
}

# LambdaGet ler da fila SQS
resource "aws_iam_policy" "lambda_sqs_receive_policy" {
  name        = "lambda-export-sqs-receive-policy"
  description = "Permite que a Lambda leia mensagens da fila de relatórios"
  policy = jsonencode({
    Version = "2012-10-17",
    Statement = [
      {
        Effect   = "Allow",
        Action = [
          # Permissões necessárias para o gatilho do SQS
          "sqs:ReceiveMessage",
          "sqs:DeleteMessage",
          "sqs:GetQueueAttributes"
        ],
        Resource = aws_sqs_queue.report_queue.arn
      }
    ]
  })
}

# Política de leitura do SQS na Lambda ExportProcess
resource "aws_iam_role_policy_attachment" "export_process_sqs_access" {
  role       = module.ExportProcess.iam_role_name
  policy_arn = aws_iam_policy.lambda_sqs_receive_policy.arn
}

# Cria o gatilho que conecta o SQS à Lambda ExportProcess
resource "aws_lambda_event_source_mapping" "export_sqs_trigger" {
  event_source_arn = aws_sqs_queue.report_queue.arn
  function_name    = module.ExportProcess.lambda_function_arn
  batch_size       = 1 # Processa uma mensagem de cada vez
}