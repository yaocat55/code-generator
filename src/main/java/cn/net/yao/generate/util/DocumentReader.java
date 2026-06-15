package cn.net.yao.generate.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.web.multipart.MultipartFile;

public class DocumentReader {

    public static String readDocument(MultipartFile file) throws IOException {
        String filename = file.getOriginalFilename();
        if (filename != null && filename.endsWith(".docx")) {
            return readDocx(file.getInputStream());
        }
        return readMarkdown(file.getInputStream());
    }

    private static String readMarkdown(InputStream in) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
            }
        }
        return sb.toString();
    }

    private static String readDocx(InputStream in) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (XWPFDocument doc = new XWPFDocument(in)) {
            doc.getParagraphs().forEach(p -> {
                String text = p.getText();
                if (text != null && !text.isBlank()) {
                    sb.append(text).append('\n');
                }
            });
            doc.getTables().forEach(table -> {
                sb.append("\n|");
                table.getRows().forEach(row -> {
                    row.getTableCells().forEach(cell -> sb.append(cell.getText()).append(" | "));
                    sb.append('\n');
                });
            });
        }
        return sb.toString();
    }
}
