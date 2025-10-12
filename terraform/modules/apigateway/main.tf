# REST API
resource "aws_api_gateway_rest_api" "api" {
  name        = var.bucket_name
  description = "API Gateway REST for lists (proxy lambdas)"
}

# Root resource id for convenience
data "aws_region" "current" {}

# ---- Create resource paths ----
resource "aws_api_gateway_resource" "create_list" {
  rest_api_id = aws_api_gateway_rest_api.api.id
  parent_id   = aws_api_gateway_rest_api.api.root_resource_id
  path_part   = "create-list"
}

resource "aws_api_gateway_resource" "list_lists" {
  rest_api_id = aws_api_gateway_rest_api.api.id
  parent_id   = aws_api_gateway_rest_api.api.root_resource_id
  path_part   = "list-lists"
}

resource "aws_api_gateway_resource" "update_list" {
  rest_api_id = aws_api_gateway_rest_api.api.id
  parent_id   = aws_api_gateway_rest_api.api.root_resource_id
  path_part   = "update-list"
}

resource "aws_api_gateway_resource" "update_list_pk" {
  rest_api_id = aws_api_gateway_rest_api.api.id
  parent_id   = aws_api_gateway_resource.update_list.id
  path_part   = "{pk}"
}

resource "aws_api_gateway_resource" "update_list_sk" {
  rest_api_id = aws_api_gateway_rest_api.api.id
  parent_id   = aws_api_gateway_resource.update_list_pk.id
  path_part   = "{sk}"
}

resource "aws_api_gateway_resource" "item_list" {
  rest_api_id = aws_api_gateway_rest_api.api.id
  parent_id   = aws_api_gateway_rest_api.api.root_resource_id
  path_part   = "item-list"
}

resource "aws_api_gateway_resource" "create_item_list" {
  rest_api_id = aws_api_gateway_rest_api.api.id
  parent_id   = aws_api_gateway_resource.item_list.id
  path_part   = "create-item-list"
}

resource "aws_api_gateway_resource" "list_items_list" {
  rest_api_id = aws_api_gateway_rest_api.api.id
  parent_id   = aws_api_gateway_resource.item_list.id
  path_part   = "list-items-lists"
}

resource "aws_api_gateway_authorizer" "cognito_authorizer" {
  name          = "CognitoUserPoolAuthorizer"
  type          = "COGNITO_USER_POOLS"
  rest_api_id   = aws_api_gateway_rest_api.api.id
  provider_arns = [var.cognito_user_pool_arn]
}

# ---- Methods ----
resource "aws_api_gateway_method" "create_list_post" {
  rest_api_id   = aws_api_gateway_rest_api.api.id
  resource_id   = aws_api_gateway_resource.create_list.id
  http_method   = "POST"
  authorization = "COGNITO_USER_POOLS"
  authorizer_id = aws_api_gateway_authorizer.cognito_authorizer.id
}

resource "aws_api_gateway_method" "list_lists_get" {
  rest_api_id   = aws_api_gateway_rest_api.api.id
  resource_id   = aws_api_gateway_resource.list_lists.id
  http_method   = "GET"
  authorization = "COGNITO_USER_POOLS"
  authorizer_id = aws_api_gateway_authorizer.cognito_authorizer.id
}

resource "aws_api_gateway_method" "update_list_put" {
  rest_api_id   = aws_api_gateway_rest_api.api.id
  resource_id   = aws_api_gateway_resource.update_list_sk.id
  http_method   = "PUT"
  authorization = "COGNITO_USER_POOLS"
  authorizer_id = aws_api_gateway_authorizer.cognito_authorizer.id

  request_parameters = {
    "method.request.path.sk" = true
  }
}

resource "aws_api_gateway_method" "create_item_list_post" {
  rest_api_id   = aws_api_gateway_rest_api.api.id
  resource_id   = aws_api_gateway_resource.create_item_list.id
  http_method   = "POST"
  authorization = "COGNITO_USER_POOLS"
  authorizer_id = aws_api_gateway_authorizer.cognito_authorizer.id
}

resource "aws_api_gateway_method" "list_items_list_get" {
  rest_api_id   = aws_api_gateway_rest_api.api.id
  resource_id   = aws_api_gateway_resource.list_items_list.id
  http_method   = "GET"
  authorization = "COGNITO_USER_POOLS"
  authorizer_id = aws_api_gateway_authorizer.cognito_authorizer.id
}

# ---- Integrations (Lambda Proxy) ----
resource "aws_api_gateway_integration" "create_list" {
  rest_api_id = aws_api_gateway_rest_api.api.id
  resource_id = aws_api_gateway_resource.create_list.id
  http_method = aws_api_gateway_method.create_list_post.http_method

  integration_http_method = "POST"
  type                    = "AWS_PROXY"
  uri                     = "arn:aws:apigateway:${data.aws_region.current.name}:lambda:path/2015-03-31/functions/${var.uri_create_list}/invocations"
}

resource "aws_api_gateway_integration" "list_lists" {
  rest_api_id = aws_api_gateway_rest_api.api.id
  resource_id = aws_api_gateway_resource.list_lists.id
  http_method = aws_api_gateway_method.list_lists_get.http_method

  integration_http_method = "POST"
  type                    = "AWS_PROXY"
  uri                     = "arn:aws:apigateway:${data.aws_region.current.name}:lambda:path/2015-03-31/functions/${var.uri_list_lists}/invocations"
}

resource "aws_api_gateway_integration" "update_list" {
  rest_api_id = aws_api_gateway_rest_api.api.id
  resource_id = aws_api_gateway_resource.update_list_sk.id
  http_method = aws_api_gateway_method.update_list_put.http_method

  integration_http_method = "POST"
  type                    = "AWS_PROXY"
  uri                     = "arn:aws:apigateway:${data.aws_region.current.name}:lambda:path/2015-03-31/functions/${var.uri_update_list}/invocations"
}

resource "aws_api_gateway_integration" "create_item_list" {
  rest_api_id = aws_api_gateway_rest_api.api.id
  resource_id = aws_api_gateway_resource.create_item_list.id
  http_method = aws_api_gateway_method.create_item_list_post.http_method

  integration_http_method = "POST"
  type                    = "AWS_PROXY"
  uri                     = "arn:aws:apigateway:${data.aws_region.current.name}:lambda:path/2015-03-31/functions/${var.uri_create_item_list}/invocations"
}

resource "aws_api_gateway_integration" "list_items_list" {
  rest_api_id = aws_api_gateway_rest_api.api.id
  resource_id = aws_api_gateway_resource.list_items_list.id
  http_method = aws_api_gateway_method.list_items_list_get.http_method

  integration_http_method = "POST"
  type                    = "AWS_PROXY"
  uri                     = "arn:aws:apigateway:${data.aws_region.current.name}:lambda:path/2015-03-31/functions/${var.uri_list_items_list}/invocations"
}

# Allow API Gateway to invoke Lambdas
resource "aws_lambda_permission" "apigw_invoke_create" {
  statement_id  = "AllowAPIGatewayInvoke_create"
  action        = "lambda:InvokeFunction"
  function_name = var.function_create_list
  principal     = "apigateway.amazonaws.com"
  source_arn    = "${aws_api_gateway_rest_api.api.execution_arn}/*/*"
}

resource "aws_lambda_permission" "apigw_invoke_list" {
  statement_id  = "AllowAPIGatewayInvoke_list"
  action        = "lambda:InvokeFunction"
  function_name = var.function_list_lists
  principal     = "apigateway.amazonaws.com"
  source_arn    = "${aws_api_gateway_rest_api.api.execution_arn}/*/*"
}

resource "aws_lambda_permission" "apigw_invoke_update" {
  statement_id  = "AllowAPIGatewayInvoke_update"
  action        = "lambda:InvokeFunction"
  function_name = var.function_update_list
  principal     = "apigateway.amazonaws.com"
  source_arn    = "${aws_api_gateway_rest_api.api.execution_arn}/*/*"
}

resource "aws_lambda_permission" "apigw_invoke_create_item" {
  statement_id  = "AllowAPIGatewayInvoke_create_item"
  action        = "lambda:InvokeFunction"
  function_name = var.function_create_item_list
  principal     = "apigateway.amazonaws.com"
  source_arn    = "${aws_api_gateway_rest_api.api.execution_arn}/*/*"
}

resource "aws_lambda_permission" "apigw_invoke_item_list" {
  statement_id  = "AllowAPIGatewayInvoke_list_items_list"
  action        = "lambda:InvokeFunction"
  function_name = var.function_list_items_list
  principal     = "apigateway.amazonaws.com"
  source_arn    = "${aws_api_gateway_rest_api.api.execution_arn}/*/*"
}

# Deployment and Stage
resource "aws_api_gateway_deployment" "deployment" {
  rest_api_id = aws_api_gateway_rest_api.api.id

  lifecycle {
    create_before_destroy = true
  }

  # force new deployment if integrations/lambdas change
  triggers = {
    redeployment = sha1(join("", [
      aws_api_gateway_integration.create_list.id,
      aws_api_gateway_integration.list_lists.id,
      aws_api_gateway_integration.update_list.id,

      aws_api_gateway_integration.create_item_list.id,
      aws_api_gateway_integration.list_items_list.id,

      var.uri_create_list,
      var.uri_list_lists,
      var.uri_update_list,

      var.uri_create_item_list,
      var.uri_list_items_list,

      var.redeployment_trigger
    ]))
  }

  depends_on = [
    aws_api_gateway_integration.create_list,
    aws_api_gateway_integration.list_lists,
    aws_api_gateway_integration.update_list,

    aws_api_gateway_integration.create_item_list,
    aws_api_gateway_integration.list_items_list
  ]
}

resource "aws_api_gateway_stage" "stage" {
  stage_name    = var.stage_name
  rest_api_id   = aws_api_gateway_rest_api.api.id
  deployment_id = aws_api_gateway_deployment.deployment.id
}
