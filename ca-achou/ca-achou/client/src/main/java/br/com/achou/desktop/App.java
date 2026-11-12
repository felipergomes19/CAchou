package br.com.achou.desktop;

import br.com.achou.desktop.api.Dtos;
import br.com.achou.desktop.ui.AuthView;
import br.com.achou.desktop.ui.Ctx;
import br.com.achou.desktop.ui.MainView;
import br.com.achou.desktop.ui.ThemeManager;
import br.com.achou.desktop.util.Async;
import br.com.achou.desktop.util.Prefs;
import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class App extends Application {

    private StackPane raiz;

    @Override
    public void start(Stage stage) {
        Ctx.stage = stage;
        ThemeManager.init();

        VBox toasts = new VBox(8);
        toasts.getStyleClass().add("toasts");
        toasts.setAlignment(Pos.BOTTOM_CENTER);
        toasts.setPickOnBounds(false);
        toasts.setMouseTransparent(true);
        Ctx.registrarToasts(toasts);

        raiz = new StackPane();
        raiz.getStyleClass().add("app-root");

        Scene cena = new Scene(new StackPane(raiz, toasts), 1100, 760);
        // a raiz da cena precisa carregar a classe do tema para os diálogos/popups herdarem as cores
        ThemeManager.aplicar(cena.getRoot());
        cena.getStylesheets().add(Ctx.css());

        stage.setTitle("CAchou? — Etec Camargo Aranha");
        stage.setMinWidth(860);
        stage.setMinHeight(600);
        stage.setScene(cena);

        Ctx.aoSair = this::mostrarLogin;
        stage.show();

        retomarSessao();
    }

    /** Se há um token salvo e ele ainda é válido, entra direto; senão mostra o login. */
    private void retomarSessao() {
        String token = Prefs.get("token", null);
        if (token == null) {
            mostrarLogin();
            return;
        }
        Ctx.api.setToken(token);
        Async.run(Ctx.api::me, this::entrar, t -> {
            Ctx.api.setToken(null);
            Prefs.remove("token");
            mostrarLogin();
        });
    }

    private void mostrarLogin() {
        raiz.getChildren().setAll(new AuthView(this::aoAutenticar));
    }

    private void aoAutenticar(Dtos.AuthResponse r) {
        Ctx.api.setToken(r.token());
        Prefs.put("token", r.token());
        entrar(r.usuario());
    }

    private void entrar(Dtos.Usuario u) {
        Ctx.usuario.set(u);
        raiz.getChildren().setAll(new MainView());
    }

    public static void main(String[] args) {
        launch(args);
    }
}
