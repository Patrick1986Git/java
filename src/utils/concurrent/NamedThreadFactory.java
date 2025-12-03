package utils.concurrent;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Fabryka wątków z czytelnymi nazwami, ułatwia debugowanie i logowanie.
 */
public class NamedThreadFactory implements ThreadFactory {

    private final String baseName;
    private final AtomicInteger counter = new AtomicInteger(1);

    public NamedThreadFactory(String baseName) {
        this.baseName = baseName;
    }

    @Override
    public Thread newThread(Runnable r) {
        Thread t = new Thread(r, baseName + "-" + counter.getAndIncrement());
        t.setDaemon(true); // wątki daemona nie blokują zamknięcia aplikacji
        return t;
    }
}
