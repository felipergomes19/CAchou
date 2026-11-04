package br.com.achou.desktop.ui;

import javafx.scene.layout.Pane;
import javafx.scene.shape.SVGPath;
import javafx.scene.transform.Scale;

import java.util.Map;

/** Ícones de traço (estilo Feather, grade 24x24) desenhados com SVGPath e coloridos via CSS. */
public final class Icons {

    private static final Map<String, String> PATHS = Map.ofEntries(
            Map.entry("home", "M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z M9 22V12h6v10"),
            Map.entry("user", "M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2 M12 3a4 4 0 1 0 0 8 4 4 0 1 0 0-8z"),
            Map.entry("settings", "M4 21v-7 M4 10V3 M12 21v-9 M12 8V3 M20 21v-5 M20 12V3 M1 14h6 M9 8h6 M17 16h6"),
            Map.entry("search", "M11 3a8 8 0 1 0 0 16 8 8 0 1 0 0-16z M21 21l-4.35-4.35"),
            Map.entry("plus", "M12 5v14 M5 12h14"),
            Map.entry("pin", "M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z M12 7a3 3 0 1 0 0 6 3 3 0 1 0 0-6z"),
            Map.entry("tag", "M20.59 13.41l-7.17 7.17a2 2 0 0 1-2.83 0L2 12V2h10l8.59 8.59a2 2 0 0 1 0 2.82z M7 7h0.01"),
            Map.entry("message", "M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"),
            Map.entry("check", "M20 6L9 17l-5-5"),
            Map.entry("trash", "M3 6h18 M19 6l-1 14a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2L5 6 M8 6V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"),
            Map.entry("camera", "M23 19a2 2 0 0 1-2 2H3a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h4l2-3h6l2 3h4a2 2 0 0 1 2 2z M12 9a4 4 0 1 0 0 8 4 4 0 1 0 0-8z"),
            Map.entry("image", "M5 3h14a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2z M10 8.5a1.5 1.5 0 1 0-3 0 1.5 1.5 0 1 0 3 0z M21 15l-5-5L5 21"),
            Map.entry("logout", "M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4 M16 17l5-5-5-5 M21 12H9"),
            Map.entry("lock", "M5 11h14a2 2 0 0 1 2 2v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-7a2 2 0 0 1 2-2z M7 11V7a5 5 0 0 1 10 0v4"),
            Map.entry("mail", "M4 4h16a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2z M22 6l-10 7L2 6"),
            Map.entry("x", "M18 6L6 18 M6 6l12 12")
    );

    private Icons() {}

    /**
     * @param nome   nome do ícone (home, user, settings, search, plus, pin, tag, message, check, trash,
     *               camera, image, logout, lock, mail, x)
     * @param tamanho lado em pixels
     * @param cor    classe CSS de cor: "icon-ink" (padrão), "icon-white", "icon-mute", "icon-red"
     */
    public static Pane de(String nome, double tamanho, String cor) {
        SVGPath p = new SVGPath();
        p.setContent(PATHS.getOrDefault(nome, PATHS.get("x")));
        p.getStyleClass().addAll("icon", cor);
        double k = tamanho / 24.0;
        p.getTransforms().add(new Scale(k, k, 0, 0));

        Pane box = new Pane(p);
        box.setMinSize(tamanho, tamanho);
        box.setPrefSize(tamanho, tamanho);
        box.setMaxSize(tamanho, tamanho);
        box.setMouseTransparent(true);
        return box;
    }
}
