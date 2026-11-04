package br.com.achou.api.service;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Proteção simples contra força bruta no login: após {@value #MAX} falhas seguidas para um mesmo RM,
 * o login fica bloqueado por {@value #BLOQUEIO_MIN} minutos. Em memória (suficiente para uma única instância;
 * em cluster, troque por Redis/banco).
 */
@Service
public class TentativasService {

    private static final int MAX = 5;
    private static final long BLOQUEIO_MIN = 15;

    private record Estado(int falhas, Instant bloqueadoAte) {}

    private final ConcurrentHashMap<String, Estado> estados = new ConcurrentHashMap<>();

    public void verificar(String chave) {
        Estado e = estados.get(chave);
        if (e != null && e.bloqueadoAte() != null) {
            if (Instant.now().isBefore(e.bloqueadoAte())) {
                long min = Math.max(1, Duration.between(Instant.now(), e.bloqueadoAte()).toMinutes() + 1);
                throw ApiException.muitasTentativas(
                        "Muitas tentativas incorretas. Tente novamente em " + min + " min ou redefina sua senha.");
            }
            estados.remove(chave);
        }
    }

    public void falhou(String chave) {
        estados.merge(chave, new Estado(1, null), (antigo, novo) -> {
            int f = antigo.falhas() + 1;
            return f >= MAX ? new Estado(f, Instant.now().plus(Duration.ofMinutes(BLOQUEIO_MIN))) : new Estado(f, null);
        });
    }

    public void sucesso(String chave) {
        estados.remove(chave);
    }
}
