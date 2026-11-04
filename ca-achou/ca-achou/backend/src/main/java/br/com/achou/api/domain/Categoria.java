package br.com.achou.api.domain;

import java.util.List;

/**
 * Categorias de itens. Cada categoria tem subtipos (ex.: Eletrônicos → Fones, Celular, Carregador),
 * usados tanto para publicar quanto para filtrar. "Outro" é sempre aceito como subtipo.
 */
public enum Categoria {
    ELETRONICOS("Eletrônicos",
            "Fones de ouvido", "Celular", "Carregador", "Cabo", "Power bank", "Calculadora",
            "Notebook / Tablet", "Pen drive", "Smartwatch"),
    ACESSORIOS("Acessórios",
            "Pulseira", "Corrente", "Anel", "Brinco", "Relógio", "Óculos", "Boné", "Prendedor de cabelo"),
    MATERIAL_ESCOLAR("Materiais escolares",
            "Caderno", "Agenda", "Estojo", "Livro / Apostila", "Caneta / Lápis", "Régua / Compasso",
            "Pasta / Fichário"),
    MOCHILAS_BOLSAS("Mochilas e bolsas",
            "Mochila", "Bolsa", "Necessaire", "Lancheira"),
    GARRAFAS_MARMITAS("Garrafas e marmitas",
            "Garrafa térmica", "Copo", "Marmita / Pote"),
    ROUPAS_UNIFORMES("Roupas e uniformes",
            "Casaco / Moletom", "Camiseta de uniforme", "Jaleco", "Calça", "Tênis / Sapato", "Touca / Luvas"),
    DOCUMENTOS_CHAVES("Documentos e chaves",
            "Chave", "Chaveiro", "Carteira", "Crachá / Carteirinha", "Cartão", "Documento"),
    OUTROS("Outros",
            "Guarda-chuva", "Instrumento musical", "Material esportivo");

    public static final String SUBTIPO_OUTRO = "Outro";

    private final String rotulo;
    private final List<String> subcategorias;

    Categoria(String rotulo, String... subcategorias) {
        this.rotulo = rotulo;
        this.subcategorias = List.of(subcategorias);
    }

    public String getRotulo() {
        return rotulo;
    }

    public List<String> getSubcategorias() {
        return subcategorias;
    }

    public boolean aceitaSubcategoria(String valor) {
        return SUBTIPO_OUTRO.equals(valor) || subcategorias.contains(valor);
    }
}
