package com.magadiflo.app.service.impl;

import com.magadiflo.app.service.IEmailService;
import com.magadiflo.app.utils.EmailUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class EmailServiceImpl implements IEmailService {

    private final JavaMailSender javaMailSender;

    @Value("${spring.mail.verify.host}")
    private String host;
    @Value("${spring.mail.username}")
    private String fromEmail;

    @Override
    public void sendSimpleMailMessage(String name, String to, String token) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setSubject("Verificación de cuenta de nuevo usuario");
            message.setFrom(this.fromEmail);
            message.setTo(to);
            message.setText(EmailUtils.getEmailMessage(name, this.host, token));

            this.javaMailSender.send(message);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            throw new RuntimeException("Error SimpleMail: " + e.getMessage());
        }
    }

    @Override
    public void sendMimeMessageWithAttachments(String name, String to, String token) {

    }

    @Override
    public void sendMimeMessageWithEmbeddedImages(String name, String to, String token) {

    }

    @Override
    public void sendMimeMessageWithEmbeddedFiles(String name, String to, String token) {

    }

    @Override
    public void sendHtmlEmail(String name, String to, String token) {

    }

    @Override
    public void sendHtmlEmailWithEmbeddedFiles(String name, String to, String token) {

    }
}
