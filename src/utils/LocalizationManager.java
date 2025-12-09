package utils;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * LocalizationManager - prosty menedżer lokalizacji z możliwością przełączania
 * języka w runtime i subskrypcjami (listener pattern).
 *
 * Uwaga: properties powinny znajdować się na classpath pod ścieżką:
 * "i18n/messages" (czyli pliki: i18n/messages_pl.properties i
 * i18n/messages_en.properties).
 */
public final class LocalizationManager {

	public interface LocaleChangeListener {
		void localeChanged(Locale newLocale);
	}

	private static final String BASE_NAME = "resources.i18n.messages";
	private static final List<LocaleChangeListener> listeners = new CopyOnWriteArrayList<>();

	private static volatile Locale currentLocale = Locale.getDefault();
	private static volatile ResourceBundle bundle = loadBundle(currentLocale);

	private LocalizationManager() {
	}

	private static ResourceBundle loadBundle(Locale locale) {
		// Custom control to read properties as UTF-8
		ResourceBundle.Control utf8Control = new ResourceBundle.Control() {
			@Override
			public ResourceBundle newBundle(String baseName, Locale locale, String format, ClassLoader loader,
					boolean reload) throws IllegalAccessException, InstantiationException, java.io.IOException {
				// The below is basically the same as ResourceBundle.Control.newBundle
				String bundleName = toBundleName(baseName, locale);
				String resourceName = toResourceName(bundleName, "properties");
				try (java.io.InputStream stream = loader.getResourceAsStream(resourceName)) {
					if (stream == null)
						return null;
					try (java.io.InputStreamReader reader = new java.io.InputStreamReader(stream,
							java.nio.charset.StandardCharsets.UTF_8)) {
						java.util.PropertyResourceBundle prb = new java.util.PropertyResourceBundle(reader);
						return prb;
					}
				}
			}
		};
		try {
			return ResourceBundle.getBundle(BASE_NAME, locale, LocalizationManager.class.getClassLoader(), utf8Control);
		} catch (MissingResourceException ex) {
			// fallback to default (en)
			try {
				return ResourceBundle.getBundle(BASE_NAME, Locale.ENGLISH, LocalizationManager.class.getClassLoader(),
						utf8Control);
			} catch (Exception e) {
				throw new RuntimeException("No localization bundles found on classpath for base: " + BASE_NAME, e);
			}
		}
	}

	public static synchronized void setLocale(Locale locale) {
		if (locale == null)
			return;
		if (!locale.equals(currentLocale)) {
			currentLocale = locale;
			bundle = loadBundle(locale);
			// notify listeners
			for (LocaleChangeListener l : listeners) {
				try {
					l.localeChanged(locale);
				} catch (Exception ex) {
					// swallow to avoid breaking others
					ex.printStackTrace();
				}
			}
		}
	}

	public static Locale getLocale() {
		return currentLocale;
	}

	public static void addListener(LocaleChangeListener l) {
		listeners.add(l);
	}

	public static void removeListener(LocaleChangeListener l) {
		listeners.remove(l);
	}

	public static String getString(String key) {
		try {
			return bundle.getString(key);
		} catch (MissingResourceException ex) {
			// fallback & easy debugging
			return "??" + key + "??";
		}
	}

	public static String getString(String key, Object... args) {
		String pattern = getString(key);
		try {
			return java.text.MessageFormat.format(pattern, args);
		} catch (Exception e) {
			return pattern;
		}
	}
}
