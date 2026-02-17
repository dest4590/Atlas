package org.collapseloader.atlas.config;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.io.InputStream;
import java.util.Properties;

@Configuration
@ConditionalOnProperty(name = "spring.mail.host", havingValue = "example.com")
public class DisabledMailConfig {

    @Bean
    public JavaMailSender javaMailSender() {
        return new JavaMailSender() {
            @Override
            public MimeMessage createMimeMessage() {
                return new MimeMessage(Session.getInstance(new Properties()));
            }

            @Override
            public @Nullable MimeMessage createMimeMessage(InputStream contentStream) {
                return null;
            }

            @Override
            public void send(MimeMessage mimeMessage) {
            }

            @Override
            public void send(MimeMessage... mimeMessages) {
            }

            @Override
            public void send(SimpleMailMessage simpleMessage) {
            }

            @Override
            public void send(SimpleMailMessage... simpleMessages) {
            }
        };
    }
}

