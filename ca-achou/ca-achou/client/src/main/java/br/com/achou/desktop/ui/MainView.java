package br.com.achou.desktop.ui;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

/** Casca do app logado: cabeçalho com navegação + páginas (Início, Perfil, Ajustes). */
public final class MainView extends BorderPane {

    private final FeedView feed = new FeedView();
    private final ProfileView perfil = new ProfileView();
    private final SettingsView ajustes = new SettingsView();
    private final StackPane paginas = new StackPane(feed, perfil, ajustes);
    private final ToggleButton navInicio = nav("Início", "home");
    private final ToggleButton navPerfil = nav("Perfil", "user");
    private final ToggleButton navAjustes = nav("Ajustes", "settings");

    public MainView() {
        setTop(cabecalho());
        setCenter(paginas);
        setBottom(rodape());

        ToggleGroup g = new ToggleGroup();
        for (ToggleButton b : new ToggleButton[]{navInicio, navPerfil, navAjustes}) b.setToggleGroup(g);
        g.selectedToggleProperty().addListener((o, a, n) -> {
            if (n == null) a.setSelected(true);
            else irPara(n == navInicio ? "inicio" : n == navPerfil ? "perfil" : "ajustes");
        });
        Ctx.irPara = this::irPara;
        Ctx.atualizarFeed = feed::recarregar;
        irPara("inicio");
    }

    private void irPara(String pagina) {
        feed.setVisible("inicio".equals(pagina));
        perfil.setVisible("perfil".equals(pagina));
        ajustes.setVisible("ajustes".equals(pagina));
        feed.setManaged(feed.isVisible());
        perfil.setManaged(perfil.isVisible());
        ajustes.setManaged(ajustes.isVisible());

        ToggleButton alvo = switch (pagina) {
            case "perfil" -> navPerfil;
            case "ajustes" -> navAjustes;
            default -> navInicio;
        };
        if (!alvo.isSelected()) alvo.setSelected(true);
        if (alvo == navPerfil) perfil.atualizar();
        if (alvo == navAjustes) ajustes.atualizar();
    }

    private VBox cabecalho() {
        Label nome = Ui.texto("Achou?", "brand-title");
        Label sub = Ui.texto("Etec Camargo Aranha", "brand-sub");
        HBox marca = new HBox(10, Logo.criar(44, Color.WHITE), new VBox(0, nome, sub));
        marca.setAlignment(Pos.CENTER_LEFT);
        marca.setOnMouseClicked(e -> irPara("inicio"));
        marca.setStyle("-fx-cursor: hand;");

        Avatar av = new Avatar(34);
        Label usuario = Ui.texto("", "header-user");
        Ctx.usuario.addListener((o, a, u) -> preencherUsuario(av, usuario));
        preencherUsuario(av, usuario);
        HBox quem = new HBox(8, av, usuario);
        quem.setAlignment(Pos.CENTER);
        quem.setStyle("-fx-cursor: hand;");
        quem.setOnMouseClicked(e -> irPara("perfil"));

        var novo = Ui.botao("Postar item", "primary", "plus", () -> PostDialog.abrir("perdi"));
        var sair = Ui.botao("Sair", "header", null, Ctx::sair);

        HBox barra = new HBox(14, marca, Ui.espaco(), quem, sair, novo);
        barra.getStyleClass().add("header-bar");
        barra.setAlignment(Pos.CENTER);

        HBox nav = new HBox(4, navInicio, navPerfil, navAjustes);
        nav.getStyleClass().add("nav");

        VBox v = new VBox(barra, nav);
        v.getStyleClass().add("header");
        return v;
    }

    private static void preencherUsuario(Avatar av, Label l) {
        var u = Ctx.usuario.get();
        if (u == null) return;
        av.definir(u.nomeCompleto(), u.fotoUrl());
        l.setText(u.nome());
    }

    private static ToggleButton nav(String texto, String icone) {
        ToggleButton b = new ToggleButton(texto);
        b.getStyleClass().add("nav-btn");
        b.setGraphic(Icons.de(icone, 17, "icon-white"));
        return b;
    }

    private Label rodape() {
        Label l = Ui.paragrafo("As publicações ficam salvas com segurança no servidor. Para a devolução ou retirada "
                + "oficial, dirija-se à secretaria ou coordenação da Etec Camargo Aranha.", "footer");
        l.setAlignment(Pos.CENTER);
        l.setMaxWidth(Double.MAX_VALUE);
        l.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        return l;
    }
}
