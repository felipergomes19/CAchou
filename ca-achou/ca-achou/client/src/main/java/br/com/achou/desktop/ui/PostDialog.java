package br.com.achou.desktop.ui;

import br.com.achou.desktop.api.Dtos;
import br.com.achou.desktop.util.Async;
import br.com.achou.desktop.util.Imagens;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.List;

/** Janela "Publicar novo item". */
public final class PostDialog {

    private static final String OUTRO = "Outro";

    private PostDialog() {}

    public static void abrir(String tipoInicial) {
        Ctx.carregarCategorias(categorias -> montar(tipoInicial, categorias));
    }

    private static void montar(String tipoInicial, List<Dtos.Categoria> categorias) {
        Stage stage = new Stage();
        stage.initOwner(Ctx.stage);
        stage.initModality(Modality.WINDOW_MODAL);
        stage.setTitle("Publicar novo item");

        // ---- tipo
        ToggleGroup tipoGrupo = new ToggleGroup();
        ToggleButton perdi = cartao("Perdi um item", "PERDI", tipoGrupo);
        ToggleButton achei = cartao("Encontrei algo", "ACHEI", tipoGrupo);
        (("achei".equalsIgnoreCase(tipoInicial)) ? achei : perdi).setSelected(true);
        tipoGrupo.selectedToggleProperty().addListener((o, a, n) -> {
            if (n == null) a.setSelected(true);
        });
        HBox tipos = new HBox(10, perdi, achei);
        HBox.setHgrow(perdi, Priority.ALWAYS);
        HBox.setHgrow(achei, Priority.ALWAYS);
        perdi.setMaxWidth(Double.MAX_VALUE);
        achei.setMaxWidth(Double.MAX_VALUE);

        // ---- campos
        TextField titulo = Ui.entrada("Ex.: Garrafa térmica azul com adesivo");
        Ui.limitar(titulo, 60);
        TextArea descricao = new TextArea();
        descricao.setPromptText("Cor, marca, chaveiros, marcas de uso ou outros detalhes");
        descricao.setPrefRowCount(3);
        descricao.setWrapText(true);
        descricao.setTextFormatter(new TextFormatter<String>(c -> c.getControlNewText().length() <= 300 ? c : null));
        TextField local = Ui.entrada("Ex.: Pátio central, Sala 14, Quadra");
        Ui.limitar(local, 50);

        ComboBox<Dtos.Categoria> categoria = new ComboBox<>();
        categoria.getItems().addAll(categorias);
        categoria.setPromptText("Selecione a categoria");
        categoria.setMaxWidth(Double.MAX_VALUE);
        categoria.setConverter(new StringConverter<>() {
            @Override public String toString(Dtos.Categoria c) { return c == null ? "" : c.rotulo(); }
            @Override public Dtos.Categoria fromString(String s) { return null; }
        });

        ComboBox<String> subtipo = new ComboBox<>();
        subtipo.setPromptText("Selecione o tipo do item");
        subtipo.setMaxWidth(Double.MAX_VALUE);
        subtipo.setDisable(true);
        categoria.valueProperty().addListener((o, a, c) -> {
            subtipo.getItems().clear();
            if (c != null) {
                subtipo.getItems().addAll(c.subcategorias());
                subtipo.getItems().add(OUTRO);
            }
            subtipo.setDisable(c == null);
            subtipo.setValue(null);
        });

        // ---- foto
        final File[] escolhida = {null};
        ImageView previa = new ImageView();
        previa.setFitHeight(120);
        previa.setPreserveRatio(true);
        previa.setManaged(false);
        previa.setVisible(false);
        Label dica = Ui.texto("Clique para escolher uma foto", "muted", "small");
        VBox zona = new VBox(6, Icons.de("image", 26, "icon-mute"), dica, previa);
        zona.setAlignment(Pos.CENTER);
        zona.getStyleClass().add("dropzone");
        zona.setOnMouseClicked(e -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("Foto do item");
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Imagens", "*.jpg", "*.jpeg", "*.png", "*.webp", "*.gif", "*.bmp"));
            File f = fc.showOpenDialog(stage);
            if (f == null) return;
            escolhida[0] = f;
            try {
                Image img = new Image(f.toURI().toString(), 0, 240, true, true);
                if (!img.isError()) {
                    previa.setImage(img);
                    previa.setManaged(true);
                    previa.setVisible(true);
                }
            } catch (Exception ignorado) {
                // sem prévia (ex.: WebP): o arquivo ainda será enviado
            }
            dica.setText(f.getName() + " — clique para trocar");
        });

        Label erro = Ui.paragrafo("", "error-text");
        erro.setManaged(false);
        erro.setVisible(false);

        Button cancelar = Ui.botao("Cancelar", "ghost", null, stage::close);
        Button publicar = Ui.botao("Publicar item", "primary", null, null);
        publicar.setOnAction(e -> {
            String msg = null;
            if (titulo.getText().isBlank()) msg = "Informe o título do item.";
            else if (categoria.getValue() == null) msg = "Escolha uma categoria.";
            else if (subtipo.getValue() == null) msg = "Escolha o tipo do item (ou \"Outro\").";
            else if (local.getText().isBlank()) msg = "Informe o local aproximado.";
            if (msg != null) {
                erro.setText(msg);
                erro.setManaged(true);
                erro.setVisible(true);
                return;
            }
            erro.setManaged(false);
            erro.setVisible(false);
            Ui.ocupado(publicar, true, "Publicar item");

            var req = new Dtos.NovaPublicacao(
                    ((String) tipoGrupo.getSelectedToggle().getUserData()),
                    titulo.getText().trim(), descricao.getText().trim(),
                    categoria.getValue().codigo(), subtipo.getValue(), local.getText().trim());
            File foto = escolhida[0];
            Async.run(() -> {
                Dtos.Publicacao criada = Ctx.api.criar(req);
                if (foto != null) {
                    try {
                        Ctx.api.enviarFoto(criada.id(), Imagens.preparar(foto, 1280, false));
                    } catch (Exception ex) {
                        return "Item publicado, mas a foto não pôde ser enviada: " + ex.getMessage();
                    }
                }
                return null;
            }, aviso -> {
                stage.close();
                Ctx.irPara.accept("inicio");
                Ctx.atualizarFeed.run();
                Ctx.toast(aviso != null ? aviso : "Item publicado!");
            }, t -> {
                Ui.ocupado(publicar, false, "Publicar item");
                erro.setText(t.getMessage());
                erro.setManaged(true);
                erro.setVisible(true);
            });
        });
        HBox botoes = new HBox(10, Ui.espaco(), cancelar, publicar);
        botoes.setAlignment(Pos.CENTER_RIGHT);

        VBox form = new VBox(14,
                Ui.texto("Publicar novo item", "auth-title"),
                tipos,
                Ui.campo("Título do item", titulo),
                HBoxDois(Ui.campo("Categoria", categoria), Ui.campo("Tipo do item", subtipo)),
                Ui.campo("Descrição detalhada", descricao),
                Ui.campo("Local aproximado", local),
                Ui.campo("Foto do item (opcional)", zona),
                erro, botoes);
        form.getStyleClass().add("dialog-root");

        ScrollPane sp = new ScrollPane(form);
        sp.setFitToWidth(true);
        sp.getStyleClass().add("page");
        sp.setPadding(Insets.EMPTY);
        StackPane raiz = new StackPane(sp);
        raiz.getStyleClass().add("dialog-root");
        raiz.setPadding(Insets.EMPTY);
        ThemeManager.aplicar(raiz);

        Scene cena = new Scene(raiz, 560, Math.min(780, javafx.stage.Screen.getPrimary().getVisualBounds().getHeight() - 60));
        cena.getStylesheets().add(Ctx.css());
        stage.setScene(cena);
        stage.show();
        titulo.requestFocus();
    }

    private static HBox HBoxDois(VBox a, VBox b) {
        HBox h = new HBox(12, a, b);
        HBox.setHgrow(a, Priority.ALWAYS);
        HBox.setHgrow(b, Priority.ALWAYS);
        a.setMaxWidth(Double.MAX_VALUE);
        b.setMaxWidth(Double.MAX_VALUE);
        a.setPrefWidth(10);
        b.setPrefWidth(10);
        return h;
    }

    private static ToggleButton cartao(String texto, String valor, ToggleGroup g) {
        ToggleButton b = new ToggleButton(texto);
        b.getStyleClass().add("radio-card");
        b.setToggleGroup(g);
        b.setUserData(valor);
        return b;
    }
}
