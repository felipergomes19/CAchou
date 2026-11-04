package br.com.achou.desktop.util;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class Fmt {

    private static final DateTimeFormatter DATA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy", new Locale("pt", "BR")).withZone(ZoneId.systemDefault());

    private Fmt() {}

    public static String ago(Instant t) {
        if (t == null) return "";
        long min = Math.max(0, Duration.between(t, Instant.now()).toMinutes());
        if (min < 1) return "agora mesmo";
        if (min < 60) return "há " + min + " min";
        long h = min / 60;
        if (h < 24) return "há " + h + " h";
        return "há " + (h / 24) + " d";
    }

    public static String data(Instant t) {
        return t == null ? "recente" : DATA.format(t);
    }
}
