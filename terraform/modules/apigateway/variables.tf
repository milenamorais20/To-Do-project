variable "bucket_name" { type = string}

variable "uri_create_task" { type = string}
variable "uri_list_tasks" { type = string}
variable "uri_updtae_task" { type = string}

variable "function_create_task" { type = string}
variable "function_list_tasks" { type = string}
variable "function_updtae_task" { type = string}

variable "stage_name" {
  type    = string
  default = "v1"
}

variable "cognito_user_pool_arn" {
  description = "O ARN do Cognito User Pool para usar no autorizador"
  type = string
}

variable "redeployment_trigger" {
  description = "Um gatilho para forçar um novo deployment da API."
  type        = string
  default     = ""
}