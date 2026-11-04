package br.com.achou.desktop.ui;

import javafx.scene.Group;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.transform.Scale;

/** Lupa do "Achou?" desenhada em código (mesma geometria do logotipo original, viewBox 60x80). */
public final class Logo {

    private static final Color AZUL_CLARO = Color.web("#8099c8");
    private static final Color VERMELHO_CLARO = Color.web("#ff6068");
    private static final Color VERMELHO = Color.web("#e00008");

    private Logo() {}

    public static Pane criar(double altura, Color anel) {
        double k = altura / 80.0;

        Circle sombra = lente(26, 28, AZUL_CLARO);
        Circle frente = lente(29, 28, anel);

        Line cabo1 = cabo(19, 59, 13, 73, VERMELHO_CLARO);
        Line cabo2 = cabo(21, 58, 15, 72, VERMELHO);

        Group g = new Group(sombra, frente, cabo1, cabo2);
        g.getTransforms().add(new Scale(k, k, 0, 0));

        Pane box = new Pane(g);
        box.setMinSize(60 * k, altura);
        box.setPrefSize(60 * k, altura);
        box.setMaxSize(60 * k, altura);
        box.setMouseTransparent(true);
        return box;
    }

    private static Circle lente(double cx, double cy, Color cor) {
        Circle c = new Circle(cx, cy, 20);
        c.setFill(Color.TRANSPARENT);
        c.setStroke(cor);
        c.setStrokeWidth(8);
        c.getStrokeDashArray().addAll(108.0, 17.7);
        c.setStrokeLineCap(StrokeLineCap.ROUND);
        c.setRotate(25);
        return c;
    }

    private static Line cabo(double x1, double y1, double x2, double y2, Color cor) {
        Line l = new Line(x1, y1, x2, y2);
        l.setStroke(cor);
        l.setStrokeWidth(9);
        l.setStrokeLineCap(StrokeLineCap.ROUND);
        return l;
    }
}
