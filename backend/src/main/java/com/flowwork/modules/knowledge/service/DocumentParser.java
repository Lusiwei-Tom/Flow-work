package com.flowwork.modules.knowledge.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 文档解析：支持 txt / md / pdf
 */
@Component
public class DocumentParser {

    public String parse(String fileName, byte[] bytes) throws IOException {
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".txt") || lower.endsWith(".md")) {
            return new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
        }
        if (lower.endsWith(".pdf")) {
            try (PDDocument doc = Loader.loadPDF(bytes)) {
                return new PDFTextStripper().getText(doc);
            }
        }
        throw new IOException("不支持的文件类型，仅支持 txt / md / pdf");
    }
}
