# CA Achou? — Achados e Perdidos · Etec Camargo Aranha

Projeto em duas partes (Maven multi-módulo, Java 21):

| Módulo | O que é |
|---|---|
| `backend/` | API REST com Spring Boot 3 (JPA, Spring Security + JWT, **PostgreSQL**) |
| `client/`  | Aplicativo desktop JavaFX que consome a API |

## Como executar

Requisitos: JDK 21 e Maven 3.9+ (e Docker, se for usar o Postgres local do `docker-compose.yml`).

```bash
# 0) Banco PostgreSQL local (usuário/senha/banco "achou", porta 5432)
docker compose up -d
#    Sem Docker/Postgres? Rode o backend com o perfil de desenvolvimento em H2:
#    mvn spring-boot:run -Dspring-boot.run.profiles=h2

# 1) Backend (porta 8080)
cd backend
mvn spring-boot:run

# 2) Cliente (em outro terminal)
cd client
mvn javafx:run
# outro servidor:  mvn javafx:run -Dachou.api=http://192.168.0.10:8080
```

Testes do backend: `cd backend && mvn test`.

## O que mudou em relação ao HTML original

- **Backend Java**: tudo que ficava no `localStorage` (usuários, posts, comentários, fotos) agora está no banco via API.
  Senhas com BCrypt, sessão por JWT (12 h), fotos gravadas **no banco** (tabela `arquivos`), validação do tipo real da imagem.
- **Login por RM** (não usa e-mail). **Cadastro**: nome, sobrenome, RM, e-mail (só para contato/recuperação), curso, ano, **turno** (Manhã, Tarde, Noite ou Integral), senha.
  O turno aparece no Perfil e pode ser editado lá. Contas antigas, sem turno, mostram "Turno não informado" e precisam escolher ao salvar o perfil.
  Edite a lista em `backend/.../domain/Turno.java` (e no espelho `Dtos.Turno` do cliente).
- **Esqueci minha senha**: o aluno informa o RM → recebe um código de 6 dígitos no e-mail cadastrado (vale 15 min,
  máx. 5 tentativas, guardado só como hash) → define a nova senha. A resposta não revela se o RM existe.
- **Categorias e tipos de item** (filtro no feed e campo obrigatório ao publicar): Eletrônicos (fones, celular, carregador…),
  Acessórios (pulseira, corrente, anel…), Materiais escolares (caderno, agenda, estojo…), Mochilas e bolsas,
  Garrafas e marmitas, Roupas e uniformes, Documentos e chaves, Outros. Cada categoria tem subtipos e "Outro".
  Edite a lista em `backend/.../domain/Categoria.java`.
- **Ajustes**: foto de perfil (escolher/trocar/remover, recorte quadrado automático) e nome + sobrenome + turma exibidos;
  também no cabeçalho, no Perfil e em cada publicação/comentário. Temas Automático / Claro / Escuro / Preto OLED mantidos.
- **Design**: mesma paleta (`#1b3057`, `#e00008`, `#8099c8`), logotipo da lupa e tipografia itálica; cartões com sombras suaves,
  ícones vetoriais, chips de filtro com rolagem, tela de entrada em duas colunas, avisos (toasts), paginação do feed.

## Configuração (variáveis de ambiente do backend)

Modelo completo em `.env.example`. As principais:

| Variável | Para quê |
|---|---|
| `ACHOU_JWT_SECRET` | **Obrigatório em produção** (≥ 32 caracteres) |
| `ACHOU_DB_HOST` `ACHOU_DB_PORT` `ACHOU_DB_NAME` `ACHOU_DB_USER` `ACHOU_DB_PASSWORD` | Conexão com o PostgreSQL (padrões = banco local do `docker-compose.yml`) |
| `ACHOU_DB_SSLMODE` | `require` na maioria dos bancos em nuvem (padrão: `prefer`) |
| `ACHOU_DB_URL` | Alternativa às partes acima: URL JDBC completa (`jdbc:postgresql://host:5432/banco?sslmode=require`) |
| `ACHOU_MAIL_ENABLED=true` + `ACHOU_MAIL_HOST/PORT/USER/PASSWORD/FROM` | Envio real do e-mail de recuperação (SMTP) |
| `PORT` | Porta HTTP (a nuvem define sozinha; padrão 8080) |

Sem e-mail configurado (modo desenvolvimento), o código de recuperação é **escrito no log do servidor**.
Se `ACHOU_MAIL_ENABLED=true` sem `ACHOU_MAIL_HOST`, o backend **recusa subir** com uma mensagem explicando o que falta.
Os cursos do cadastro ficam em `application.yml` (`achou.cursos`).

### E-mail (recuperação de senha)

Exemplo com Gmail (para testar): ative a verificação em duas etapas na conta Google, crie uma **senha de app** e use:

```
ACHOU_MAIL_ENABLED=true
ACHOU_MAIL_HOST=smtp.gmail.com
ACHOU_MAIL_PORT=587
ACHOU_MAIL_USER=seu.email@gmail.com
ACHOU_MAIL_PASSWORD=<senha de app de 16 letras>
ACHOU_MAIL_FROM=seu.email@gmail.com
```

Para a porta 465 (SSL direto) use também `ACHOU_MAIL_SSL=true` e `ACHOU_MAIL_STARTTLS=false`.
O envio é assíncrono: se falhar (senha errada, porta bloqueada), o motivo aparece **no log do servidor**; a tela sempre mostra a mesma mensagem genérica de propósito (não revelar se o RM existe).

## Publicar na nuvem

Arquitetura: **cliente JavaFX → HTTPS → API (container Docker) → PostgreSQL gerenciado.**

1. **Banco**: crie um PostgreSQL gerenciado (Neon, Supabase, Render, etc.) e anote host, banco, usuário e senha.
   Confira os limites do plano gratuito: o Postgres gratuito da Render, por exemplo, expira 30 dias após a criação.
   As tabelas são criadas sozinhas na primeira subida (`ddl-auto: update`).
2. **API**: o `backend/Dockerfile` roda em qualquer nuvem com Docker. Na Render, o `render.yaml` (na raiz do repositório)
   cria o serviço: *New → Blueprint* e informe as variáveis marcadas no painel.
3. **Variáveis**: `ACHOU_DB_*` (com `ACHOU_DB_SSLMODE=require`), `ACHOU_JWT_SECRET` e as de e-mail, conforme a tabela acima.
4. **Cliente**: aponte para a URL pública: `mvn javafx:run -Dachou.api=https://SEU-SERVICO.onrender.com`
   (ou a variável `ACHOU_API`).

Limites do plano gratuito da Render que afetam este projeto (confira a página de planos, pois mudam):

- Serviço gratuito **dorme após 15 min sem tráfego** e leva ~1 min para acordar: a primeira ação depois disso pode dar timeout no cliente (conexão 8 s / resposta 20 s).
- **SMTP nas portas 25, 465 e 587 é bloqueado** em serviços gratuitos: use um provedor SMTP com porta alternativa ou um plano pago.
- **Sem disco persistente**: por isso as fotos ficam no PostgreSQL e não em arquivos.

## Principais endpoints

```
POST /api/auth/cadastro | /login | /esqueci-senha | /redefinir-senha      GET /api/categorias | /api/cursos   (públicos)
GET/PUT /api/me   PUT /api/me/senha   PUT/DELETE /api/me/foto   GET /api/me/estatisticas
GET /api/publicacoes?tipo=&categoria=&subcategoria=&q=&somenteAbertos=&minhas=&pagina=&tamanho=
POST /api/publicacoes   PUT /api/publicacoes/{id}/foto   PATCH /api/publicacoes/{id}/devolvido   DELETE /api/publicacoes/{id}
GET/POST /api/publicacoes/{id}/comentarios        GET /api/arquivos/{nome}  (fotos, exige login)
```

## Observações / próximos passos

- O RM não é validado contra o NSA (não há integração); qualquer RM com 4–8 dígitos pode se cadastrar, uma vez por RM.
  Se a escola quiser garantir que só alunos reais entrem, é preciso uma lista de RMs ou integração oficial.
- O bloqueio de tentativas de login fica em memória (uma instância). Em cluster, mover para banco/Redis.
- O esquema do banco é mantido pelo Hibernate (`ddl-auto: update`), que não altera constraints já existentes; para evoluir o esquema com segurança, o próximo passo é adotar migrações (Flyway).
- Fotos no PostgreSQL funcionam bem para a escala de uma escola; para volume grande, o caminho é armazenamento de objetos (S3 e similares).
- O token de sessão é guardado nas preferências do usuário do sistema ("manter conectado"); "Sair" apaga.
