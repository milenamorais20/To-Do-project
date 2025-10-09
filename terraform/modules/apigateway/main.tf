# REST API
resource "aws_api_gateway_rest_api" "api" {
  name        = var.bucket_name
  description = "API Gateway REST for tasks (proxy lambdas)"
}

# Root resource id for convenience
data "aws_region" "current" {}

# ---- Create resource paths ----
resource "aws_api_gateway_resource" "create_task" {
  rest_api_id = aws_api_gateway_rest_api.api.id
  parent_id   = aws_api_gateway_rest_api.api.root_resource_id
  path_part   = "create-task"
}

resource "aws_api_gateway_resource" "list_tasks" {
  rest_api_id = aws_api_gateway_rest_api.api.id
  parent_id   = aws_api_gateway_rest_api.api.root_resource_id
  path_part   = "list-tasks"
}

resource "aws_api_gateway_resource" "update_task" {
  rest_api_id = aws_api_gateway_rest_api.api.id
  parent_id   = aws_api_gateway_rest_api.api.root_resource_id
  path_part   = "update-task"
}

resource "aws_api_gateway_resource" "update_task_pk" {
  rest_api_id = aws_api_gateway_rest_api.api.id
  parent_id   = aws_api_gateway_resource.update_task.id
  path_part   = "{pk}"
}

resource "aws_api_gateway_resource" "update_task_sk" {
  rest_api_id = aws_api_gateway_rest_api.api.id
  parent_id   = aws_api_gateway_resource.update_task_pk.id
  path_part   = "{sk}"
}

# ---- Methods ----
resource "aws_api_gateway_method" "create_task_post" {
  rest_api_id   = aws_api_gateway_rest_api.api.id
  resource_id   = aws_api_gateway_resource.create_task.id
  http_method   = "POST"
  authorization = "NONE"
}

resource "aws_api_gateway_method" "list_tasks_get" {
  rest_api_id   = aws_api_gateway_rest_api.api.id
  resource_id   = aws_api_gateway_resource.list_tasks.id
  http_method   = "GET"
  authorization = "NONE"
}

resource "aws_api_gateway_method" "update_task_put" {
  rest_api_id   = aws_api_gateway_rest_api.api.id
  resource_id   = aws_api_gateway_resource.update_task_sk.id
  http_method   = "PUT"
  authorization = "NONE"

  request_parameters = {
    "method.request.path.sk" = true
  }
}

# ---- Integrations (Lambda Proxy) ----
resource "aws_api_gateway_integration" "create_task" {
  rest_api_id = aws_api_gateway_rest_api.api.id
  resource_id = aws_api_gateway_resource.create_task.id
  http_method = aws_api_gateway_method.create_task_post.http_method

  integration_http_method = "POST"
  type                    = "AWS_PROXY"
  uri                     = "arn:aws:apigateway:${data.aws_region.current.name}:lambda:path/2015-03-31/functions/${var.uri_create_task}/invocations"
}

resource "aws_api_gateway_integration" "list_tasks" {
  rest_api_id = aws_api_gateway_rest_api.api.id
  resource_id = aws_api_gateway_resource.list_tasks.id
  http_method = aws_api_gateway_method.list_tasks_get.http_method

  integration_http_method = "POST"
  type                    = "AWS_PROXY"
  uri                     = "arn:aws:apigateway:${data.aws_region.current.name}:lambda:path/2015-03-31/functions/${var.uri_list_tasks}/invocations"
}

resource "aws_api_gateway_integration" "update_task" {
  rest_api_id = aws_api_gateway_rest_api.api.id
  resource_id = aws_api_gateway_resource.update_task_sk.id
  http_method = aws_api_gateway_method.update_task_put.http_method

  integration_http_method = "POST"
  type                    = "AWS_PROXY"
  uri                     = "arn:aws:apigateway:${data.aws_region.current.name}:lambda:path/2015-03-31/functions/${var.uri_updtae_task}/invocations"
}

# Allow API Gateway to invoke Lambdas
resource "aws_lambda_permission" "apigw_invoke_create" {
  statement_id  = "AllowAPIGatewayInvoke_create"
  action        = "lambda:InvokeFunction"
  function_name = var.function_create_task
  principal     = "apigateway.amazonaws.com"
  source_arn    = "${aws_api_gateway_rest_api.api.execution_arn}/*/*"
}

resource "aws_lambda_permission" "apigw_invoke_list" {
  statement_id  = "AllowAPIGatewayInvoke_list"
  action        = "lambda:InvokeFunction"
  function_name = var.function_list_tasks
  principal     = "apigateway.amazonaws.com"
  source_arn    = "${aws_api_gateway_rest_api.api.execution_arn}/*/*"
}

resource "aws_lambda_permission" "apigw_invoke_update" {
  statement_id  = "AllowAPIGatewayInvoke_update"
  action        = "lambda:InvokeFunction"
  function_name = var.function_updtae_task
  principal     = "apigateway.amazonaws.com"
  source_arn    = "${aws_api_gateway_rest_api.api.execution_arn}/*/*"
}

# Deployment and Stage
resource "aws_api_gateway_deployment" "deployment" {
  rest_api_id = aws_api_gateway_rest_api.api.id

  # force new deployment if integrations/lambdas change
  triggers = {
    redeployment = sha1(join("", [
      aws_api_gateway_integration.create_task.id,
      aws_api_gateway_integration.list_tasks.id,
      aws_api_gateway_integration.update_task.id,
      var.uri_create_task,
      var.uri_list_tasks,
      var.uri_updtae_task
    ]))
  }

  depends_on = [
    aws_api_gateway_integration.create_task,
    aws_api_gateway_integration.list_tasks,
    aws_api_gateway_integration.update_task
  ]
}

resource "aws_api_gateway_stage" "stage" {
  stage_name    = var.stage_name
  rest_api_id   = aws_api_gateway_rest_api.api.id
  deployment_id = aws_api_gateway_deployment.deployment.id
}
