package com.app.signflow.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Base64;
import com.app.signflow.model.dto.ImageBlock;
import com.app.signflow.model.dto.TextBlock;
import com.app.signflow.model.dto.TextReplaceBlock;

@Service
@Slf4j
public class PdfService {

    private static final PDType1Font HELVETICA = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

    public byte[] addSignatureToPdf(String pdfPath, String signatureBase64, int pageNumber, int x, int y, Integer width, Integer height) {
        try {
            // Load existing PDF
            File pdfFile = new File(pdfPath);
            PDDocument document = Loader.loadPDF(pdfFile);

            // Decode base64 signature image
            String base64Image = signatureBase64;
            if (base64Image.contains(",")) {
                base64Image = base64Image.split(",")[1];
            }
            byte[] imageBytes = Base64.getDecoder().decode(base64Image);

            // Save signature as temporary file
            File tempImageFile = File.createTempFile("signature", ".png");
            java.nio.file.Files.write(tempImageFile.toPath(), imageBytes);

            // Get the page
            PDPage page = document.getPage(pageNumber - 1); // Pages are 0-indexed

            // Load signature image
            PDImageXObject pdImage = PDImageXObject.createFromFile(tempImageFile.getAbsolutePath(), document);

            // Add signature to page
            PDPageContentStream contentStream = new PDPageContentStream(
                    document, page, PDPageContentStream.AppendMode.APPEND, true, true);

            // Calculate signature dimensions (adjust as needed)
            float signatureWidth = (width != null && width > 0) ? width : 150;
            float signatureHeight = (height != null && height > 0) ? height : 75;

            contentStream.drawImage(pdImage, x, page.getMediaBox().getHeight() - y - signatureHeight, 
                    signatureWidth, signatureHeight);
            contentStream.close();

            // Save to byte array
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            document.save(outputStream);
            document.close();

            // Clean up temp file
            tempImageFile.delete();

            return outputStream.toByteArray();

        } catch (IOException e) {
            log.error("Error adding signature to PDF", e);
            throw new RuntimeException("Failed to add signature to PDF");
        }
    }

    public byte[] addTextToPdf(String pdfPath, String text, int pageNumber, int x, int y) {
        try {
            File pdfFile = new File(pdfPath);
            PDDocument document = Loader.loadPDF(pdfFile);

            PDPage page = document.getPage(pageNumber - 1);
            PDPageContentStream contentStream = new PDPageContentStream(
                    document, page, PDPageContentStream.AppendMode.APPEND, true, true);

            contentStream.beginText();
            contentStream.newLineAtOffset(x, page.getMediaBox().getHeight() - y);
            contentStream.showText(text);
            contentStream.endText();
            contentStream.close();

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            document.save(outputStream);
            document.close();

            return outputStream.toByteArray();

        } catch (IOException e) {
            log.error("Error adding text to PDF", e);
            throw new RuntimeException("Failed to add text to PDF");
        }
    }

    public byte[] mergePdfs(String[] pdfPaths) {
        try {
            PDDocument mergedDocument = new PDDocument();

            for (String pdfPath : pdfPaths) {
                File pdfFile = new File(pdfPath);
                PDDocument document = Loader.loadPDF(pdfFile);
                
                for (PDPage page : document.getPages()) {
                    mergedDocument.addPage(page);
                }
                
                document.close();
            }

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            mergedDocument.save(outputStream);
            mergedDocument.close();

            return outputStream.toByteArray();

        } catch (IOException e) {
            log.error("Error merging PDFs", e);
            throw new RuntimeException("Failed to merge PDFs");
        }
    }

    public byte[] applyEdits(String pdfPath, List<TextBlock> textBlocks, List<ImageBlock> imageBlocks, List<TextReplaceBlock> replaceBlocks) {
        try {
            File pdfFile = new File(pdfPath);
            PDDocument document = Loader.loadPDF(pdfFile);

            if (replaceBlocks != null) {
                for (TextReplaceBlock block : replaceBlocks) {
                    PDPage page = document.getPage(block.getPage() - 1);
                    float pageHeight = page.getMediaBox().getHeight();
                    float fontSize = block.getFontSize() != null ? block.getFontSize() : 14;
                    float leading = fontSize * 1.2f;

                    try (PDPageContentStream contentStream = new PDPageContentStream(
                            document, page, PDPageContentStream.AppendMode.APPEND, true, true)) {
                        contentStream.setNonStrokingColor(1f, 1f, 1f);
                        contentStream.addRect(
                                block.getX(),
                                pageHeight - block.getY() - block.getHeight(),
                                block.getWidth(),
                                block.getHeight()
                        );
                        contentStream.fill();

                        contentStream.setNonStrokingColor(0f, 0f, 0f);
                        contentStream.beginText();
                        contentStream.setFont(HELVETICA, fontSize);
                        contentStream.newLineAtOffset(block.getX(), pageHeight - block.getY() - fontSize);

                        List<String> lines = wrapText(block.getText(), block.getWidth(), fontSize);
                        for (int i = 0; i < lines.size(); i++) {
                            if (i > 0) {
                                contentStream.newLineAtOffset(0, -leading);
                            }
                            contentStream.showText(lines.get(i));
                        }

                        contentStream.endText();
                    }
                }
            }

            if (textBlocks != null) {
                for (TextBlock block : textBlocks) {
                    PDPage page = document.getPage(block.getPage() - 1);
                    float pageHeight = page.getMediaBox().getHeight();
                    float fontSize = block.getFontSize() != null ? block.getFontSize() : 14;

                    try (PDPageContentStream contentStream = new PDPageContentStream(
                            document, page, PDPageContentStream.AppendMode.APPEND, true, true)) {
                        contentStream.beginText();
                        contentStream.setFont(HELVETICA, fontSize);
                        contentStream.newLineAtOffset(block.getX(), pageHeight - block.getY());
                        contentStream.showText(block.getText());
                        contentStream.endText();
                    }
                }
            }

            if (imageBlocks != null) {
                for (ImageBlock block : imageBlocks) {
                    PDPage page = document.getPage(block.getPage() - 1);
                    float pageHeight = page.getMediaBox().getHeight();

                    String base64Image = block.getDataUrl();
                    if (base64Image.contains(",")) {
                        base64Image = base64Image.split(",")[1];
                    }
                    byte[] imageBytes = Base64.getDecoder().decode(base64Image);

                    File tempImageFile = File.createTempFile("image", ".png");
                    java.nio.file.Files.write(tempImageFile.toPath(), imageBytes);

                    PDImageXObject pdImage = PDImageXObject.createFromFile(tempImageFile.getAbsolutePath(), document);

                    try (PDPageContentStream contentStream = new PDPageContentStream(
                            document, page, PDPageContentStream.AppendMode.APPEND, true, true)) {
                        contentStream.drawImage(
                                pdImage,
                                block.getX(),
                                pageHeight - block.getY() - block.getHeight(),
                                block.getWidth(),
                                block.getHeight()
                        );
                    }

                    tempImageFile.delete();
                }
            }

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            document.save(outputStream);
            document.close();
            return outputStream.toByteArray();
        } catch (IOException e) {
            // log.error("Error applying edits", e);
            throw new RuntimeException("Failed to apply edits");
        }
    }

    private List<String> wrapText(String text, float maxWidth, float fontSize) throws IOException {
        List<String> lines = new java.util.ArrayList<>();
        String[] words = text.replace("\n", " \n ").split("\\s+");
        StringBuilder line = new StringBuilder();

        for (String word : words) {
            if ("\n".equals(word)) {
                lines.add(line.toString().trim());
                line = new StringBuilder();
                continue;
            }
            String testLine = line.length() == 0 ? word : line + " " + word;
            float size = HELVETICA.getStringWidth(testLine) / 1000f * fontSize;
            if (size > maxWidth && line.length() > 0) {
                lines.add(line.toString().trim());
                line = new StringBuilder(word);
            } else {
                line = new StringBuilder(testLine);
            }
        }
        if (line.length() > 0) {
            lines.add(line.toString().trim());
        }
        return lines;
    }

}
