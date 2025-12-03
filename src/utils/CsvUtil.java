package utils;

import utils.concurrent.AppExecutors;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Asynchroniczny writer CSV — bezpieczny dla GUI. Operacje IO wykonywane są w
 * tle przez AppExecutors.IO_EXECUTOR.
 */
public final class CsvUtil {
	private CsvUtil() {
	}

	/**
	 * Zapisuje dane do pliku synchronicznie (używany wewnętrznie lub testowo).
	 */
	public static void writeCsv(String path, List<String[]> rows) throws IOException {
		try (BufferedWriter bw = new BufferedWriter(new FileWriter(path))) {
			for (String[] row : rows) {
				bw.write(String.join(",", escape(row)));
				bw.newLine();
			}
		}
	}

	/**
	 * Asynchroniczny zapis CSV — wykonuje się w puli IO_EXECUTOR.
	 */
	public static CompletableFuture<Void> writeCsvAsync(String path, List<String[]> rows) {
		return CompletableFuture.runAsync(() -> {
			try {
				writeCsv(path, rows);
				LoggerUtil.info("CSV zapisany: " + path);
			} catch (IOException e) {
				LoggerUtil.error("Błąd zapisu CSV: " + path, e);
				throw new RuntimeException(e);
			}
		}, AppExecutors.IO_EXECUTOR);
	}

	private static String[] escape(String[] row) {
		String[] out = new String[row.length];
		for (int i = 0; i < row.length; i++) {
			String cell = row[i] == null ? "" : row[i];
			if (cell.contains(",") || cell.contains("\n") || cell.contains("\"")) {
				cell = "\"" + cell.replace("\"", "\"\"") + "\"";
			}
			out[i] = cell;
		}
		return out;
	}
}
