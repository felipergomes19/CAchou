package br.com.achou.desktop.ui;

import br.com.achou.desktop.util.Prefs;
import javafx.application.ColorScheme;
import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.stage.Window;

import java.util.ArrayList;

/** Tema claro/escuro/preto + modo automático (segue o sistema operacional). */
public final class ThemeManager {

    public enum Tema {
        AUTO("Automático"), LIGHT("Claro"), DARK("Escuro"), BLACK("Preto OLED");

        public final String rotulo;

        Tema(String rotulo) {
            this.rotulo = rotulo;
        }
    }

    private static Tema atual = Tema.AUTO;

    private ThemeManager() {}

    /** Chame uma vez, depois que o JavaFX iniciou. */
    public static void init() {
        try {
            atual = Tema.valueOf(Prefs.get("tema", Tema.AUTO.name()));
        } catch (IllegalArgumentException e) {
            atual = Tema.AUTO;
        }
        try {
            Platform.getPreferences().colorSchemeProperty().addListener((o, antigo, novo) -> {
                if (atual == Tema.AUTO) aplicarEmTodas();
            });
        } catch (Throwable ignorado) {
            // plataforma sem suporte a preferências: o modo automático fica no claro
        }
    }

    public static Tema atual() {
        return atual;
    }

    public static void definir(Tema tema) {
        atual = tema;
        Prefs.put("tema", tema.name());
        aplicarEmTodas();
    }

    /** Marca a raiz de uma cena com a classe do tema (as cores estão em achou.css). */
    public static void aplicar(Parent raiz) {
        raiz.getStyleClass().removeAll("theme-light", "theme-dark", "theme-black");
        raiz.getStyleClass().add(classeAtual());
    }

    private static String classeAtual() {
        return switch (atual) {
            case LIGHT -> "theme-light";
            case DARK -> "theme-dark";
            case BLACK -> "theme-black";
            case AUTO -> {
                boolean escuro;
                try {
                    escuro = Platform.getPreferences().getColorScheme() == ColorScheme.DARK;
                } catch (Throwable e) {
                    escuro = false;
                }
                yield escuro ? "theme-dark" : "theme-light";
            }
        };
    }

    private static void aplicarEmTodas() {
        for (Window w : new ArrayList<>(Window.getWindows())) {
            if (w.getScene() != null) aplicar(w.getScene().getRoot());
        }
    }
}
