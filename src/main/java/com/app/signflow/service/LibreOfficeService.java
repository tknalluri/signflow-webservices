package com.app.signflow.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
@Slf4j
public class LibreOfficeService {

    @Value("${libreoffice.soffice-path:C:\\Program Files\\LibreOffice\\program\\soffice.exe}")
    private String sofficePath;

    public Path convertToHtml(Path inputPdf, Path outputDir) throws IOException, InterruptedException {
        return runConvert(inputPdf, outputDir, "html");
    }

    public Path convertToPdf(Path inputHtml, Path outputDir) throws IOException, InterruptedException {
        return runConvert(inputHtml, outputDir, "pdf");
    }

    public Path convertToDocx(Path inputFile, Path outputDir) throws IOException, InterruptedException {
        return runConvert(inputFile, outputDir, "docx");
    }

    private Path runConvert(Path inputFile, Path outputDir, String format) throws IOException, InterruptedException {
        Files.createDirectories(outputDir);
        String soffice = Files.exists(Path.of(sofficePath)) ? sofficePath : "soffice";
        String inputName = inputFile.getFileName().toString().toLowerCase();
        boolean isPdfInput = inputName.endsWith(".pdf");
        ProcessBuilder builder;
        if (isPdfInput && "docx".equalsIgnoreCase(format)) {
            builder = new ProcessBuilder(
                soffice,
                "--headless",
                "--nologo",
                "--nolockcheck",
                "--infilter=writer_pdf_import",
                "--convert-to", format,
                "--outdir", outputDir.toString(),
                inputFile.toString()
            );
        } else {
            builder = new ProcessBuilder(
                soffice,
                "--headless",
                "--nologo",
                "--nolockcheck",
                "--convert-to", format,
                "--outdir", outputDir.toString(),
                inputFile.toString()
            );
        }
        builder.redirectErrorStream(true);
        Process process = builder.start();
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            String output = new String(process.getInputStream().readAllBytes());
            log.error("LibreOffice conversion failed: {}", output);
            throw new RuntimeException("LibreOffice conversion failed");
        }

        String baseName = inputFile.getFileName().toString();
        int dot = baseName.lastIndexOf('.');
        if (dot > 0) {
            baseName = baseName.substring(0, dot);
        }
        Path outputFile = outputDir.resolve(baseName + "." + format);
        if (!Files.exists(outputFile) && "html".equalsIgnoreCase(format)) {
            Path htmFile = outputDir.resolve(baseName + ".htm");
            if (Files.exists(htmFile)) {
                return htmFile;
            }
        }
        if (!Files.exists(outputFile)) {
            throw new RuntimeException("Converted file not found: " + outputFile);
        }
        return outputFile;
    }
}