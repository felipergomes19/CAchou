package br.com.achou.desktop.util;

import br.com.achou.desktop.ui.Ctx;
import javafx.application.Platform;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/** Executa chamadas de rede fora da thread da interface e devolve o resultado nela. */
public final class Async {

    private static final ExecutorService POOL = Executors.newFixedThreadPool(4, r -> {
        Thread t = new Thread(r, "achou-io");
        t.setDaemon(true);
        return t;
    });

    private Async() {}

    /** Erros caem no tratamento padrão (toast, ou logout se a sessão expirou). */
    public static <T> void run(Callable<T> tarefa, Consumer<T> aoConcluir) {
        run(tarefa, aoConcluir, Ctx::erro);
    }

    public static <T> void run(Callable<T> tarefa, Consumer<T> aoConcluir, Consumer<Throwable> aoFalhar) {
        POOL.submit(() -> {
            try {
                T resultado = tarefa.call();
                Platform.runLater(() -> aoConcluir.accept(resultado));
            } catch (Throwable t) {
                Platform.runLater(() -> aoFalhar.accept(t));
            }
        });
    }
}
