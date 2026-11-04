package br.com.achou.desktop.ui;

import br.com.achou.desktop.api.Dtos;
import br.com.achou.desktop.util.Async;
import br.com.achou.desktop.util.Fmt;
import javafx.geometry.Pos;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.Button;
import javafx.scene.layout.*;

import java.util.List;

/** Perfil: foto, nome, turma, estatísticas e edição dos dados. */
public final class ProfileView extends ScrollPane {

    private final VBox conteudo = new VBox(18);

    public ProfileView() {
        conteudo.setMaxWidth(820);
        StackPane suporte = new StackPane(conteudo);
        suporte.setAlignment(Pos.TOP_CENTER);
        suporte.setPadding(new javafx.geometry.Insets(22, 24, 40, 24));
        setContent(suporte);
        setFitToWidth(true);
        setHbarPolicy(ScrollBarPolicy.NEVER);
        getStyleClass().add("page");
        Ctx.usuario.addListener((o, a, b) -> atualizar());
        atualizar();
    }

    public void atualizar() {
        Dtos.Usuario u = Ctx.usuario.get();
        conteudo.getChildren().clear();
        if (u == null) return;

        Avatar av = new Avatar(88);
        av.definir(u.nomeCompleto(), u.fotoUrl());
        HBox topo = new HBox(18, av, new VBox(2,
                Ui.texto(u.nomeCompleto(), "title-xl"),
                Ui.texto(u.turma() + " · RM " + u.rm(), "muted"),
                Ui.texto("Membro desde " + Fmt.data(u.criadoEm()), "muted", "small")));
        topo.setAlignment(Pos.CENTER_LEFT);

        HBox stats = new HBox(10);
        conteudo.getChildren().addAll(topo, stats, editar(u));
        Async.run(Ctx.api::estatisticas, e -> {
            stats.getChildren().setAll(
                    stat(e.publicacoes(), "Publicações"), stat(e.perdidos(), "Perdidos"),
                    stat(e.achados(), "Achados"), stat(e.devolvidos(), "Devolvidos"),
                    stat(e.comentarios(), "Comentários"));
            stats.getChildren().forEach(n -> HBox.setHgrow(n, Priority.ALWAYS));
        });
    }

    private static VBox stat(long n, String rotulo) {
        VBox v = new VBox(0, Ui.texto(String.valueOf(n), "stat-num"), Ui.texto(rotulo, "stat-label"));
        v.getStyleClass().add("stat");
        v.setAlignment(Pos.CENTER);
        v.setMaxWidth(Double.MAX_VALUE);
        return v;
    }

    private VBox editar(Dtos.Usuario u) {
        VBox box = Ui.caixa("Editar perfil", "Mantenha seus dados atualizados. O RM não pode ser alterado.");
        TextField nome = Ui.entrada("Nome");
        nome.setText(u.nome());
        Ui.limitar(nome, 40);
        TextField sobrenome = Ui.entrada("Sobrenome");
        sobrenome.setText(u.sobrenome());
        Ui.limitar(sobrenome, 60);
        TextField email = Ui.entrada("E-mail");
        email.setText(u.email());
        ComboBox<String> curso = new ComboBox<>();
        curso.setEditable(true);
        curso.setValue(u.curso());
        curso.setMaxWidth(Double.MAX_VALUE);
        Async.run(Ctx.api::cursos, lista -> {
            String atual = curso.getEditor().getText();
            curso.getItems().setAll(lista);
            curso.getEditor().setText(atual);
        }, t -> { });
        ComboBox<Integer> ano = new ComboBox<>();
        ano.getItems().addAll(1, 2, 3, 4);
        ano.setValue(u.ano());
        ano.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(Integer i) { return i == null ? "" : i + "º ano"; }
            @Override public Integer fromString(String s) { return null; }
        });
        ano.setMaxWidth(Double.MAX_VALUE);

        HBox l1 = dois(Ui.campo("Nome", nome), Ui.campo("Sobrenome", sobrenome));
        VBox cCurso = Ui.campo("Curso", curso);
        VBox cAno = Ui.campo("Ano", ano);
        HBox.setHgrow(cCurso, Priority.ALWAYS);
        HBox l2 = new HBox(12, cCurso, cAno);

        Button salvar = Ui.botao("Salvar alterações", "primary", null, null);
        salvar.setOnAction(e -> {
            String c = curso.getEditor().getText().trim();
            if (nome.getText().isBlank() || sobrenome.getText().isBlank() || c.isEmpty()
                    || !email.getText().trim().matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+") || ano.getValue() == null) {
                Ctx.toast("Confira nome, sobrenome, e-mail, curso e ano.");
                return;
            }
            Ui.ocupado(salvar, true, "Salvar alterações");
            var req = new Dtos.AtualizarPerfil(nome.getText().trim(), sobrenome.getText().trim(),
                    email.getText().trim(), c, ano.getValue());
            Async.run(() -> Ctx.api.atualizarPerfil(req), novo -> {
                Ctx.usuario.set(novo);
                Ctx.atualizarFeed.run();
                Ctx.toast("Perfil atualizado.");
            }, t -> {
                Ui.ocupado(salvar, false, "Salvar alterações");
                Ctx.erro(t);
            });
        });
        box.getChildren().addAll(l1, l2, Ui.campo("E-mail (recuperação de senha)", email), salvar);
        return box;
    }

    private static HBox dois(VBox a, VBox b) {
        HBox h = new HBox(12, a, b);
        HBox.setHgrow(a, Priority.ALWAYS);
        HBox.setHgrow(b, Priority.ALWAYS);
        a.setPrefWidth(10);
        b.setPrefWidth(10);
        return h;
    }
}
