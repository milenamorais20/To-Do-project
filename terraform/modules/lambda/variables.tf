variable "function_name" {
  description = "Nome da função Lambda"
  type        = string
}

variable "handler" {
  description = "O handler da função Lambda"
  type        = string
}
variable "runtime" {
  description = "O runtime da função Lambda"
  type        = string
}
variable "source_code_path" {
  description = "Caminho para o arquivo .jar do código da Lambda"
  type        = string
}

variable "memory_size" {
  description = "Tamanho da memória para a função Lambda em MB"
  type        = number
  default     = 512
}

variable "timeout" {
  description = "Tempo máximo de execução da função Lambda em segundos"
  type        = number
  default     = 10
}
variable "tags" {
  description = "Um mapa de tags para aplicar aos recursos"
  type        = map(string)
  default     = {}
}
variable "tasks_table_name" {
  description = "O nome da tabela DynamoDB a ser usada pela função."
  type        = string
  default     = ""
}
variable "environment_variables" {
  description = "Um mapa de variáveis de ambiente para a função Lambda."
  type        = map(string)
  default     = {}
}