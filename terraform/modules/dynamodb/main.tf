resource "aws_dynamodb_table" "this" {
  name = var.table_name
  billing_mode = var.billing_mode
  hash_key     = var.hash_key_name
  range_key = var.range_key_name

  attribute {
    name = var.hash_key_name
    type = var.hash_key_type
  }
  dynamic "attribute" {
    for_each = var.range_key_name != null ?[1] : []
    content {
      name = var.range_key_name
      type = var.range_key_type
    }
  }
  tags = var.tags
}