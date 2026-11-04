package br.com.achou.desktop.ui;

import br.com.achou.desktop.api.ApiClient;
import br.com.achou.desktop.api.ApiException;
import br.com.achou.desktop.api.Dtos;
import br.com.achou.desktop.util.Async;
import br.com.achou.desktop.util.Prefs;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.List;
import java.util.function.Consumer;

/** Estado global do aplicativo: cliente da API, usuário logado, navegação e avisos (toasts). */
public final class Ctx {

    /** Endereço do backend: -Dachou.api=..., variável ACHOU_API ou preferência salva. */
    public static final String API_URL = System.getProperty("achou.api",
            System.getenv().getOrDefault("ACHOU_API", Prefs.get("api", "http://localhost:8080")));

    public static final ApiClient api = new ApiClient(API_URL);
    public static final ObjectProperty<Dtos.Usuario> usuario = new SimpleObjectProperty<>();

    public static Stage stage;
    /** Chamado quando o usuário sai (ou a sessão expira): volta para a tela de login. */
    public static Runnable aoSair = () -> {};
    /** Navega entre as páginas principais: "inicio", "perfil" ou "ajustes". */
    public static Consumer<String> irPara = pagina -> {};
    /** Recarrega o feed (após criar/alterar/excluir publicações ou mudar a foto). */
    public static Runnable atualizarFeed = () -> {};

    private static VBox areaToasts;
    private static List<Dtos.Categoria> categorias;
    private static final String CSS = Ctx.class.getResource("/css/achou.css").toExternalForm();

    private Ctx() {}

    public static String css() {
        return CSS;
    }

    // ----------------- sessão -----------------

    public static void sair() {
        api.setToken(null);
        Prefs.remove("token");
        usuario.set(null);
        aoSair.run();
    }

    // ----------------- categorias (cache) -----------------

    public static void carregarCategorias(Consumer<List<Dtos.Categoria>> aoCarregar) {
        if (categorias != null) {
            aoCarregar.accept(categorias);
            return;
        }
        Async.run(api::categorias, lista -> {
            categorias = lista;
            aoCarregar.accept(lista);
        });
    }

    // ----------------- avisos -----------------

    public static void registrarToasts(VBox area) {
        areaToasts = area;
    }

    public static void toast(String mensagem) {
        mostrarToast(mensagem, false);
    }

    public static void erro(Throwable t) {
        if (t instanceof ApiException a) {
            if (a.status() == 401 && usuario.get() != null) {
                sair();
                mostrarToast("Sua sessão expirou. Entre novamente.", true);
                return;
            }
            mostrarToast(a.getMessage(), true);
        } else {
            t.printStackTrace();
            mostrarToast("Algo deu errado. Tente novamente.", true);
        }
    }

    private static void mostrarToast(String mensagem, boolean erro) {
        if (areaToasts == null) return;
        Label l = new Label(mensagem);
        l.setWrapText(true);
        l.setMaxWidth(520);
        l.getStyleClass().addAll("toast", erro ? "toast-erro" : "toast-ok");
        l.setOpacity(0);
        areaToasts.getChildren().add(l);

        FadeTransition entra = new FadeTransition(Duration.millis(180), l);
        entra.setToValue(1);
        PauseTransition espera = new PauseTransition(Duration.seconds(erro ? 4.5 : 3));
        FadeTransition sai = new FadeTransition(Duration.millis(300), l);
        sai.setToValue(0);
        sai.setOnFinished(e -> areaToasts.getChildren().remove(l));
        new SequentialTransition(entra, espera, sai).play();
    }
}
