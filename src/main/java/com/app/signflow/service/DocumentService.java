package com.app.signflow.service;

import com.app.signflow.model.dto.*;
import com.app.signflow.model.entity.AuditLog;
import com.app.signflow.model.entity.Document;
import com.app.signflow.model.entity.Document.DocumentStatus;
import com.app.signflow.model.entity.User;
import com.app.signflow.repo.AuditLogRepository;
import com.app.signflow.repo.DocumentRepository;
import com.app.signflow.repo.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final AuditLogRepository auditLogRepository;
    private final PdfService pdfService;
    private final EmailService emailService;
    private final LibreOfficeService libreOfficeService;

    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    public DocumentUploadResponse uploadDocument(MultipartFile file) {
        try {
            User currentUser = getCurrentUser();

            // Validate file
            if (file.isEmpty()) {
                throw new RuntimeException("File is empty");
            }

            String contentType = file.getContentType();
            boolean isPdf = "application/pdf".equalsIgnoreCase(contentType);
            boolean isWord = "application/msword".equalsIgnoreCase(contentType)
                    || "application/vnd.openxmlformats-officedocument.wordprocessingml.document".equalsIgnoreCase(contentType);
            if (!isPdf && !isWord) {
                throw new RuntimeException("Only PDF or Word files are allowed");
            }

            // Create upload directory if not exists
            Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // Generate unique filename
            String originalFilename = file.getOriginalFilename();
            String filename = UUID.randomUUID().toString() + "_" + originalFilename;
            Path filePath = uploadPath.resolve(filename);

            // Save file
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // Create document record
            Document document = Document.builder()
                    .ownerId(currentUser.getId())
                    .fileName(originalFilename)
                    .filePath(filePath.toString())
                    .status(DocumentStatus.DRAFT)
                    .build();

            document = documentRepository.save(document);

            // Create audit log
            createAuditLog(document.getId(), "UPLOAD", currentUser.getId());

            return DocumentUploadResponse.builder()
                    .documentId(document.getId())
                    .fileName(document.getFileName())
                    .filePath(document.getFilePath())
                    .build();

        } catch (IOException e) {
            log.error("Error uploading document", e);
            throw new RuntimeException("Failed to upload document");
        }
    }

    public List<DocumentDTO> getDocuments(DocumentStatus status, String search) {
        User currentUser = getCurrentUser();
        List<Document> documents;

        if (status != null && search != null && !search.isEmpty()) {
            documents = documentRepository.findByOwnerIdAndStatus(currentUser.getId(), status)
                    .stream()
                    .filter(d -> d.getFileName().toLowerCase().contains(search.toLowerCase()))
                    .collect(Collectors.toList());
        } else if (status != null) {
            documents = documentRepository.findByOwnerIdAndStatus(currentUser.getId(), status);
        } else if (search != null && !search.isEmpty()) {
            documents = documentRepository.searchDocumentsByOwner(currentUser.getId(), search);
        } else {
            documents = documentRepository.findByOwnerId(currentUser.getId());
        }

        return documents.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public DocumentDTO getDocument(Long id) {
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Document not found"));

        User currentUser = getCurrentUser();
        if (!document.getOwnerId().equals(currentUser.getId())) {
            throw new RuntimeException("Access denied");
        }

        return convertToDTO(document);
    }

    public byte[] downloadDocument(Long id) {
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Document not found"));

        User currentUser = getCurrentUser();
        if (!document.getOwnerId().equals(currentUser.getId())) {
            throw new RuntimeException("Access denied");
        }

        try {
            Path filePath = resolveDocumentPath(document);
            return Files.readAllBytes(filePath);
        } catch (IOException e) {
            log.error("Error reading document file", e);
            throw new RuntimeException("Failed to read document");
        }
    }

    public byte[] downloadAsPdf(Long id) {
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Document not found"));

        User currentUser = getCurrentUser();
        if (!document.getOwnerId().equals(currentUser.getId())) {
            throw new RuntimeException("Access denied");
        }

        try {
            Path resolvedPath = resolveDocumentPath(document);
            Path tempDir = Files.createTempDirectory("signflow-convert-");
            Path pdfFile = libreOfficeService.convertToPdf(resolvedPath, tempDir);
            return Files.readAllBytes(pdfFile);
        } catch (Exception e) {
            log.error("Error converting document to PDF", e);
            throw new RuntimeException("Failed to convert document to PDF");
        }
    }

    public byte[] downloadAsDocx(Long id) {
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Document not found"));

        User currentUser = getCurrentUser();
        if (!document.getOwnerId().equals(currentUser.getId())) {
            throw new RuntimeException("Access denied");
        }

        try {
            Path resolvedPath = resolveDocumentPath(document);
            Path tempDir = Files.createTempDirectory("signflow-convert-");
            Path docxFile = libreOfficeService.convertToDocx(resolvedPath, tempDir);
            return Files.readAllBytes(docxFile);
        } catch (Exception e) {
            log.error("Error converting document to Word", e);
            throw new RuntimeException("Failed to convert document to Word");
        }
    }

    public void deleteDocument(Long id) {
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Document not found"));

        User currentUser = getCurrentUser();
        if (!document.getOwnerId().equals(currentUser.getId())) {
            throw new RuntimeException("Access denied");
        }

        // Delete file
        try {
            Path filePath = resolveDocumentPath(document);
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            log.error("Error deleting document file", e);
        }

        // Create audit log before delete to avoid FK issues
        createAuditLog(document.getId(), "DELETE", currentUser.getId());

        // Delete database record
        documentRepository.delete(document);
    }

    public DocumentDTO signDocument(Long id, SignatureRequest signatureRequest) {
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Document not found"));

        User currentUser = getCurrentUser();
        if (!document.getOwnerId().equals(currentUser.getId())) {
            throw new RuntimeException("Access denied");
        }

        try {
            // Add signature to PDF
                Path resolvedPath = resolveDocumentPath(document);
                    byte[] signedPdf = pdfService.addSignatureToPdf(
                        resolvedPath.toString(),
                        signatureRequest.getSignatureImage(),
                        signatureRequest.getPage(),
                        signatureRequest.getX(),
                        signatureRequest.getY(),
                        signatureRequest.getWidth(),
                        signatureRequest.getHeight()
                    );

            // Save signed PDF
            Files.write(resolvedPath, signedPdf);

            // Update document status
            document.setStatus(DocumentStatus.SIGNED);
            document = documentRepository.save(document);

            // Create audit log
            createAuditLog(document.getId(), "SIGN", currentUser.getId());

            return convertToDTO(document);
        } catch (Exception e) {
            log.error("Error signing document", e);
            throw new RuntimeException("Failed to sign document");
        }
    }

    public void emailDocument(Long id, String email) {
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Document not found"));

        User currentUser = getCurrentUser();
        if (!document.getOwnerId().equals(currentUser.getId())) {
            throw new RuntimeException("Access denied");
        }

        try {
            byte[] pdfData = downloadDocument(id);
            emailService.sendDocumentEmail(email, document.getFileName(), pdfData);

            // Create audit log
            createAuditLog(document.getId(), "EMAIL", currentUser.getId());
        } catch (Exception e) {
            log.error("Error emailing document", e);
            throw new RuntimeException("Failed to email document");
        }
    }

    public DocumentDTO editDocument(Long id, EditDocumentRequest request) {
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Document not found"));

        User currentUser = getCurrentUser();
        if (!document.getOwnerId().equals(currentUser.getId())) {
            throw new RuntimeException("Access denied");
        }

        try {
            Path resolvedPath = resolveDocumentPath(document);
            int textCount = request.getTextBlocks() != null ? request.getTextBlocks().size() : 0;
            int imageCount = request.getImageBlocks() != null ? request.getImageBlocks().size() : 0;
            int replaceCount = request.getReplaceBlocks() != null ? request.getReplaceBlocks().size() : 0;
            log.info("Editing document {} with {} text blocks, {} image blocks, {} replace blocks at {}",
                document.getId(), textCount, imageCount, replaceCount, resolvedPath);
            byte[] editedPdf = pdfService.applyEdits(
                    resolvedPath.toString(),
                    request.getTextBlocks(),
                    request.getImageBlocks(),
                    request.getReplaceBlocks()
            );
            log.info("Edited PDF size: {} bytes", editedPdf != null ? editedPdf.length : 0);
            Files.write(resolvedPath, editedPdf);

            document.setStatus(DocumentStatus.DRAFT);
            document = documentRepository.save(document);

            createAuditLog(document.getId(), "EDIT", currentUser.getId());

            return convertToDTO(document);
        } catch (Exception e) {
            log.error("Error editing document", e);
            throw new RuntimeException("Failed to edit document");
        }
    }

    public String getEditableHtml(Long id) {
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Document not found"));

        User currentUser = getCurrentUser();
        if (!document.getOwnerId().equals(currentUser.getId())) {
            throw new RuntimeException("Access denied");
        }

        try {
            Path resolvedPath = resolveDocumentPath(document);
            Path tempDir = Files.createTempDirectory("signflow-html-");
            Path htmlFile = libreOfficeService.convertToHtml(resolvedPath, tempDir);
            String html = Files.readString(htmlFile);
            log.info("Converted HTML size: {} bytes from {}", html != null ? html.length() : 0, htmlFile);
            String body = extractBodyHtml(html);
            String styles = extractHeadStyles(html);
            return styles + wrapPages(body);
        } catch (Exception e) {
            log.error("Error converting document to HTML", e);
            throw new RuntimeException("Failed to convert document to HTML");
        }
    }

    public DocumentDTO saveEditableHtml(Long id, String html) {
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Document not found"));

        User currentUser = getCurrentUser();
        if (!document.getOwnerId().equals(currentUser.getId())) {
            throw new RuntimeException("Access denied");
        }

        if (html == null || html.isBlank()) {
            throw new RuntimeException("HTML content is empty");
        }

        try {
            Path tempDir = Files.createTempDirectory("signflow-html-save-");
            Path htmlFile = tempDir.resolve("edited.html");
            Files.writeString(htmlFile, wrapHtml(html));

            Path pdfFile = libreOfficeService.convertToPdf(htmlFile, tempDir);

            Path resolvedPath = resolveDocumentPath(document);
            Files.copy(pdfFile, resolvedPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

            document.setStatus(DocumentStatus.DRAFT);
            document = documentRepository.save(document);
            createAuditLog(document.getId(), "EDIT", currentUser.getId());

            return convertToDTO(document);
        } catch (Exception e) {
            log.error("Error saving edited HTML", e);
            throw new RuntimeException("Failed to save edited document");
        }
    }

    private String extractBodyHtml(String html) {
        if (html == null || html.isBlank()) {
            return "";
        }
        String lower = html.toLowerCase();
        int bodyStart = lower.indexOf("<body");
        if (bodyStart < 0) {
            return html;
        }
        int bodyTagEnd = lower.indexOf(">", bodyStart);
        if (bodyTagEnd < 0) {
            return html;
        }
        int bodyEnd = lower.indexOf("</body>", bodyTagEnd);
        if (bodyEnd < 0) {
            return html.substring(bodyTagEnd + 1);
        }
        return html.substring(bodyTagEnd + 1, bodyEnd);
    }

    private String extractHeadStyles(String html) {
        if (html == null || html.isBlank()) {
            return "";
        }
        String lower = html.toLowerCase();
        int headStart = lower.indexOf("<head");
        if (headStart < 0) {
            return "";
        }
        int headTagEnd = lower.indexOf(">", headStart);
        if (headTagEnd < 0) {
            return "";
        }
        int headEnd = lower.indexOf("</head>", headTagEnd);
        if (headEnd < 0) {
            return "";
        }
        String head = html.substring(headTagEnd + 1, headEnd);
        StringBuilder styles = new StringBuilder();
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("(?is)<style.*?>.*?</style>").matcher(head);
        while (matcher.find()) {
            styles.append(matcher.group());
        }
        return styles.toString();
    }

    private String wrapHtml(String bodyHtml) {
        String lower = bodyHtml.toLowerCase();
        if (lower.contains("<html")) {
            if (lower.contains("<style")) {
                return bodyHtml;
            }
            return bodyHtml.replace("<head>", "<head>" + getPdfExportStyles());
        }
        return "<!DOCTYPE html><html><head><meta charset=\"utf-8\">" + getPdfExportStyles() + "</head><body>" + bodyHtml + "</body></html>";
    }

    private String getPdfExportStyles() {
        return "<style>" +
                "body{font-family:Arial, sans-serif; color:#212529; margin:0; padding:0;}" +
                ".page{width:794px; min-height:1123px; margin:0 auto; padding:48px 56px; box-sizing:border-box; page-break-after:always;}" +
                "p{margin:0 0 12px 0; white-space:pre-wrap; word-break:break-word; overflow-wrap:anywhere;}" +
                "*{word-break:break-word; overflow-wrap:anywhere;}" +
                "</style>";
    }

    private String wrapPages(String bodyHtml) {
        if (bodyHtml == null || bodyHtml.isBlank()) {
            return "";
        }
        String content = bodyHtml;
        String pageBreakPattern = "(?i)<(div|p)([^>]*?)page-break-before\\s*:\\s*always([^>]*?)>";
        content = content.replaceAll(pageBreakPattern, "</div><div class=\"page\"><$1$2page-break-before:always$3>");

        String trimmed = content.trim();
        if (!trimmed.startsWith("<div class=\"page\">")) {
            content = "<div class=\"page\">" + content + "</div>";
        }
        return content;
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private void createAuditLog(Long documentId, String action, Long userId) {
        AuditLog log = AuditLog.builder()
                .documentId(documentId)
                .action(action)
                .performedBy(userId)
                .timestamp(LocalDateTime.now())
                .build();
        auditLogRepository.saveAndFlush(log);
    }

    private DocumentDTO convertToDTO(Document document) {
        return DocumentDTO.builder()
                .id(document.getId())
                .ownerId(document.getOwnerId())
                .fileName(document.getFileName())
                .filePath(document.getFilePath())
                .status(document.getStatus())
                .createdAt(document.getCreatedAt())
                .updatedAt(document.getUpdatedAt())
                .build();
    }

    private Path resolveDocumentPath(Document document) {
        Path storedPath = Paths.get(document.getFilePath());
        if (storedPath.isAbsolute()) {
            return storedPath;
        }
        Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        return uploadPath.resolve(storedPath).normalize();
    }
}
