package com.andrii.taskmanagement.config;

import com.andrii.taskmanagement.exception.EntityNotFoundException;
import com.andrii.taskmanagement.model.Role;
import com.andrii.taskmanagement.model.RoleName;
import com.andrii.taskmanagement.model.User;
import com.andrii.taskmanagement.repository.user.RoleRepository;
import com.andrii.taskmanagement.repository.user.UserRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AdminInitializer implements CommandLineRunner {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.username}")
    private String adminUsername;

    @Value("${app.admin.email}")
    private String adminEmail;

    @Value("${app.admin.password}")
    private String adminPassword;

    @Value("${app.admin.first-name}")
    private String adminFirstName;

    @Value("${app.admin.last-name}")
    private String adminLastName;

    @Override
    public void run(String... args) {
        if (userRepository.existsByEmail(adminEmail)
                || userRepository.existsByUsername(adminUsername)) {
            return;
        }

        Role role = roleRepository.findByName(RoleName.ROLE_ADMIN)
                .orElseThrow(() -> new EntityNotFoundException(
                        RoleName.ROLE_ADMIN + " not found"
                ));

        User admin = new User();
        admin.setUsername(adminUsername);
        admin.setEmail(adminEmail);
        admin.setPassword(passwordEncoder.encode(adminPassword));
        admin.setFirstName(adminFirstName);
        admin.setLastName(adminLastName);
        admin.setRole(role);

        LocalDateTime now = LocalDateTime.now();
        admin.setCreatedAt(now);
        admin.setUpdatedAt(now);

        userRepository.save(admin);
    }
}
