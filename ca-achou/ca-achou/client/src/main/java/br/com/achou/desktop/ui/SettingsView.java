package br.com.achou.desktop.ui;

import br.com.achou.desktop.api.ApiClient;
import br.com.achou.desktop.api.Dtos;
import br.com.achou.desktop.util.Async;
import br.com.achou.desktop.util.Fmt;
import br.com.achou.desktop.util.Imagens;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.Arrays;
import java.util.List;

/** Ajustes: conta (foto de perfil + nome), tema, senha e histórico de publicações. */
public final class SettingsView extends ScrollPane {

    private final VBox conteudo = new VBox(18);
    private final VBox conta = new VBox(14);
    private final VBox historico = new VBox(0);
    private String histTipo = "PERDI";

    public SettingsView() {
        conteudo.setMaxWidth(820);
        conteudo.getChildren().addAll(Ui.texto("Configurações", "title-lg"), caixaConta(), caixaTema(), caixaSenha(), caixaHistorico());
        StackPane suporte = new StackPane(conteudo);
        suporte.setAlignment(Pos.TOP_CENTER);
        suporte.setPadding(new javafx.geometry.Insets(22, 24, 40, 24));
        setContent(suporte);
        setFitToWidth(true);
        setHbarPolicy(ScrollBarPolicy.NEVER);
        getStyleClass().add("page");
        Ctx.usuario.addListener((o, a, b) -> desenharConta());
    }

    /** Chamado ao abrir a página. */
    public void atualizar() {
        desenharConta();
        carregarHistorico();
    }

    // ------------------------------------------------------------ conta

    private VBox caixaConta() {
        VBox box = Ui.caixa("Conta", null);
        box.getChildren().add(conta);
        desenharConta();
        return box;
    }

    private void desenharConta() {
        Dtos.Usuario u = Ctx.usuario.get();
        conta.getChildren().clear();
        if (u == null) return;

        Avatar av = new Avatar(84);
        av.definir(u.nomeCompleto(), u.fotoUrl());

        Button trocar = Ui.botao(u.fotoUrl() == null ? "Escolher foto de perfil" : "Trocar foto", "primary", "camera", null);
        trocar.setOnAction(e -> escolherFoto(trocar));
        HBox botoes = new HBox(10, trocar);
        if (u.fotoUrl() != null) {
            Button remover = Ui.botao("Remover foto", "ghost", "trash", () ->
                    Async.run(Ctx.api::removerFotoPerfil, novo -> {
                        Ctx.usuario.set(novo);
                        Ctx.atualizarFeed.run();
                        Ctx.toast("Foto removida.");
                    }));
            botoes.getChildren().add(remover);
        }
        Label dica = Ui.texto("JPG, PNG ou WebP · será recortada em formato quadrado", "muted", "small");

        HBox linha = new HBox(18, av, new VBox(6,
                Ui.texto(u.nomeCompleto(), "title-lg"),
                Ui.texto(u.turma() + " · RM " + u.rm(), "muted"),
                botoes, dica));
        linha.setAlignment(Pos.CENTER_LEFT);

        Button sair = Ui.botao("Sair da conta", "danger", "logout", Ctx::sair);
        conta.getChildren().addAll(linha, sair);
    }

    private void escolherFoto(Button origem) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Escolha sua foto de perfil");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Imagens", "*.jpg", "*.jpeg", "*.png", "*.webp", "*.gif", "*.bmp"));
        File f = fc.showOpenDialog(Ctx.stage);
        if (f == null) return;
        origem.setDisable(true);
        Async.run(() -> Ctx.api.definirFotoPerfil(Imagens.preparar(f, 512, true)), novo -> {
            origem.setDisable(false);
            Ctx.usuario.set(novo);
            Ctx.atualizarFeed.run();
            Ctx.toast("Foto de perfil atualizada!");
        }, t -> {
            origem.setDisable(false);
            Ctx.erro(t);
        });
    }

    // ------------------------------------------------------------ tema

    private VBox caixaTema() {
        VBox box = Ui.caixa("Tema visual", "Personalize a aparência conforme sua preferência.");
        List<ThemeManager.Tema> temas = Arrays.asList(ThemeManager.Tema.values());
        HBox seg = Ui.segmentado(temas.stream().map(t -> t.rotulo).toList(), temas.indexOf(ThemeManager.atual()),
                i -> ThemeManager.definir(temas.get(i)));
        box.getChildren().add(seg);
        return box;
    }

    // ------------------------------------------------------------ senha

    private VBox caixaSenha() {
        VBox box = Ui.caixa("Segurança", "Altere a senha de acesso. O login continua sendo feito com seu RM.");
        PasswordField atual = Ui.senha("Senha atual");
        PasswordField nova = Ui.senha("Nova senha (mínimo 6 caracteres)");
        PasswordField confirma = Ui.senha("Repita a nova senha");
        Button salvar = Ui.botao("Alterar senha", "ghost", "lock", null);
        salvar.setOnAction(e -> {
            if (atual.getText().isEmpty() || nova.getText().length() < 6) {
                Ctx.toast("Informe a senha atual e uma nova senha de pelo menos 6 caracteres.");
                return;
            }
            if (!nova.getText().equals(confirma.getText())) {
                Ctx.toast("As senhas não coincidem.");
                return;
            }
            salvar.setDisable(true);
            Async.run(() -> Ctx.api.alterarSenha(atual.getText(), nova.getText()), r -> {
                salvar.setDisable(false);
                atual.clear();
                nova.clear();
                confirma.clear();
                Ctx.toast(r.mensagem());
            }, t -> {
                salvar.setDisable(false);
                Ctx.erro(t);
            });
        });
        box.getChildren().addAll(Ui.campo("Senha atual", atual), Ui.campo("Nova senha", nova),
                Ui.campo("Confirmar nova senha", confirma), salvar);
        return box;
    }

    // ------------------------------------------------------------ histórico

    private VBox caixaHistorico() {
        VBox box = Ui.caixa("Meu histórico", "Gerencie seus itens publicados no sistema.");
        HBox abas = Ui.segmentado(List.of("Meus itens perdidos", "Itens que encontrei"), 0, i -> {
            histTipo = i == 0 ? "PERDI" : "ACHEI";
            carregarHistorico();
        });
        box.getChildren().addAll(abas, historico);
        return box;
    }

    private void carregarHistorico() {
        if (Ctx.usuario.get() == null) return;
        var filtro = new ApiClient.Filtro(histTipo, null, null, null, false, true);
        Async.run(() -> Ctx.api.listar(filtro, 0, 50), pagina -> {
            historico.getChildren().clear();
            if (pagina.itens().isEmpty()) {
                Label vazio = Ui.texto("Nenhum registro encontrado nesta categoria.", "muted");
                vazio.setPadding(new javafx.geometry.Insets(14, 0, 4, 0));
                historico.getChildren().add(vazio);
                return;
            }
            for (Dtos.Publicacao p : pagina.itens()) historico.getChildren().add(linha(p));
        });
    }

    private HBox linha(Dtos.Publicacao p) {
        Label status = new Label(p.devolvido() ? "DEVOLVIDO" : "EM ABERTO");
        status.getStyleClass().addAll("tag", p.devolvido() ? "tag-devolvido" : "PERDI".equalsIgnoreCase(p.tipo()) ? "tag-perdi" : "tag-achei");

        VBox info = new VBox(1, Ui.texto(p.titulo(), "post-author"),
                Ui.texto(p.local() + " · " + p.categoriaRotulo() + " · " + Fmt.ago(p.criadoEm()), "muted", "small"));
        HBox.setHgrow(info, Priority.ALWAYS);

        HBox h = new HBox(10, info, status);
        h.getStyleClass().add("hist-row");
        h.setAlignment(Pos.CENTER_LEFT);
        if (!p.devolvido()) {
            Button concluir = Ui.botao("Concluir", "ghost", null, null);
            concluir.getStyleClass().add("btn-small");
            concluir.setOnAction(e -> Async.run(() -> Ctx.api.marcarDevolvido(p.id()), r -> {
                carregarHistorico();
                Ctx.atualizarFeed.run();
            }));
            h.getChildren().add(concluir);
        }
        Button excluir = Ui.botao("Excluir", "danger", null, null);
        excluir.getStyleClass().add("btn-small");
        excluir.setOnAction(e -> {
            if (!Ui.confirmar("Excluir publicação", "Excluir \"" + p.titulo() + "\"?", "Excluir")) return;
            Async.run(() -> {
                Ctx.api.excluir(p.id());
                return true;
            }, r -> {
                carregarHistorico();
                Ctx.atualizarFeed.run();
            });
        });
        h.getChildren().add(excluir);
        return h;
    }
}
