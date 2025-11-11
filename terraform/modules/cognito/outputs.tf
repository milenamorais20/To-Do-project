# pegar os ids
output "cognito_user_pool_id" {
  description = "ID do Cognito User Pool criado"
  value       = aws_cognito_user_pool.user_pool.id
}

output "cognito_user_pool_client_id" {
  description = "ID do Cognito User Pool Client. Use este ID no corpo da sua requisição."
  value       = aws_cognito_user_pool_client.user_pool_client.id
}

output "user_pool_arn" {
  description = "O ARN do Cognito User Pool criado"
  value       = aws_cognito_user_pool.user_pool.arn
}
