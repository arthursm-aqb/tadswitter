# Tadswitter

Com o Docker instalado e na pasta do projeto, execute:

```powershell
docker compose up --build
```

Quando os três serviços terminarem de iniciar, abra **http://localhost:8080**. Para encerrar, pressione `Ctrl+C` e execute:

```powershell
docker compose down
```

Os bancos ficam preservados em volumes do Docker. Para apagar também os dados cadastrados, use `docker compose down -v`.

| Processo | Responsabilidade | Porta |
| --- | --- | --- |
| `usuarios-api` | Cadastro, autenticação e entidade `Usuario` | 8081 |
| `board-api` | Entidades `Postagem` e `Comentario`; salva e ordena dados | 8082 |
| `gateway` | JWT, HATEOAS, Swagger, cliente web e entrada única | 8080 |


## Rotas do Gateway

| Método | Rota | Uso |
| --- | --- | --- |
| POST | `/api/auth/cadastro` | `{ "nome": "Ana", "login": "ana", "senha": "123456" }` |
| POST | `/api/auth/login` | `{ "login": "ana", "senha": "123456" }` → JWT |
| GET | `/api/postagens` | Todas as postagens e seus comentários |
| GET | `/api/postagens/{id}` | Uma postagem |
| POST | `/api/postagens` | `{ "titulo": "Assunto", "texto": "Olá!" }` |
| POST | `/api/postagens/{id}/comentarios` | `{ "texto": "Resposta" }` |

https://canva.link/zdwk4qwd41zyi4i
