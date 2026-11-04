package br.com.achou.desktop.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.List;
import java.util.function.IntConsumer;

/** Fábricas de componentes com o visual do app (estilos em achou.css). */
public final class Ui {

    private Ui() {}

    public static Label texto(String texto, String... classes) {
        Label l = new Label(texto);
        l.getStyleClass().addAll(classes);
        return l;
    }

    /** Texto que quebra linha. */
    public static Label paragrafo(String texto, String... classes) {
        Label l = texto(texto, classes);
        l.setWrapText(true);
        return l;
    }

    /**
     * @param variante "primary", "ghost", "white", "header" ou "danger"
     * @param icone    nome do ícone (ou null)
     */
    public static Button botao(String texto, String variante, String icone, Runnable acao) {
        Button b = new Button(texto);
        b.getStyleClass().addAll("btn", "btn-" + variante);
        if (icone != null) {
            String cor = variante.equals("ghost") ? "icon-ink" : variante.equals("danger") ? "icon-red" : "icon-white";
            if (variante.equals("white")) cor = "icon-navy";
            b.setGraphic(Icons.de(icone, 16, cor));
            b.setGraphicTextGap(8);
        }
        if (acao != null) b.setOnAction(e -> acao.run());
        return b;
    }

    public static void ocupado(Button b, boolean ocupado, String textoNormal) {
        b.setDisable(ocupado);
        if (ocupado) {
            b.setText("Aguarde…");
        } else {
            b.setText(textoNormal);
        }
    }

    public static Region espaco() {
        Region r = new Region();
        HBox.setHgrow(r, Priority.ALWAYS);
        VBox.setVgrow(r, Priority.ALWAYS);
        return r;
    }

    public static VBox campo(String rotulo, Node entrada) {
        VBox v = new VBox(6, texto(rotulo, "field-label"), entrada);
        if (entrada instanceof Region r) r.setMaxWidth(Double.MAX_VALUE);
        return v;
    }

    public static TextField entrada(String dica) {
        TextField t = new TextField();
        t.setPromptText(dica);
        return t;
    }

    public static PasswordField senha(String dica) {
        PasswordField p = new PasswordField();
        p.setPromptText(dica);
        return p;
    }

    public static void limitar(TextInputControl campo, int max) {
        campo.setTextFormatter(new TextFormatter<String>(c -> c.getControlNewText().length() <= max ? c : null));
    }

    public static void somenteDigitos(TextField campo, int max) {
        campo.setTextFormatter(new TextFormatter<String>(
                c -> c.getControlNewText().matches("\\d{0," + max + "}") ? c : null));
    }

    /** Controle segmentado (um sempre selecionado). */
    public static HBox segmentado(List<String> rotulos, int inicial, IntConsumer aoMudar) {
        HBox box = new HBox(6);
        box.setAlignment(Pos.CENTER_LEFT);
        ToggleGroup grupo = new ToggleGroup();
        for (int i = 0; i < rotulos.size(); i++) {
            ToggleButton tb = new ToggleButton(rotulos.get(i));
            tb.getStyleClass().add("seg-btn");
            tb.setToggleGroup(grupo);
            tb.setUserData(i);
            tb.setSelected(i == inicial);
            box.getChildren().add(tb);
        }
        grupo.selectedToggleProperty().addListener((o, antigo, novo) -> {
            if (novo == null) {
                if (antigo != null) antigo.setSelected(true); // não permite ficar sem seleção
            } else {
                aoMudar.accept((Integer) novo.getUserData());
            }
        });
        return box;
    }

    /** Página rolável com conteúdo centralizado (largura máxima 820). */
    public static ScrollPane pagina(Region conteudo) {
        conteudo.setMaxWidth(820);
        StackPane suporte = new StackPane(conteudo);
        suporte.setAlignment(Pos.TOP_CENTER);
        suporte.setPadding(new Insets(22, 24, 40, 24));
        ScrollPane sp = new ScrollPane(suporte);
        sp.setFitToWidth(true);
        sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        sp.getStyleClass().add("page");
        return sp;
    }

    public static VBox caixa(String titulo, String subtitulo) {
        VBox box = new VBox(12);
        box.getStyleClass().add("box");
        box.getChildren().add(texto(titulo, "box-title"));
        if (subtitulo != null) box.getChildren().add(paragrafo(subtitulo, "muted"));
        return box;
    }

    public static boolean confirmar(String titulo, String mensagem, String textoBotao) {
        ButtonType ok = new ButtonType(textoBotao, ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelar = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        Alert a = new Alert(Alert.AlertType.CONFIRMATION, mensagem, ok, cancelar);
        a.setTitle(titulo);
        a.setHeaderText(null);
        a.setGraphic(null);
        if (Ctx.stage != null) a.initOwner(Ctx.stage);
        a.getDialogPane().getStylesheets().add(Ctx.css());
        ThemeManager.aplicar(a.getDialogPane());
        return a.showAndWait().filter(b -> b == ok).isPresent();
    }
}
