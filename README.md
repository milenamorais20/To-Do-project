# API Serverless de Tarefas (To-Do) na AWS

Este projeto é uma API REST serverless para um aplicativo de "To-Do" (listas de tarefas), construído inteiramente no ecossistema AWS. Ele utiliza AWS Lambda com Java para o backend, API Gateway para exposição dos endpoints, e DynamoDB como banco de dados NoSQL.

O projeto também inclui um fluxo assíncrono para exportação de dados do usuário, utilizando SQS para enfileiramento, S3 para armazenamento de relatórios e SES para notificação por e-mail.

Toda a infraestrutura é gerenciada como código (IaC) usando **Terraform**.

## Funcionalidades Principais

* **Gerenciamento de Listas:** CRUD (Criar, Ler, Atualizar, Deletar) completo para Listas de Tarefas.
* **Gerenciamento de Itens:** CRUD (Criar, Ler, Atualizar, Deletar) completo para Itens *dentro* de uma lista.
* **Design de Tabela Única:** Utiliza o padrão de *Single-Table Design* no DynamoDB para modelagem de dados eficiente.
* **Exportação Assíncrona:** Um endpoint protegido que inicia um fluxo de exportação de todas as tarefas de um usuário. O relatório em CSV é gerado em segundo plano e enviado por e-mail para o usuário autenticado.

## Arquitetura e Tecnologias

Este projeto utiliza os seguintes serviços e tecnologias:

* **Backend:** Java
* **Computação:** AWS Lambda
* **API:** AWS API Gateway
* **Autenticação:** AWS Cognito (integrado ao API Gateway como Autorizador)
* **Banco de Dados:** AWS DynamoDB (com SDK v2 Enhanced Client)
* **Mensageria:** AWS SQS (para decuplar o fluxo de exportação)
* **Armazenamento:** AWS S3 (para armazenar os relatórios CSV exportados)
* **Notificação:** AWS SES (para enviar o e-mail com o CSV em anexo)
* **Infraestrutura como Código (IaC):** Terraform
* **Build:** Apache Maven
* **Testes:** JUnit 5 & Mockito

---

## Design do Banco de Dados (DynamoDB Single-Table)

O projeto utiliza um padrão de design de tabela única. Tanto as 'Listas' quanto os 'Itens' são armazenados na mesma tabela do DynamoDB, usando prefixos nas chaves de partição (PK) e ordenação (SK) para modelar os relacionamentos:

* **Lista de Tarefas:**
    * `PK: USER#<username>`
    * `SK: LIST#<uuid>`
* **Item da Lista:**
    * `PK: LIST#<uuid>` (O SK da lista pai)
    * `SK: <item_uuid>` (Um UUID aleatório para o item)

Essa modelagem permite consultas eficientes, como "buscar todas as listas de um usuário" (Query por PK) ou "buscar todos os itens de uma lista" (Query por PK).

## Fluxo de Exportação Assíncrona

Um dos principais recursos deste projeto é a exportação assíncrona de dados, que segue o fluxo abaixo para evitar timeouts no API Gateway e fornecer uma melhor experiência ao usuário:

1.  O usuário (autenticado) chama o endpoint `POST /export?pk=USER#...`.
2.  A `LambdaPostFunction` (API Gateway) valida a requisição, extrai o e-mail do usuário (injetado pelo Autorizador Cognito) e o `pk` (ID do usuário).
3.  Uma mensagem contendo `{ "pk": "...", "email": "..." }` é enviada para uma fila **SQS**.
4.  A API Gateway responde imediatamente ao usuário com `HTTP 200 (Sucesso)`.
5.  Uma `LambdaGetFunction` (SQS Trigger) consome a mensagem da fila.
6.  O handler busca *todas* as tarefas do usuário no DynamoDB usando o `pk`.
7.  Gera um arquivo **CSV** com os dados.
8.  Faz o upload do arquivo CSV para um bucket **S3**.
9.  Envia um e-mail para o usuário (via **SES**) com o CSV em anexo e uma mensagem de sucesso.

---

## Como Construir o Projeto

### Pré-requisitos

* Java (JDK 11 ou superior)
* Apache Maven
* AWS CLI
* Terraform

### 1. Construir o Artefato (JAR)

O projeto é gerenciado pelo Maven. Para compilar o código, executar os testes e empacotar o `.jar`:

```bash
# A partir do diretório raiz do projeto (todo-project)
mvn clean package
