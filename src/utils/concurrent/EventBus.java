package utils.concurrent;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Lekki lokalny EventBus - umożliwia asynchroniczną komunikację
 * między modułami bez ścisłego powiązania.
 */
public class EventBus {

    private static final Map<Class<?>, List<Consumer<?>>> listeners = new HashMap<>();

    private EventBus() {}

    /** Rejestruje słuchacza danego typu zdarzenia */
    public static synchronized <T> void register(Class<T> type, Consumer<T> listener) {
        listeners.computeIfAbsent(type, k -> new CopyOnWriteArrayList<>()).add(listener);
    }

    /** Wysyła zdarzenie do wszystkich subskrybentów */
    @SuppressWarnings("unchecked")
    public static <T> void post(T event) {
        List<Consumer<?>> subs = listeners.get(event.getClass());
        if (subs != null) {
            for (Consumer<?> c : subs) {
                ((Consumer<T>) c).accept(event);
            }
        }
    }
}
