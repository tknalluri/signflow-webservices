package com.app.signflow.controller;

import com.app.signflow.model.dto.DocumentDTO;
import com.app.signflow.model.dto.DocumentUploadResponse;
import com.app.signflow.model.dto.EditDocumentRequest;
import com.app.signflow.model.dto.SignatureRequest;
import com.app.signflow.model.entity.Document.DocumentStatus;
import com.app.signflow.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/documents")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class DocumentController {

    private final DocumentService documentService;

    @PostMapping("/upload")
    public ResponseEntity<DocumentUploadResponse> uploadDocument(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(documentService.uploadDocument(file));
    }

    @GetMapping
    public ResponseEntity<List<DocumentDTO>> getDocuments(
            @RequestParam(required = false) DocumentStatus status,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(documentService.getDocuments(status, search));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DocumentDTO> getDocument(@PathVariable Long id) {
        return ResponseEntity.ok(documentService.getDocument(id));
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> downloadDocument(@PathVariable Long id) {
        byte[] pdfData = documentService.downloadDocument(id);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "document.pdf");
        headers.setCacheControl("no-store, no-cache, must-revalidate, max-age=0");
        headers.add("Pragma", "no-cache");
        headers.add("Expires", "0");
        
        return ResponseEntity.ok()
                .headers(headers)
                .body(pdfData);
    }

    @GetMapping("/{id}/download-as-pdf")
    public ResponseEntity<byte[]> downloadAsPdf(@PathVariable Long id) {
        DocumentDTO document = documentService.getDocument(id);
        byte[] pdfData = documentService.downloadAsPdf(id);

        String fileName = document.getFileName() != null ? document.getFileName() : "document";
        if (fileName.toLowerCase().endsWith(".docx") || fileName.toLowerCase().endsWith(".doc")) {
            int dot = fileName.lastIndexOf('.');
            fileName = dot > 0 ? fileName.substring(0, dot) : fileName;
        }
        fileName = fileName + ".pdf";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", fileName);
        headers.setCacheControl("no-store, no-cache, must-revalidate, max-age=0");
        headers.add("Pragma", "no-cache");
        headers.add("Expires", "0");

        return ResponseEntity.ok()
                .headers(headers)
                .body(pdfData);
    }

    @GetMapping("/{id}/download-as-docx")
    public ResponseEntity<byte[]> downloadAsDocx(@PathVariable Long id) {
        DocumentDTO document = documentService.getDocument(id);
        byte[] docxData = documentService.downloadAsDocx(id);

        String fileName = document.getFileName() != null ? document.getFileName() : "document";
        if (fileName.toLowerCase().endsWith(".pdf")) {
            int dot = fileName.lastIndexOf('.');
            fileName = dot > 0 ? fileName.substring(0, dot) : fileName;
        }
        fileName = fileName + ".docx";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document"));
        headers.setContentDispositionFormData("attachment", fileName);
        headers.setCacheControl("no-store, no-cache, must-revalidate, max-age=0");
        headers.add("Pragma", "no-cache");
        headers.add("Expires", "0");

        return ResponseEntity.ok()
                .headers(headers)
                .body(docxData);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDocument(@PathVariable Long id) {
        documentService.deleteDocument(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/sign")
    public ResponseEntity<DocumentDTO> signDocument(
            @PathVariable Long id,
            @RequestBody SignatureRequest signatureRequest) {
        return ResponseEntity.ok(documentService.signDocument(id, signatureRequest));
    }

    @PostMapping("/{id}/edit")
    public ResponseEntity<DocumentDTO> editDocument(
            @PathVariable Long id,
            @RequestBody EditDocumentRequest request) {
        return ResponseEntity.ok(documentService.editDocument(id, request));
    }

    @GetMapping("/{id}/edit-html")
    public ResponseEntity<String> getEditableHtml(@PathVariable Long id) {
        String html = documentService.getEditableHtml(id);
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(html);
    }

    @PostMapping("/{id}/save-html")
    public ResponseEntity<DocumentDTO> saveEditableHtml(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        String html = request.get("html");
        return ResponseEntity.ok(documentService.saveEditableHtml(id, html));
    }

    @PostMapping("/{id}/email")
    public ResponseEntity<Map<String, String>> emailDocument(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        documentService.emailDocument(id, request.get("email"));
        return ResponseEntity.ok(Map.of("message", "Document sent successfully"));
    }
}
