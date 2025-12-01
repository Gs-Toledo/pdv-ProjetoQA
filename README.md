#Links da Entrega do Trabalho:
- Relatório: https://docs.google.com/document/d/1-HTSL34HFOh3lIVD5cOovYlBIT1oY6nWF6ocYRycL1w/edit?tab=t.xf26y051cayj#heading=h.91t39drov929
- Casos de Teste: https://docs.google.com/document/d/1EmAXKb3CXfmrqRMeNc3-IYPYFGv85Mo3HZ4JMrDKnhQ/edit?tab=t.fdyl70fkar5i#heading=h.rbuoqsltif6d
- Apresentação: https://www.canva.com/design/DAG6O5a0p8U/RNZw8OJBkccf-3dfSGFBeA/edit?ui=eyJBIjp7fX0

# pdv
Sistema de ERP web desenvolvido em Java com Spring Framework

# Recursos
- Cadastro produtos/clientes/fornecedor
- Controle de estoque
- Gerenciar comandas
- Realizar venda
- Controle de fluxo de caixa
- Controle de pagar e receber
- Venda com cartões
- Gerenciar permissões de usuários por grupos
- Cadastrar novas formas de pagamentos
- Relatórios

# Tecnologias utilizadas
- Spring Framework 5
- Thymeleaf 3
- MySQL
- Hibernate
- FlyWay
- SonarQube (Análise de Código)

# Instalação (Modo Tradicional)
Para instalar o sistema manualmente, você deve criar o banco de dados "pdv" no MySQL e configurar o arquivo `application.properties` com os dados do seu usuário root do MySQL. Em seguida, rode o projeto pelo Eclipse ou gere o jar do mesmo e execute.

# Logando no sistema
Para logar no sistema, use o usuário "gerente" e a senha "123".

---

# Execução com Docker (Recomendado)

Para executar a aplicação e o banco de dados utilizando o Docker, utilize o seguinte comando na raiz do projeto:

```sh
  docker compose up 
```

O sistema estará disponível em: http://localhost:8080

# Verificação de Qualidade (SonarQube)

O projeto está configurado para rodar análise estática de código via SonarQube. Siga os passos abaixo:

## 1. Inicie o ambiente
Certifique-se de que os containers estão rodando com o comando docker compose up -d citado acima.

Nota: Aguarde cerca de 2 a 3 minutos após iniciar o Docker para que o servidor do SonarQube termine de carregar completamente.

## 2. Execute a análise

Para rodar a verificação e enviar as métricas para o servidor, execute o comando abaixo no seu terminal (na raiz do projeto):

```sh
docker compose run --rm maven-build mvn clean verify sonar:sonar \
  -DskipTests \
  -Dsonar.host.url=http://sonarqube:9000 \
  -Dsonar.login=admin \
  -Dsonar.password=admin
```

# 3. Acesse o Relatório

Após o comando finalizar com "BUILD SUCCESS", você pode visualizar o relatório completo de bugs, vulnerabilidades e code smells acessando:

URL: http://localhost:9000

    Login inicial: admin
    Senha inicial: admin

    Importante: Ao acessar o painel pela primeira vez, o SonarQube pode solicitar a troca da senha padrão.
    Se você alterar a senha no navegador, lembre-se de atualizar o parâmetro
    -Dsonar.password no comando acima nas próximas execuções.
