terraform {
  backend "s3" {
    bucket         = "lambda-bucket-todo-list-integration-tf-state"
    key            = "global/terraform.tfstate"
    region         = "us-east-1"
    dynamodb_table = "lambda-bucket-todo-list-integration-tf-locks"
    encrypt        = true
  }
}