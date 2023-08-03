package com.magadiflo.app.service.impl;

import com.magadiflo.app.service.IEmailService;
import com.magadiflo.app.utils.EmailUtils;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.File;

@RequiredArgsConstructor
@Service
public class EmailServiceImpl implements IEmailService {

    private final JavaMailSender javaMailSender;

    @Value("${spring.mail.verify.host}")
    private String host;
    @Value("${spring.mail.username}")
    private String fromEmail;

    @Override
    @Async
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
    @Async
    public void sendMimeMessageWithAttachments(String name, String to, String token) {
        try {
            MimeMessage message = this.getMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setPriority(1); //Establece la prioridad (encabezado "X-Priority") del mensaje. Entre 1(más alto) y 5 (más bajo)
            helper.setSubject("Verificación de cuenta de nuevo usuario");
            helper.setFrom(this.fromEmail);
            helper.setTo(to);
            helper.setText(EmailUtils.getEmailMessage(name, this.host, token));

            // Agregando archivos adjuntos
            FileSystemResource dog = new FileSystemResource(new File(System.getProperty("user.home") + "/Downloads/dog.jpg"));
            FileSystemResource programming = new FileSystemResource(new File(System.getProperty("user.home") + "/Downloads/programming.jpg"));
            FileSystemResource angular = new FileSystemResource(new File(System.getProperty("user.home") + "/Downloads/angular.pdf"));

            helper.addAttachment(dog.getFilename(), dog);
            helper.addAttachment(programming.getFilename(), programming);
            helper.addAttachment(angular.getFilename(), angular);

            this.javaMailSender.send(message);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            throw new RuntimeException("Error SimpleMail: " + e.getMessage());
        }
    }

    @Override
    @Async
    public void sendMimeMessageWithEmbeddedImages(String name, String to, String token) {

    }

    @Override
    @Async
    public void sendMimeMessageWithEmbeddedFiles(String name, String to, String token) {

    }

    @Override
    @Async
    public void sendHtmlEmail(String name, String to, String token) {

    }

    @Override
    @Async
    public void sendHtmlEmailWithEmbeddedFiles(String name, String to, String token) {

    }

    private MimeMessage getMimeMessage() {
        return this.javaMailSender.createMimeMessage();
    }
}
