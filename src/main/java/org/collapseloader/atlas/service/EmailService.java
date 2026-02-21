package org.collapseloader.atlas.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Service
@Slf4j
public class EmailService {
    private final JavaMailSender mailSender;

    @Value("${spring.mail.host:}")
    private String mailHost;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    @Value("${atlas.app-url}")
    private String appUrl;

    public EmailService(@Autowired(required = false) JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendVerificationEmail(String to, String token) {
        if (mailSender == null || mailHost == null || mailHost.isEmpty() || "example.com".equals(mailHost)) {
            log.warn(
                    "Mail sending is disabled because host is '{}' or mail sender is not configured. Skipping sending verification email to {}",
                    mailHost, to);
            return;
        }

        String redirectUrl = appUrl + "/api/v1/auth/verify-redirect?code=" + token + "&email=" + to;
        String subject = "CollapseLoader - Confirm your email";
        String content = """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset='UTF-8'>
                    <meta name='viewport' content='width=device-width, initial-scale=1.0'>
                    <style>
                        :root { --bg: #000000; --card: #ffffff; --muted: #666666; --accent: #000000; --brand: #000000; }
                        body { margin: 0; padding: 24px; background: var(--bg); font-family: Arial, sans-serif; color: var(--brand); }
                        .container { max-width: 600px; margin: 0 auto; }
                        .card { background: var(--card); border-radius: 0px; overflow: hidden; border: 1px solid #333333; }
                        .header { padding: 40px; text-align: center; background: #000000; color: #ffffff; }
                        .brand { font-size: 20px; font-weight: 700; letter-spacing: 2px; text-transform: uppercase; margin: 0; }
                        .body { padding: 40px 32px; }
                        p { margin: 0 0 18px; color: #000000; font-size: 16px; line-height: 1.5; }
                        .code-box { background: #f2f2f2; border: 1px solid #000000; padding: 20px; text-align: center; margin: 20px 0; }
                        .code { font-family: monospace; font-size: 34px; font-weight: 700; letter-spacing: 8px; color: #000000; }
                        .cta { text-align: center; margin: 30px 0; }
                        .btn { background: #000000; color: #ffffff !important; text-decoration: none; padding: 14px 40px; font-weight: 700; display: inline-block; text-transform: uppercase; font-size: 14px; }
                        .footer { padding: 20px; text-align: center; background: #ffffff; border-top: 1px solid #eeeeee; font-size: 11px; color: #999999; }
                        @media (max-width:420px) {
                            .body { padding: 20px; }
                            .code { font-size: 28px; letter-spacing: 4px; }
                            .btn { width: 100%; box-sizing: border-box; }
                        }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="card">
                            <div class="header">
                                <h1 class="brand">CollapseLoader</h1>
                                <div style="margin-top:8px;font-size:12px;opacity:0.8;text-transform:uppercase;">Verification</div>
                            </div>
                            <div class="body">
                                <p>Hello,</p>
                                <p>Your verification code is below. Enter it to confirm your email and start using CollapseLoader.</p>
                                <div class="code-box">
                                    <div class="code">%s</div>
                                </div>
                
                                <p style="text-align: center;">Or press the button below to verify your email:</p>
                
                                <div class="cta">
                                    <a href="%s" class="btn">Verify Email</a>
                                </div>
                                <p style="color:#999999;font-size:13px;text-align: center;">If you didn't create this account, you can safely ignore this message.</p>
                            </div>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(token, redirectUrl);

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
            throw new RuntimeException("Failed to send email", e);
        }
    }
}
