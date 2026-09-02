package com.vinayakbhat.bookingSuite.common.notification;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.UnsupportedEncodingException;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {
    // Inject values from application.yml
    @Value("${app.email.sender-address}")
    private String senderAddress;

    @Value("${app.email.sender-name}")
    private String senderName;

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Async("emailTaskExecutor")
    public void sendDynamicHtmlEmail(String to, String subject, String templateName, Map<String, Object> variables) {
        try {
            // 1. Pass data to the HTML template
            Context context = new Context();
            context.setVariables(variables);
            String htmlBody = templateEngine.process("emails/" + templateName, context);

            // 2. Create the email
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(senderAddress, senderName);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true); // true = HTML format

            // 3. Send it via Gmail
            mailSender.send(message);
            log.info("Success! Email sent to {}", to);

        } catch (MessagingException e) {
            // We catch and log the exception here. Because this runs on a background thread,
            // throwing an exception would just kill the thread silently.
            log.error("Failed to send email to {}", to, e);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }


}
