package com.magadiflo.app.service.impl;

import com.magadiflo.app.service.IEmailService;
import com.magadiflo.app.utils.EmailUtils;
import jakarta.activation.DataHandler;
import jakarta.activation.DataSource;
import jakarta.activation.FileDataSource;
import jakarta.mail.BodyPart;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.File;
import java.time.LocalDateTime;
import java.util.Map;

@RequiredArgsConstructor
@Service
public class EmailServiceImpl implements IEmailService {

    private final JavaMailSender javaMailSender;
    private final TemplateEngine templateEngine;

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
    public void sendMimeMessageWithEmbeddedFiles(String name, String to, String token) {
        try {
            MimeMessage message = this.getMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setPriority(1);
            helper.setSubject("Verificación de cuenta de nuevo usuario");
            helper.setFrom(this.fromEmail);
            helper.setTo(to);
            helper.setText(EmailUtils.getEmailMessage(name, this.host, token));

            // Agregando archivos adjuntos
            FileSystemResource dog = new FileSystemResource(new File(System.getProperty("user.home") + "/Downloads/dog.jpg"));
            FileSystemResource programming = new FileSystemResource(new File(System.getProperty("user.home") + "/Downloads/programming.jpg"));
            FileSystemResource angular = new FileSystemResource(new File(System.getProperty("user.home") + "/Downloads/angular.pdf"));

            helper.addInline(this.getContentId(dog.getFilename()), dog);
            helper.addInline(this.getContentId(programming.getFilename()), programming);
            helper.addInline(this.getContentId(angular.getFilename()), angular);

            this.javaMailSender.send(message);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            throw new RuntimeException("Error SimpleMail: " + e.getMessage());
        }
    }

    @Override
    @Async
    public void sendHtmlEmail(String name, String to, String token) {
        try {
            Context context = new Context();
            context.setVariables(Map.of(
                    "name", name,
                    "url", EmailUtils.getVerificationUrl(this.host, token),
                    "currentdate", LocalDateTime.now())
            );
            String text = templateEngine.process("email-confirmation-template", context);

            MimeMessage message = this.getMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setPriority(1);
            helper.setSubject("Verificación de cuenta de nuevo usuario");
            helper.setFrom(this.fromEmail);
            helper.setTo(to);
            helper.setText(text, true);

            this.javaMailSender.send(message);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            throw new RuntimeException("Error SimpleMail: " + e.getMessage());
        }
    }

    @Override
    @Async
    public void sendHtmlEmailWithEmbeddedFiles(String name, String to, String token) {
        try {
            MimeMessage message = this.getMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setPriority(1);
            helper.setSubject("Verificación de cuenta de nuevo usuario");
            helper.setFrom(this.fromEmail);
            helper.setTo(to);
            //helper.setText(text, true); <-- Ya no se enviará de esta forma como en los anteriores métodos.

            Context context = new Context();
            context.setVariables(Map.of(
                    "name", name,
                    "url", EmailUtils.getVerificationUrl(this.host, token),
                    "currentdate", LocalDateTime.now())
            );
            String text = templateEngine.process("email-confirmation-template", context);

            MimeMultipart mimeMultipart = new MimeMultipart("related");

            // Agrega el cuerpo del correo html
            BodyPart messageBodyPart = new MimeBodyPart();
            messageBodyPart.setContent(text, "text/html;charset=utf8");
            mimeMultipart.addBodyPart(messageBodyPart);

            // Agrega imágen al cuerpo del correo
            BodyPart imageBodyPart = new MimeBodyPart();
            DataSource dataSource = new FileDataSource(System.getProperty("user.home") + "/Downloads/dog.jpg");
            imageBodyPart.setDataHandler(new DataHandler(dataSource));
            imageBodyPart.setHeader("Content-ID", "image");//En el html <img src="cid:image">
            mimeMultipart.addBodyPart(imageBodyPart);

            message.setContent(mimeMultipart);

            this.javaMailSender.send(message);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            throw new RuntimeException("Error SimpleMail: " + e.getMessage());
        }
    }

    private MimeMessage getMimeMessage() {
        return this.javaMailSender.createMimeMessage();
    }

    private String getContentId(String filename) {
        return "<" + filename + ">";
    }
}
