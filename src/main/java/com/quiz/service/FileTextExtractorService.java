package com.quiz.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.apache.pdfbox.Loader;
import java.io.IOException;

@Service
public class FileTextExtractorService {

    public String extract(MultipartFile file) throws IOException {
        String name = file.getOriginalFilename();
        if (name == null) throw new IllegalArgumentException("Missing filename");

        if (name.endsWith(".pdf")) {
            return extractPdf(file);
        } else if (name.endsWith(".pptx")) {
            return extractPptx(file);
        } else {
            throw new IllegalArgumentException("Unsupported file type. Use PDF or PPTX.");
        }
    }

private String extractPdf(MultipartFile file) throws IOException {
    try (PDDocument doc = Loader.loadPDF(file.getBytes())) {
        PDFTextStripper stripper = new PDFTextStripper();
        return stripper.getText(doc);
    }
}

    private String extractPptx(MultipartFile file) throws IOException {
        try (XMLSlideShow pptx = new XMLSlideShow(file.getInputStream())) {
            StringBuilder sb = new StringBuilder();
            for (XSLFSlide slide : pptx.getSlides()) {
                sb.append("--- Slide ---\n");
                for (XSLFShape shape : slide.getShapes()) {
                    if (shape instanceof XSLFTextShape textShape) {
                        String text = textShape.getText();
                        if (text != null && !text.isBlank()) {
                            sb.append(text.trim()).append("\n");
                        }
                    }
                }
            }
            return sb.toString();
        }
    }
}