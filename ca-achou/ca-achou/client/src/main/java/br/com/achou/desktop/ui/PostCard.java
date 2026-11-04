package br.com.achou.desktop.ui;

import br.com.achou.desktop.api.Dtos;
import br.com.achou.desktop.util.Async;
import br.com.achou.desktop.util.Fmt;
import br.com.achou.desktop.util.Imagens;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;

import java.util.List;

/** Cartão de uma publicação (perdi/achei) com foto, categoria, comentários e ações. */
public final class PostCard extends VBox {

    private Dtos.Publicacao p;
    private final Runnable aoMudar;
    private final VBox comentarios = new VBox(8);
    private final Button btnComentarios;
    private boolean carregados;

    public PostCard(Dtos.Publicacao publicacao, Runnable aoMudar) {
        this.p = publicacao;
        this.aoMudar = aoMudar;
        getStyleClass().add("post");

        btnComentarios = Ui.botao("Comentários (" + p.comentarios() + ")", "ghost", "message", this::alternarComentarios);
        btnComentarios.getStyleClass().setAll("btn-text");
        btnComentarios.setGraphic(Icons.de("message", 16, "icon-accent"));

        comentarios.getStyleClass().add("comments");
        comentarios.setManaged(false);
        comentarios.setVisible(false);

        getChildren().addAll(cabecalho(), foto(), corpo(), acoes(), comentarios);
    }

    // ------------------------------------------------------------ partes

    private HBox cabecalho() {
        Avatar av = new Avatar(42);
        av.definir(p.autor().nomeCompleto(), p.autor().fotoUrl());

        Label nome = Ui.texto(p.autor().nomeCompleto(), "post-author");
        Label sub = Ui.texto(p.autor().turma() + " · " + Fmt.ago(p.criadoEm()), "muted", "small");

        Label tag = new Label(p.devolvido() ? "DEVOLVIDO" : "perdi".equalsIgnoreCase(p.tipo()) ? "PERDEU" : "ACHOU");
        tag.getStyleClass().addAll("tag", p.devolvido() ? "tag-devolvido" : "perdi".equalsIgnoreCase(p.tipo()) ? "tag-perdi" : "tag-achei");

        HBox h = new HBox(12, av, new VBox(1, nome, sub), Ui.espaco(), tag);
        h.getStyleClass().add("post-head");
        h.setAlignment(Pos.CENTER_LEFT);
        return h;
    }

    private StackPane foto() {
        StackPane box = new StackPane();
        if (p.fotoUrl() == null) {
            box.getStyleClass().add("post-no-photo");
            VBox vazio = new VBox(6, Icons.de("image", 26, "icon-mute"), Ui.texto("Sem foto cadastrada", "muted", "small"));
            vazio.setAlignment(Pos.CENTER);
            box.getChildren().add(vazio);
            return box;
        }
        box.getStyleClass().add("post-photo-wrap");
        box.setMinHeight(180);
        ImageView iv = new ImageView();
        iv.setPreserveRatio(true);
        iv.setSmooth(true);
        iv.setFitHeight(380);
        box.getChildren().add(iv);
        Imagens.carregar(p.fotoUrl(), 900, img -> {
            iv.setImage(img);
            box.setMinHeight(Region.USE_COMPUTED_SIZE);
        });
        return box;
    }

    private VBox corpo() {
        VBox v = new VBox(8);
        v.getStyleClass().add("post-body");
        v.getChildren().add(Ui.paragrafo(p.titulo(), "post-title"));
        if (p.descricao() != null && !p.descricao().isBlank()) {
            v.getChildren().add(Ui.paragrafo(p.descricao(), "muted"));
        }
        String categoria = p.subcategoria() == null ? p.categoriaRotulo() : p.categoriaRotulo() + " · " + p.subcategoria();
        FlowPane chips = new FlowPane(8, 8, chip("pin", p.local()), chip("tag", categoria));
        v.getChildren().add(chips);
        return v;
    }

    private static Label chip(String icone, String texto) {
        Label l = new Label(texto);
        l.getStyleClass().add("chip");
        l.setGraphic(Icons.de(icone, 13, "icon-mute"));
        return l;
    }

    private HBox acoes() {
        HBox h = new HBox(4, btnComentarios);
        h.getStyleClass().add("post-actions");
        h.setAlignment(Pos.CENTER_LEFT);

        if (p.minha() && !p.devolvido()) {
            Button ok = Ui.botao("Marcar devolvido", "ghost", "check", () -> marcarDevolvido());
            ok.getStyleClass().setAll("btn-text");
            ok.setGraphic(Icons.de("check", 16, "icon-accent"));
            h.getChildren().add(ok);
        }
        if (p.minha()) {
            Button del = Ui.botao("Excluir", "ghost", "trash", this::excluir);
            del.getStyleClass().setAll("btn-text", "btn-claim");
            del.setGraphic(Icons.de("trash", 16, "icon-red"));
            h.getChildren().add(del);
        }
        h.getChildren().add(Ui.espaco());
        if (!p.devolvido() && !p.minha()) {
            boolean achei = "achei".equalsIgnoreCase(p.tipo());
            Button claim = new Button(achei ? "É meu!" : "Eu vi!");
            claim.getStyleClass().addAll("btn-text", "btn-claim");
            claim.setOnAction(e -> {
                abrirComentarios();
                preencher(achei ? "Olá! Acredito que esse item é meu. Como posso retirar com você?"
                        : "Olá! Vi um item muito parecido, posso te ajudar!");
            });
            h.getChildren().add(claim);
        }
        return h;
    }

    // ------------------------------------------------------------ ações

    private void marcarDevolvido() {
        Async.run(() -> Ctx.api.marcarDevolvido(p.id()), r -> {
            Ctx.toast("Item marcado como devolvido.");
            aoMudar.run();
        });
    }

    private void excluir() {
        if (!Ui.confirmar("Excluir publicação", "Excluir \"" + p.titulo() + "\"? Essa ação não pode ser desfeita.", "Excluir")) {
            return;
        }
        Async.run(() -> {
            Ctx.api.excluir(p.id());
            return true;
        }, r -> {
            Ctx.toast("Publicação excluída.");
            aoMudar.run();
        });
    }

    private void alternarComentarios() {
        if (comentarios.isVisible()) {
            comentarios.setVisible(false);
            comentarios.setManaged(false);
        } else {
            abrirComentarios();
        }
    }

    private void abrirComentarios() {
        comentarios.setVisible(true);
        comentarios.setManaged(true);
        if (!carregados) {
            carregados = true;
            carregarComentarios(null);
        }
    }

    private TextField campoComentario;

    private void preencher(String texto) {
        if (campoComentario != null) {
            campoComentario.setText(texto);
            campoComentario.requestFocus();
            campoComentario.end();
        } else {
            pendente = texto;
        }
    }

    private String pendente;

    private void carregarComentarios(String manterTexto) {
        Async.run(() -> Ctx.api.comentarios(p.id()), lista -> montarComentarios(lista, manterTexto));
    }

    private void montarComentarios(List<Dtos.Comentario> lista, String manterTexto) {
        comentarios.getChildren().clear();
        for (Dtos.Comentario c : lista) {
            Label autor = Ui.texto(c.autor().nomeCompleto() + ":", "comment-author");
            Label texto = Ui.paragrafo(c.texto(), "comment-text");
            HBox linha = new HBox(6, autor, texto);
            linha.getStyleClass().add("comment");
            linha.setAlignment(Pos.TOP_LEFT);
            HBox.setHgrow(texto, Priority.ALWAYS);
            comentarios.getChildren().add(linha);
        }
        if (lista.isEmpty()) {
            comentarios.getChildren().add(Ui.texto("Ainda não há comentários.", "muted", "small"));
        }

        campoComentario = Ui.entrada("Escreva um comentário...");
        Ui.limitar(campoComentario, 200);
        if (manterTexto != null) campoComentario.setText(manterTexto);
        if (pendente != null) {
            campoComentario.setText(pendente);
            pendente = null;
        }
        Button enviar = Ui.botao("Enviar", "primary", null, null);
        enviar.getStyleClass().add("btn-small");
        Runnable acao = () -> {
            String t = campoComentario.getText().trim();
            if (t.isEmpty()) return;
            enviar.setDisable(true);
            Async.run(() -> Ctx.api.comentar(p.id(), t), c -> {
                p = new Dtos.Publicacao(p.id(), p.tipo(), p.titulo(), p.descricao(), p.categoria(), p.categoriaRotulo(),
                        p.subcategoria(), p.local(), p.autor(), p.criadoEm(), p.devolvido(), p.fotoUrl(),
                        p.comentarios() + 1, p.minha());
                btnComentarios.setText("Comentários (" + p.comentarios() + ")");
                carregarComentarios(null);
            }, t2 -> {
                enviar.setDisable(false);
                Ctx.erro(t2);
            });
        };
        enviar.setOnAction(e -> acao.run());
        campoComentario.setOnAction(e -> acao.run());
        HBox form = new HBox(8, campoComentario, enviar);
        form.setAlignment(Pos.CENTER);
        form.setPadding(new Insets(4, 0, 0, 0));
        HBox.setHgrow(campoComentario, Priority.ALWAYS);
        comentarios.getChildren().add(form);
    }
}
