package co.edu.proxypat.service;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Latencia de trabajo del servicio simulado (consulta a base de datos local).
 * Sin ella todas las operaciones miden 0 ms y la gráfica no dice nada.
 */
final class Latency {

    private Latency() {
    }

    static void of(int minMs, int maxMs) {
        try {
            Thread.sleep(ThreadLocalRandom.current().nextInt(minMs, maxMs + 1));
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
    }
}
