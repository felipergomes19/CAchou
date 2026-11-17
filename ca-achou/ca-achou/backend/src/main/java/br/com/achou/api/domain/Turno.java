package br.com.achou.api.domain;

/** Turno em que o aluno estuda. O nome do enum é o que trafega na API/banco; o rótulo é só para exibição. */
public enum Turno {
    MANHA("Manhã"),
    TARDE("Tarde"),
    NOITE("Noite"),
    INTEGRAL("Integral");

    private final String rotulo;

    Turno(String rotulo) {
        this.rotulo = rotulo;
    }

    public String getRotulo() {
        return rotulo;
    }
}
