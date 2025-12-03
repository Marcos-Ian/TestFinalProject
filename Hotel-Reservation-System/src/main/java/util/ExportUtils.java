package util;

import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.function.Function;

/**
 * Simplified CSV/TXT exporter. Extend to PDF using a PDF library when available.
 */
public final class ExportUtils {
    private ExportUtils() {}

    public static <T> void exportCsv(List<T> rows, List<String> headers, Function<T, List<String>> mapper, Writer writer) throws IOException {
        writer.write(String.join(",", headers));
        writer.write("\n");
        for (T row : rows) {
            writer.write(String.join(",", mapper.apply(row)));
            writer.write("\n");
        }
    }

    public static <T> void exportTxt(List<T> rows, Function<T, String> formatter, Writer writer) throws IOException {
        for (T row : rows) {
            writer.write(formatter.apply(row));
            writer.write("\n");
        }
    }
}
