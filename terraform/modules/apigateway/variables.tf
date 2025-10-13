variable "bucket_name" { type = string}

variable "uri_create_list" { type = string}
variable "uri_list_lists" { type = string}
variable "uri_update_list" { type = string}

variable "uri_create_item_list" { type = string}
variable "uri_list_items_list" { type = string}
variable "uri_update_item_list" { type = string}

variable "function_create_list" { type = string}
variable "function_list_lists" { type = string}
variable "function_update_list" { type = string}

variable "function_create_item_list" { type = string}
variable "function_list_items_list" { type = string}
variable "function_update_item_list" { type = string}


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