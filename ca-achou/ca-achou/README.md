# CA Achou? — Achados e Perdidos · Etec Camargo Aranha

Projeto em duas partes (Maven multi-módulo, Java 21):

| Módulo | O que é |
|---|---|
| `backend/` | API REST com Spring Boot 3 (JPA, Spring Security + JWT, H2 em arquivo) |
| `client/`  | Aplicativo desktop JavaFX que consome a API |

## Como executar

Requisitos: JDK 21 e Maven 3.9+.

```bash
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
  Senhas com BCrypt, sessão por JWT (12 h), fotos gravadas em disco (`data/uploads`), validação do tipo real da imagem.
- **Login por RM** (não usa e-mail). **Cadastro**: nome, sobrenome, RM, e-mail (só para contato/recuperação), curso, ano, senha.
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

| Variável | Para quê |
|---|---|
| `ACHOU_JWT_SECRET` | **Obrigatório em produção** (≥ 32 caracteres) |
| `ACHOU_DB_URL` / `ACHOU_DB_USER` / `ACHOU_DB_PASSWORD` | Trocar o H2 por PostgreSQL/MySQL (adicione o driver no `pom.xml`) |
| `ACHOU_UPLOADS_DIR` | Pasta das fotos |
| `ACHOU_MAIL_ENABLED=true` + `spring.mail.*` | Envio real do e-mail de recuperação |

Sem e-mail configurado (modo desenvolvimento), o código de recuperação é **escrito no log do servidor**.
Os cursos do cadastro ficam em `application.yml` (`achou.cursos`).

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
- O token de sessão é guardado nas preferências do usuário do sistema ("manter conectado"); "Sair" apaga.
