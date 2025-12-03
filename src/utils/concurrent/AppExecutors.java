package utils.concurrent;

import javax.swing.*;
import java.util.concurrent.*;

/**
 * Globalny menedżer pul wątków dla aplikacji enterprise.
 * Obejmuje wątki dla DB, IO, logów, planowania i GUI.
 */
public final class AppExecutors {

    /** Pula dla zadań bazodanowych (większa, wielowątkowa) */
    public static final ExecutorService DB_EXECUTOR =
            Executors.newFixedThreadPool(10, new NamedThreadFactory("DB-Worker"));

    /** Pula dla operacji wejścia/wyjścia (np. pliki, eksport CSV) */
    public static final ExecutorService IO_EXECUTOR =
            Executors.newFixedThreadPool(4, new NamedThreadFactory("IO-Worker"));

    /** Wykonawca dla logów, lekkich zadań tła i zdarzeń */
    public static final ExecutorService BACKGROUND_EXECUTOR =
            Executors.newSingleThreadExecutor(new NamedThreadFactory("Background-Worker"));

    /** Wykonawca cykliczny np. auto-save, monitoring */
    public static final ScheduledExecutorService SCHEDULED_EXECUTOR =
            Executors.newScheduledThreadPool(2, new NamedThreadFactory("Scheduler"));

    /** Wykonawca dla operacji GUI (zwraca się do SwingUtilities) */
    public static final Executor UI_EXECUTOR = SwingUtilities::invokeLater;

    private AppExecutors() {}
}
