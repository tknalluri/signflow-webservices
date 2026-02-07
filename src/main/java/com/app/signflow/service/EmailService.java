package com.app.signflow.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:noreply@signflow.com}")
    private String fromEmail;

    public void sendPasswordResetEmail(String toEmail, String resetToken) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("SignFlow - Password Reset");
            message.setText("Click the link below to reset your password:\n\n" +
                    "http://localhost:4200/auth/reset-password?token=" + resetToken + "\n\n" +
                    "This link will expire in 24 hours.");

            mailSender.send(message);
            log.info("Password reset email sent to: {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send password reset email", e);
            throw new RuntimeException("Failed to send email");
        }
    }

    public void sendDocumentEmail(String toEmail, String documentName, byte[] pdfData) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("SignFlow - Document: " + documentName);
            helper.setText("Please find attached the document: " + documentName);

            helper.addAttachment(documentName, () -> new java.io.ByteArrayInputStream(pdfData));

            mailSender.send(message);
            log.info("Document email sent to: {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send document email", e);
            throw new RuntimeException("Failed to send email");
        }
    }
}
