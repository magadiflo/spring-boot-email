package com.magadiflo.app.service.impl;

import com.magadiflo.app.domain.Confirmation;
import com.magadiflo.app.domain.User;
import com.magadiflo.app.repository.IConfirmationRepository;
import com.magadiflo.app.repository.IUserRepository;
import com.magadiflo.app.service.IEmailService;
import com.magadiflo.app.service.IUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class UserServiceImpl implements IUserService {

    private final IUserRepository userRepository;
    private final IConfirmationRepository confirmationRepository;
    private final IEmailService emailService;

    @Override
    @Transactional
    public User saveUser(User user) {
        if (this.userRepository.existsByEmail(user.getEmail())) {
            throw new RuntimeException(String.format("El email %s ya existe", user.getEmail()));
        }

        user.setEnabled(false);
        this.userRepository.save(user);

        Confirmation confirmation = new Confirmation(user);
        this.confirmationRepository.save(confirmation);

        // Enviando email a usuarios de forma asíncrona
        //this.emailService.sendSimpleMailMessage(user.getName(), user.getEmail(), confirmation.getToken());
        //this.emailService.sendMimeMessageWithAttachments(user.getName(), user.getEmail(), confirmation.getToken());
        //this.emailService.sendMimeMessageWithEmbeddedImages(user.getName(), user.getEmail(), confirmation.getToken());
        this.emailService.sendHtmlEmail(user.getName(), user.getEmail(), confirmation.getToken());


        return user;
    }

    @Override
    @Transactional
    public Boolean verifyToken(String token) {
        return this.confirmationRepository.findByToken(token)
                .map(confirmationDB -> {

                    String email = confirmationDB.getUser().getEmail();
                    User userDB = this.userRepository.findByEmailIgnoreCase(email)
                            .orElseThrow(() -> new RuntimeException(String.format("No existe el email %s", email)));

                    userDB.setEnabled(true);
                    this.userRepository.save(userDB);
                    this.confirmationRepository.delete(confirmationDB);

                    return Boolean.TRUE;
                })
                .orElseGet(() -> Boolean.FALSE);
    }
}
