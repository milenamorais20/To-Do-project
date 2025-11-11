variable "table_name" {
  description = "Table name"
  type        = string
}

variable "billing_mode" {
  description = ""
  type        = string
  default     = "PAY_PER_REQUEST"
}
variable "hash_key_name" {
  description = "Hash key"
  type        = string
}
variable "hash_key_type" {
  description = "Hash key type"
  type        = string
}
variable "range_key_name" {
  description = "Range key"
  type        = string
}
variable "range_key_type" {
  description = "Range key"
  type        = string
  default     = null
}
variable "tags" {
  description = "Range key type"
  type        = map(string)
  default     = {}
}