output "lambda_function_arn" {
  description = "O ARN da função Lambda"
  value = aws_lambda_function.this.arn
}

output "lambda_function_name" {
  description = "O nome da função Lambda criada"
  value = aws_lambda_function.this.function_name
}

output "iam_role_arn" {
  description = "O ARN do IAM Role criado para a Lambda"
  value = aws_iam_role.lambda_exec_role.arn
}
output "iam_role_name" {
  description = "O nome do IAM Role criado para a Lambda"
  value = aws_iam_role.lambda_exec_role.name
}