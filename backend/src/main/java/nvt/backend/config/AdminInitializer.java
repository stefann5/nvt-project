package nvt.backend.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nvt.backend.model.auth.Role;
import nvt.backend.model.user.Manager;
import nvt.backend.repositories.user.ManagerRepository;
import nvt.backend.repositories.user.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.Base64;

@Component
@RequiredArgsConstructor
@Slf4j
@Order(1)
public class AdminInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final ManagerRepository managerRepository;
    private final PasswordEncoder passwordEncoder;

    private static final String ADMIN_USERNAME = "admin";
    private static final String PASSWORD_FILE_PATH = "admin_password.txt";

    @Override
    public void run(String... args) {
        if (userRepository.findByUsername(ADMIN_USERNAME).isEmpty()) {
            createSuperAdmin();
        } else {
            log.info("Super admin already exists, skipping initialization");
        }
    }

    private void createSuperAdmin() {
        String generatedPassword = generateSecurePassword();

        Manager superAdmin = new Manager();
        superAdmin.setUsername(ADMIN_USERNAME);
        superAdmin.setPassword(passwordEncoder.encode(generatedPassword));
        superAdmin.setName("Super");
        superAdmin.setSurname("Admin");
        superAdmin.setRole(Role.S);
        superAdmin.setAuthorities("ADMIN");
        superAdmin.setActive(true);
        superAdmin.setMustChangePassword(true);
        superAdmin.setBlocked(false);

        managerRepository.save(superAdmin);

        savePasswordToFile(generatedPassword);

        log.info("Super admin created successfully. Password saved to: {}", PASSWORD_FILE_PATH);
    }

    private String generateSecurePassword() {
        SecureRandom secureRandom = new SecureRandom();
        byte[] randomBytes = new byte[24];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    private void savePasswordToFile(String password) {
        try {
            Path path = Paths.get(PASSWORD_FILE_PATH);
            Files.writeString(path, 
                "===========================================\n" +
                "SUPER ADMIN CREDENTIALS\n" +
                "===========================================\n" +
                "Username: " + ADMIN_USERNAME + "\n" +
                "Password: " + password + "\n" +
                "===========================================\n" +
                "IMPORTANT: Change this password immediately after first login!\n" +
                "This file should be deleted after you note the password.\n" +
                "===========================================\n"
            );
            log.info("Admin password saved to file: {}", path.toAbsolutePath());
        } catch (IOException e) {
            log.error("Failed to save admin password to file", e);
            throw new RuntimeException("Failed to save admin password to file", e);
        }
    }
}
