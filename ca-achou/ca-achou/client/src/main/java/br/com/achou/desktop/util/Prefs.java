package br.com.achou.desktop.util;

import java.util.prefs.Preferences;

/** Preferências locais do usuário (tema, sessão mantida). */
public final class Prefs {

    private static final Preferences NODE = Preferences.userRoot().node("br/com/achou/desktop");

    private Prefs() {}

    public static String get(String chave, String padrao) {
        try {
            return NODE.get(chave, padrao);
        } catch (Exception e) {
            return padrao;
        }
    }

    public static void put(String chave, String valor) {
        try {
            NODE.put(chave, valor);
        } catch (Exception ignorado) {
            // preferências indisponíveis: segue sem persistir
        }
    }

    public static void remove(String chave) {
        try {
            NODE.remove(chave);
        } catch (Exception ignorado) {
            // idem
        }
    }
}
