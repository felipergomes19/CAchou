package br.com.achou.desktop.ui;

import br.com.achou.desktop.api.ApiClient;
import br.com.achou.desktop.api.Dtos;
import br.com.achou.desktop.util.Async;
import javafx.animation.PauseTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.Duration;

import java.util.List;

/** Página inicial: hero, busca, filtros (tipo, categoria, subtipo) e lista paginada de publicações. */
public final class FeedView extends ScrollPane {

    private static final int TAMANHO_PAGINA = 10;

    private final VBox lista = new VBox(18);
    private final HBox categoriasBox = new HBox(8);
    private final HBox subcategoriasBox = new HBox(8);
    private final ScrollPane categoriasScroll = new ScrollPane(categoriasBox);
    private final ScrollPane subScroll = new ScrollPane(subcategoriasBox);
    private final Button maisBtn = Ui.botao("Carregar mais", "ghost", null, null);
    private final Label contador = Ui.texto("", "muted", "small");
    private final TextField busca = Ui.entrada("Buscar por mochila, garrafa, chave, casaco...");
    private final CheckBox somenteAbertos = new CheckBox("Ocultar já devolvidos");

    private String tipo;          // null = todos
    private String categoria;     // null = todas
    private String subcategoria;  // null = todas
    private int pagina;
    private int geracao;          // descarta respostas antigas quando o filtro muda
    private List<Dtos.Categoria> catalogo = List.of();
    private final PauseTransition debounce = new PauseTransition(Duration.millis(300));

    public FeedView() {
        VBox raiz = new VBox(16);
        raiz.setMaxWidth(820);
        raiz.getChildren().addAll(hero(), ferramentas(), categoriasScroll, subScroll, contador, lista, maisBtn);
        raiz.setAlignment(Pos.TOP_CENTER);

        StackPane suporte = new StackPane(raiz);
        suporte.setAlignment(Pos.TOP_CENTER);
        suporte.setPadding(new Insets(22, 24, 40, 24));
        setContent(suporte);
        setFitToWidth(true);
        setHbarPolicy(ScrollBarPolicy.NEVER);
        getStyleClass().add("page");

        configurarRolagemHorizontal(categoriasScroll);
        configurarRolagemHorizontal(subScroll);
        subScroll.setManaged(false);
        subScroll.setVisible(false);

        maisBtn.setOnAction(e -> carregar(false));
        maisBtn.setManaged(false);
        maisBtn.setVisible(false);
        maisBtn.setMaxWidth(Double.MAX_VALUE);

        debounce.setOnFinished(e -> carregar(true));
        busca.textProperty().addListener((o, a, b) -> debounce.playFromStart());
        somenteAbertos.setOnAction(e -> carregar(true));

        Ctx.carregarCategorias(c -> {
            catalogo = c;
            desenharCategorias();
        });
        carregar(true);
    }

    /** Recarrega do início (chamado após criar/alterar publicações). */
    public void recarregar() {
        carregar(true);
    }

    // ------------------------------------------------------------ construção

    private VBox hero() {
        Label titulo = Ui.paragrafo("Perdeu algo na escola? Achou no corredor?", "hero-title");
        Label texto = Ui.paragrafo("Poste, comente e ajude a devolver. A comunidade da Etec Camargo Aranha "
                + "colabora unida.", "hero-text");
        Button perdi = Ui.botao("Perdi um item", "primary", "plus", () -> PostDialog.abrir("perdi"));
        Button achei = Ui.botao("Achei um item", "white", "plus", () -> PostDialog.abrir("achei"));
        HBox acoes = new HBox(12, perdi, achei);

        VBox h = new VBox(10, titulo, texto, acoes);
        h.getStyleClass().add("hero");
        return h;
    }

    private VBox ferramentas() {
        StackPane campo = new StackPane(busca);
        busca.getStyleClass().add("search-field");
        var lupa = Icons.de("search", 18, "icon-mute");
        StackPane.setAlignment(lupa, Pos.CENTER_LEFT);
        StackPane.setMargin(lupa, new Insets(0, 0, 0, 15));
        campo.getChildren().add(lupa);
        HBox.setHgrow(campo, Priority.ALWAYS);

        HBox tipos = Ui.segmentado(List.of("Todos", "Perdidos", "Achados"), 0, i -> {
            tipo = i == 0 ? null : i == 1 ? "PERDI" : "ACHEI";
            carregar(true);
        });
        HBox linha = new HBox(12, campo, tipos);
        linha.setAlignment(Pos.CENTER_LEFT);

        somenteAbertos.getStyleClass().add("muted");
        return new VBox(8, linha, somenteAbertos);
    }

    private static void configurarRolagemHorizontal(ScrollPane sp) {
        sp.setFitToHeight(true);
        sp.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        sp.getStyleClass().add("page");
        sp.setMinHeight(40);
        sp.setPrefHeight(40);
        // roda do mouse rola a lista de chips horizontalmente
        sp.addEventFilter(javafx.scene.input.ScrollEvent.SCROLL, e -> {
            double dx = Math.abs(e.getDeltaX()) > Math.abs(e.getDeltaY()) ? e.getDeltaX() : e.getDeltaY();
            sp.setHvalue(sp.getHvalue() - dx / 800.0);
            e.consume();
        });
    }

    private void desenharCategorias() {
        categoriasBox.getChildren().clear();
        ToggleGroup g = new ToggleGroup();
        categoriasBox.getChildren().add(chipCategoria("Todas", null, g, categoria == null));
        for (Dtos.Categoria c : catalogo) {
            categoriasBox.getChildren().add(chipCategoria(c.rotulo(), c.codigo(), g, c.codigo().equals(categoria)));
        }
        desenharSubcategorias();
    }

    private ToggleButton chipCategoria(String rotulo, String codigo, ToggleGroup g, boolean selecionado) {
        ToggleButton b = new ToggleButton(rotulo);
        b.getStyleClass().addAll("seg-btn", "cat-chip");
        b.setToggleGroup(g);
        b.setSelected(selecionado);
        b.setOnAction(e -> {
            if (!b.isSelected()) { // clique em quem já está ativo: mantém selecionado
                b.setSelected(true);
                return;
            }
            categoria = codigo;
            subcategoria = null;
            desenharSubcategorias();
            carregar(true);
        });
        return b;
    }

    private void desenharSubcategorias() {
        subcategoriasBox.getChildren().clear();
        Dtos.Categoria atual = catalogo.stream().filter(c -> c.codigo().equals(categoria)).findFirst().orElse(null);
        boolean mostrar = atual != null && !atual.subcategorias().isEmpty();
        subScroll.setManaged(mostrar);
        subScroll.setVisible(mostrar);
        if (!mostrar) return;

        ToggleGroup g = new ToggleGroup();
        subcategoriasBox.getChildren().add(chipSub("Todos os tipos", null, g, true));
        for (String s : atual.subcategorias()) {
            subcategoriasBox.getChildren().add(chipSub(s, s, g, false));
        }
    }

    private ToggleButton chipSub(String rotulo, String valor, ToggleGroup g, boolean selecionado) {
        ToggleButton b = new ToggleButton(rotulo);
        b.getStyleClass().addAll("seg-btn", "sub-chip");
        b.setToggleGroup(g);
        b.setSelected(selecionado);
        b.setOnAction(e -> {
            if (!b.isSelected()) {
                b.setSelected(true);
                return;
            }
            subcategoria = valor;
            carregar(true);
        });
        return b;
    }

    // ------------------------------------------------------------ dados

    private void carregar(boolean recomecar) {
        if (recomecar) {
            pagina = 0;
            geracao++;
        }
        final int minhaGeracao = geracao;
        final int paginaPedida = pagina;
        var filtro = new ApiClient.Filtro(tipo, categoria, subcategoria, busca.getText(), somenteAbertos.isSelected(), false);
        maisBtn.setDisable(true);

        Async.run(() -> Ctx.api.listar(filtro, paginaPedida, TAMANHO_PAGINA), res -> {
            if (minhaGeracao != geracao) return;
            if (paginaPedida == 0) lista.getChildren().clear();
            for (Dtos.Publicacao p : res.itens()) {
                lista.getChildren().add(new PostCard(p, () -> carregar(true)));
            }
            if (lista.getChildren().isEmpty()) lista.getChildren().add(vazio());
            contador.setText(res.total() == 1 ? "1 item encontrado" : res.total() + " itens encontrados");
            boolean temMais = res.pagina() + 1 < res.totalPaginas();
            maisBtn.setManaged(temMais);
            maisBtn.setVisible(temMais);
            maisBtn.setDisable(false);
            pagina = res.pagina() + 1;
        }, t -> {
            maisBtn.setDisable(false);
            if (minhaGeracao == geracao) Ctx.erro(t);
        });
    }

    private VBox vazio() {
        VBox v = new VBox(8, Icons.de("search", 40, "icon-mute"),
                Ui.texto("Nenhum item encontrado.", "box-title"),
                Ui.paragrafo("Tente alterar os filtros ou publique um novo anúncio.", "muted"));
        v.setAlignment(Pos.CENTER);
        v.getStyleClass().add("empty");
        return v;
    }
}
