package br.com.achou.desktop.ui;

import br.com.achou.desktop.util.Imagens;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;

/** Foto circular do usuário; mostra a inicial do nome enquanto não há foto (ou ela carrega). */
public final class Avatar extends StackPane {

    private final double tamanho;
    private final Label inicial = new Label();
    private final ImageView foto = new ImageView();
    private String urlAtual;

    public Avatar(double tamanho) {
        this.tamanho = tamanho;
        getStyleClass().add("avatar");
        setAlignment(Pos.CENTER);
        setMinSize(tamanho, tamanho);
        setPrefSize(tamanho, tamanho);
        setMaxSize(tamanho, tamanho);
        setStyle("-fx-background-radius: " + tamanho + ";");

        inicial.getStyleClass().add("avatar-initial");
        inicial.setStyle("-fx-font-size: " + Math.round(tamanho * 0.42) + "px;");

        foto.setFitWidth(tamanho);
        foto.setFitHeight(tamanho);
        foto.setPreserveRatio(false);
        foto.setSmooth(true);
        foto.setClip(new Circle(tamanho / 2, tamanho / 2, tamanho / 2));
        foto.setVisible(false);

        getChildren().addAll(inicial, foto);
    }

    public void definir(String nomeCompleto, String fotoUrl) {
        String nome = nomeCompleto == null ? "" : nomeCompleto.trim();
        inicial.setText(nome.isEmpty() ? "?" : nome.substring(0, 1).toUpperCase());
        urlAtual = fotoUrl;
        foto.setVisible(false);
        foto.setImage(null);
        if (fotoUrl != null) {
            Imagens.carregar(fotoUrl, tamanho * 2, img -> {
                if (fotoUrl.equals(urlAtual)) {
                    foto.setImage(img);
                    foto.setVisible(true);
                }
            });
        }
    }
}
