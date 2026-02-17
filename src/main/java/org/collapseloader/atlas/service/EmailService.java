package org.collapseloader.atlas.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {
    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${atlas.app-url}")
    private String appUrl;

    public void sendVerificationEmail(String to, String token) {
        String verificationUrl = appUrl + "/auth/verify?token=" + token;
        String subject = "CollapseLoader - Confirm your email";
        String content = "Hello,<br><br>"
                + "Thank you for registering. Please click the link below to verify your email:<br>"
                + "<h3><a href=\"" + verificationUrl + "\" target=\"_self\">VERIFY EMAIL</a></h3>"
                + "If you did not register, please ignore this email.<br><br>"
                + "Best regards,<br>"
                + "CollapseLoader Team";

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                    StandardCharsets.UTF_8.name());

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(content, true);

            mailSender.send(message);
            log.info("Verification email sent to {}", to);
        } catch (MessagingException e) {
            log.error("Failed to send verification email to {}", to, e);
            throw new RuntimeException("Failed to send verification email");
        }
    }
}
