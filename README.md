# Tadswitter

Projeto acadêmico de board com login, postagens e comentários. O cliente conversa somente com o **Gateway**; ele valida o JWT e chama duas APIs internas em processos separados.

```text
Navegador ──HTTP:8080──> Gateway ──HTTP:8081──> usuarios-api ──> H2 usuarios
                           │
                           └────HTTP:8082──> board-api ────> H2 board
```

## Como executar

Requer **Java 17 ou superior**. Na pasta do projeto, no PowerShell:

```powershell
.\mvnw.cmd -DskipTests package
.\iniciar.ps1
```

Abra **http://localhost:8080**. Crie uma conta na própria tela e entre. A documentação interativa fica em **http://localhost:8080/swagger-ui.html**. Para encerrar os três processos, execute `./parar.ps1` no PowerShell.

Se o PowerShell impedir scripts, execute `powershell -ExecutionPolicy Bypass -File .\iniciar.ps1` (e o equivalente para `parar.ps1`). O script cria a pasta `dados/`, onde ficam os dois bancos H2 e os registros da execução. Ela é ignorada pelo Git.

## Apresentação no laboratório

1. Copie ou clone o projeto na máquina que será o servidor, instale Java 17+ e execute os dois comandos acima.
2. Descubra o IP dessa máquina com `ipconfig` e libere a porta TCP **8080** no firewall do laboratório, se necessário.
3. Na máquina principal, abra `http://IP-DO-SERVIDOR:8080`. Cadastro, login, board e Swagger estão nesse mesmo endereço. As APIs internas escutam somente em `127.0.0.1` nas portas 8081 e 8082.
4. Demonstre: cadastrar usuário → login → ver a lista de postagens e a contagem de comentários → abrir uma postagem → comentar → criar nova postagem → abrir Swagger. No Swagger, use o token retornado em `POST /api/auth/login` no botão **Authorize** para testar as rotas protegidas.

## Organização

Cada API tem pastas `controller`, `model`, `repository` e `service`. O gateway tem `controller`, `model` e `service`, porque não possui banco próprio: acessa as APIs internas por HTTP. O cliente HTML/CSS/JS está em `gateway/src/main/resources/static`.

| Processo | Responsabilidade | Porta |
| --- | --- | --- |
| `usuarios-api` | Cadastro, autenticação e entidade `Usuario` | 8081 |
| `board-api` | Entidades `Postagem` e `Comentario`; salva e ordena dados | 8082 |
| `gateway` | JWT, HATEOAS, Swagger, cliente web e entrada única | 8080 |

O gateway assina JWT com HMAC-SHA256, válido por 8 horas. Nas rotas protegidas, envie `Authorization: Bearer <token>`. Para alterar a chave antes da apresentação, defina a variável de ambiente `JWT_SECRET` com **pelo menos 32 caracteres** antes de iniciar o gateway. A senha do usuário é armazenada como hash BCrypt. As respostas da board incluem `_links` (por exemplo, `self`, `comentar`, `board`), que demonstram HATEOAS. Na tela inicial, as postagens mais recentes aparecem primeiro com sua contagem de comentários; ao abrir uma postagem, os comentários aparecem do mais antigo ao mais recente.

## Rotas do Gateway

| Método | Rota | Uso |
| --- | --- | --- |
| POST | `/api/auth/cadastro` | `{ "nome": "Ana", "login": "ana", "senha": "123456" }` |
| POST | `/api/auth/login` | `{ "login": "ana", "senha": "123456" }` → JWT |
| GET | `/api/postagens` | Todas as postagens e seus comentários |
| GET | `/api/postagens/{id}` | Uma postagem |
| POST | `/api/postagens` | `{ "texto": "Olá!" }` |
| POST | `/api/postagens/{id}/comentarios` | `{ "texto": "Resposta" }` |

As quatro últimas rotas exigem JWT. O Swagger gera também o contrato OpenAPI em `/v3/api-docs`.

## GitHub

Depois de criar um repositório vazio no GitHub, execute:

```powershell
git init
git add .
git commit -m "Projeto acadêmico Tadswitter"
git branch -M main
git remote add origin https://github.com/SEU-USUARIO/tadswitter.git
git push -u origin main
```

Compartilhe a URL do repositório com o professor.
