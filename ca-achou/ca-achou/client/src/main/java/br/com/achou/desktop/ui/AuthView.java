package br.com.achou.desktop.ui;

import br.com.achou.desktop.api.Dtos;
import br.com.achou.desktop.util.Async;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

import java.util.List;
import java.util.function.Consumer;

/** Primeira página: entrar (RM + senha), criar conta e recuperar senha por código enviado ao e-mail. */
public final class AuthView extends HBox {

    private final Consumer<Dtos.AuthResponse> aoAutenticar;
    private final StackPane area = new StackPane();
    private final ToggleButton abaEntrar = new ToggleButton("Entrar");
    private final ToggleButton abaCriar = new ToggleButton("Criar conta");
    private final HBox abas = new HBox(4, abaEntrar, abaCriar);
    private final Label titulo = Ui.texto("Entrar", "auth-title");
    private final Label erro = Ui.paragrafo("", "error-text");

    private List<String> cursos = List.of();

    public AuthView(Consumer<Dtos.AuthResponse> aoAutenticar) {
        this.aoAutenticar = aoAutenticar;
        getStyleClass().add("auth-right");
        getChildren().addAll(painelMarca(), painelFormulario());
        HBox.setHgrow(getChildren().get(1), Priority.ALWAYS);

        Async.run(Ctx.api::cursos, c -> {
            cursos = c;
            if (abaCriar.isSelected()) mostrarCadastro();
        }, e -> { /* sem backend: o erro aparece ao tentar entrar */ });
        mostrarLogin();
    }

    // ------------------------------------------------------------ layout

    private Node painelMarca() {
        VBox p = new VBox(18);
        p.getStyleClass().add("auth-left");
        p.setAlignment(Pos.CENTER_LEFT);
        p.setPrefWidth(430);
        p.setMinWidth(340);
        p.setMaxWidth(470);

        HBox marca = new HBox(12, Logo.criar(56, Color.WHITE),
                new VBox(0, Ui.texto("Achou?", "brand-title"), Ui.texto("Etec Camargo Aranha", "brand-sub")));
        marca.setAlignment(Pos.CENTER_LEFT);

        Label claim = Ui.paragrafo("Perdeu algo na escola? Achou no corredor?", "auth-claim");
        Label texto = Ui.paragrafo("Poste, comente e ajude a devolver. A comunidade da Etec Camargo Aranha "
                + "colabora unida.", "auth-claim-text");

        VBox pontos = new VBox(10,
                ponto("search", "Encontre itens por categoria"),
                ponto("camera", "Publique com foto em segundos"),
                ponto("message", "Combine a devolução pelos comentários"));
        pontos.setPadding(new Insets(10, 0, 0, 0));

        p.getChildren().addAll(marca, Ui.espaco(), claim, texto, pontos, Ui.espaco());
        return p;
    }

    private Node painelFormulario() {
        abaEntrar.getStyleClass().add("auth-tab");
        abaCriar.getStyleClass().add("auth-tab");
        ToggleGroup g = new ToggleGroup();
        abaEntrar.setToggleGroup(g);
        abaCriar.setToggleGroup(g);
        abaEntrar.setSelected(true);
        g.selectedToggleProperty().addListener((o, antigo, novo) -> {
            if (novo == null) {
                antigo.setSelected(true);
            } else if (novo == abaEntrar) {
                mostrarLogin();
            } else {
                mostrarCadastro();
            }
        });
        abas.getStyleClass().add("auth-tabs");
        abas.setAlignment(Pos.CENTER);
        abaEntrar.setMaxWidth(Double.MAX_VALUE);
        abaCriar.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(abaEntrar, Priority.ALWAYS);
        HBox.setHgrow(abaCriar, Priority.ALWAYS);

        erro.setManaged(false);
        erro.setVisible(false);

        VBox card = new VBox(16, titulo, abas, area, erro);
        card.getStyleClass().add("auth-card");
        card.setMaxWidth(440);
        card.setMinWidth(320);

        ScrollPane sp = new ScrollPane(new StackPane(card));
        ((StackPane) sp.getContent()).setPadding(new Insets(32));
        ((StackPane) sp.getContent()).setAlignment(Pos.CENTER);
        sp.setFitToWidth(true);
        sp.setFitToHeight(true);
        sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        sp.getStyleClass().add("page");
        return sp;
    }

    private Node ponto(String icone, String texto) {
        HBox h = new HBox(10, Icons.de(icone, 18, "icon-white"), Ui.texto(texto, "auth-point"));
        h.setAlignment(Pos.CENTER_LEFT);
        return h;
    }

    private void mostrarErro(String msg) {
        boolean tem = msg != null && !msg.isBlank();
        erro.setText(tem ? msg : "");
        erro.setManaged(tem);
        erro.setVisible(tem);
    }

    private void trocar(String tituloTexto, boolean comAbas, Node conteudo) {
        titulo.setText(tituloTexto);
        abas.setManaged(comAbas);
        abas.setVisible(comAbas);
        mostrarErro(null);
        area.getChildren().setAll(conteudo);
    }

    // ------------------------------------------------------------ entrar

    private void mostrarLogin() {
        TextField rm = Ui.entrada("Seu RM da Etec (ex.: 12345)");
        Ui.somenteDigitos(rm, 8);
        PasswordField senha = Ui.senha("Sua senha");

        Button entrar = Ui.botao("Entrar", "primary", null, null);
        entrar.setMaxWidth(Double.MAX_VALUE);
        Runnable enviar = () -> {
            if (rm.getText().isBlank() || senha.getText().isEmpty()) {
                mostrarErro("Informe seu RM e sua senha.");
                return;
            }
            mostrarErro(null);
            Ui.ocupado(entrar, true, "Entrar");
            Async.run(() -> Ctx.api.login(rm.getText().trim(), senha.getText()), aoAutenticar, t -> {
                Ui.ocupado(entrar, false, "Entrar");
                mostrarErro(t.getMessage());
            });
        };
        entrar.setOnAction(e -> enviar.run());
        senha.setOnAction(e -> enviar.run());
        rm.setOnAction(e -> senha.requestFocus());

        Hyperlink esqueci = new Hyperlink("Esqueci minha senha");
        esqueci.getStyleClass().add("link");
        esqueci.setOnAction(e -> mostrarEsqueci(rm.getText()));

        VBox form = new VBox(14,
                Ui.campo("RM", rm), Ui.campo("Senha", senha), entrar, esqueci);
        form.setAlignment(Pos.CENTER_LEFT);
        trocar("Entrar no CA Achou?", true, form);
        rm.requestFocus();
    }

    // ------------------------------------------------------------ cadastro

    private void mostrarCadastro() {
        TextField nome = Ui.entrada("Ex.: Ana");
        Ui.limitar(nome, 40);
        TextField sobrenome = Ui.entrada("Ex.: Souza");
        Ui.limitar(sobrenome, 60);
        TextField rm = Ui.entrada("Número de RM (o mesmo do NSA)");
        Ui.somenteDigitos(rm, 8);
        TextField email = Ui.entrada("seu.email@exemplo.com");
        Ui.limitar(email, 120);

        ComboBox<String> curso = new ComboBox<>();
        curso.getItems().addAll(cursos);
        curso.setEditable(cursos.isEmpty());
        curso.setPromptText("Selecione seu curso");
        curso.setMaxWidth(Double.MAX_VALUE);

        ComboBox<Integer> ano = new ComboBox<>();
        ano.getItems().addAll(1, 2, 3, 4);
        ano.setPromptText("Ano");
        ano.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(Integer i) { return i == null ? "" : i + "º ano"; }
            @Override public Integer fromString(String s) { return null; }
        });
        ano.setMaxWidth(Double.MAX_VALUE);

        PasswordField senha = Ui.senha("Mínimo de 6 caracteres");
        PasswordField confirma = Ui.senha("Repita a senha");

        Button criar = Ui.botao("Criar conta", "primary", null, null);
        criar.setMaxWidth(Double.MAX_VALUE);
        criar.setOnAction(e -> {
            String c = curso.isEditable() ? curso.getEditor().getText() : curso.getValue();
            String msg = validarCadastro(nome, sobrenome, rm, email, c, ano.getValue(), senha, confirma);
            if (msg != null) {
                mostrarErro(msg);
                return;
            }
            mostrarErro(null);
            Ui.ocupado(criar, true, "Criar conta");
            var req = new Dtos.Cadastro(nome.getText().trim(), sobrenome.getText().trim(), rm.getText().trim(),
                    email.getText().trim(), c.trim(), ano.getValue(), senha.getText());
            Async.run(() -> Ctx.api.cadastrar(req), aoAutenticar, t -> {
                Ui.ocupado(criar, false, "Criar conta");
                mostrarErro(t.getMessage());
            });
        });

        HBox linhaNome = new HBox(12, Ui.campo("Nome", nome), Ui.campo("Sobrenome", sobrenome));
        linhaNome.getChildren().forEach(n -> HBox.setHgrow(n, Priority.ALWAYS));
        VBox colCurso = Ui.campo("Curso", curso);
        VBox colAno = Ui.campo("Ano", ano);
        HBox.setHgrow(colCurso, Priority.ALWAYS);
        colAno.setMinWidth(110);
        HBox linhaCurso = new HBox(12, colCurso, colAno);

        Label aviso = Ui.paragrafo("O e-mail é usado somente para recuperar sua senha. O login é feito com o RM.",
                "muted", "small");

        VBox form = new VBox(12, linhaNome, Ui.campo("RM", rm), linhaCurso,
                Ui.campo("E-mail (para recuperação de senha)", email), aviso,
                Ui.campo("Senha", senha), Ui.campo("Confirmar senha", confirma), criar);
        trocar("Criar conta no CA Achou?", true, form);
        nome.requestFocus();
    }

    private static String validarCadastro(TextField nome, TextField sobrenome, TextField rm, TextField email,
                                          String curso, Integer ano, PasswordField senha, PasswordField confirma) {
        if (nome.getText().isBlank()) return "Informe seu nome.";
        if (sobrenome.getText().isBlank()) return "Informe seu sobrenome.";
        if (!rm.getText().matches("\\d{4,8}")) return "O RM deve ter de 4 a 8 dígitos.";
        if (curso == null || curso.isBlank()) return "Selecione seu curso.";
        if (ano == null) return "Selecione o ano que você cursa.";
        if (!email.getText().trim().matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")) return "Informe um e-mail válido.";
        if (senha.getText().length() < 6) return "A senha deve ter pelo menos 6 caracteres.";
        if (!senha.getText().equals(confirma.getText())) return "As senhas não coincidem.";
        return null;
    }

    // ------------------------------------------------------------ esqueci a senha

    private void mostrarEsqueci(String rmInicial) {
        TextField rm = Ui.entrada("Seu RM");
        Ui.somenteDigitos(rm, 8);
        rm.setText(rmInicial == null ? "" : rmInicial);

        Button enviar = Ui.botao("Enviar código", "primary", "mail", null);
        enviar.setMaxWidth(Double.MAX_VALUE);
        enviar.setOnAction(e -> {
            if (rm.getText().isBlank()) {
                mostrarErro("Informe seu RM.");
                return;
            }
            mostrarErro(null);
            Ui.ocupado(enviar, true, "Enviar código");
            Async.run(() -> Ctx.api.esqueciSenha(rm.getText().trim()), r -> {
                Ctx.toast(r.mensagem());
                mostrarRedefinir(rm.getText().trim());
            }, t -> {
                Ui.ocupado(enviar, false, "Enviar código");
                mostrarErro(t.getMessage());
            });
        });
        rm.setOnAction(e -> enviar.fire());

        Button temCodigo = Ui.botao("Já tenho um código", "ghost", null,
                () -> mostrarRedefinir(rm.getText().trim()));
        temCodigo.setMaxWidth(Double.MAX_VALUE);

        VBox form = new VBox(14,
                Ui.paragrafo("Informe seu RM. Enviaremos um código de 6 dígitos para o e-mail que você "
                        + "cadastrou.", "muted"),
                Ui.campo("RM", rm), enviar, temCodigo, voltar());
        trocar("Recuperar senha", false, form);
        rm.requestFocus();
    }

    private void mostrarRedefinir(String rmInicial) {
        TextField rm = Ui.entrada("Seu RM");
        Ui.somenteDigitos(rm, 8);
        rm.setText(rmInicial == null ? "" : rmInicial);
        TextField codigo = Ui.entrada("Código de 6 dígitos");
        Ui.somenteDigitos(codigo, 6);
        PasswordField nova = Ui.senha("Mínimo de 6 caracteres");
        PasswordField confirma = Ui.senha("Repita a nova senha");

        Button salvar = Ui.botao("Redefinir senha", "primary", "lock", null);
        salvar.setMaxWidth(Double.MAX_VALUE);
        salvar.setOnAction(e -> {
            if (rm.getText().isBlank() || !codigo.getText().matches("\\d{6}")) {
                mostrarErro("Informe seu RM e o código de 6 dígitos.");
                return;
            }
            if (nova.getText().length() < 6) {
                mostrarErro("A nova senha deve ter pelo menos 6 caracteres.");
                return;
            }
            if (!nova.getText().equals(confirma.getText())) {
                mostrarErro("As senhas não coincidem.");
                return;
            }
            mostrarErro(null);
            Ui.ocupado(salvar, true, "Redefinir senha");
            Async.run(() -> Ctx.api.redefinirSenha(rm.getText().trim(), codigo.getText(), nova.getText()), r -> {
                Ctx.toast(r.mensagem());
                abaEntrar.setSelected(true);
                mostrarLogin();
            }, t -> {
                Ui.ocupado(salvar, false, "Redefinir senha");
                mostrarErro(t.getMessage());
            });
        });

        Hyperlink reenviar = new Hyperlink("Não recebi o código");
        reenviar.getStyleClass().add("link");
        reenviar.setOnAction(e -> mostrarEsqueci(rm.getText()));

        VBox form = new VBox(12,
                Ui.paragrafo("Digite o código enviado ao seu e-mail (vale por 15 minutos) e escolha uma nova "
                        + "senha.", "muted"),
                Ui.campo("RM", rm), Ui.campo("Código", codigo),
                Ui.campo("Nova senha", nova), Ui.campo("Confirmar nova senha", confirma),
                salvar, reenviar, voltar());
        trocar("Nova senha", false, form);
        codigo.requestFocus();
    }

    private Node voltar() {
        Hyperlink l = new Hyperlink("← Voltar para o login");
        l.getStyleClass().add("link");
        l.setOnAction(e -> {
            abaEntrar.setSelected(true);
            mostrarLogin();
        });
        return l;
    }
}
