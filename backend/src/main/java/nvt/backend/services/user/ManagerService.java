package nvt.backend.services.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nvt.backend.dto.user.CreateManagerDTO;
import nvt.backend.dto.user.ManagerResponseDTO;
import nvt.backend.model.auth.Role;
import nvt.backend.model.user.Manager;
import nvt.backend.repositories.user.ManagerRepository;
import nvt.backend.repositories.user.UserRepository;
import nvt.backend.services.auth.EmailService;
import nvt.backend.services.storage.MinioService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ManagerService {

    private final ManagerRepository managerRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final MinioService minioService;
    private final EmailService emailService;

    @Value("${minio.bucket.profile-images:profile-images}")
    private String profileImagesBucket;

    /**
     * Create a new manager (only super admin can do this)
     */
    @Transactional
    public ManagerResponseDTO createManager(CreateManagerDTO dto, MultipartFile profileImage) {
        // Check if username already exists
        if (userRepository.findByUsername(dto.getUsername()).isPresent()) {
            throw new RuntimeException("User with this email already exists");
        }

        // Generate random password
        String generatedPassword = generateSecurePassword();

        // Create manager
        Manager manager = new Manager();
        manager.setUsername(dto.getUsername());
        manager.setPassword(passwordEncoder.encode(generatedPassword));
        manager.setName(dto.getName());
        manager.setSurname(dto.getSurname());
        manager.setPhoneNumber(dto.getPhoneNumber());
        manager.setRole(Role.M);
        manager.setAuthorities("MANAGER");
        manager.setActive(true);
        manager.setMustChangePassword(true);
        manager.setBlocked(false);

        // Upload profile image if provided
        if (profileImage != null && !profileImage.isEmpty()) {
            String photoPath = minioService.uploadFile(profileImage, profileImagesBucket, "managers");
            manager.setPhoto(photoPath);
        }

        manager = managerRepository.save(manager);

        // Send email with credentials
        sendCredentialsEmail(dto.getUsername(), generatedPassword, dto.getName());

        log.info("Manager created successfully: {}", dto.getUsername());

        return mapToDTO(manager);
    }

    /**
     * Get all managers with pagination
     */
    public Page<ManagerResponseDTO> getAllManagers(Pageable pageable) {
        return managerRepository.findAll(pageable)
                .map(this::mapToDTO);
    }

    /**
     * Get all managers (no pagination)
     */
    public List<ManagerResponseDTO> getAllManagers() {
        return managerRepository.findAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Search managers by name, surname, or email
     */
    public Page<ManagerResponseDTO> searchManagers(String query, Pageable pageable) {
        return managerRepository.searchManagers(query, pageable)
                .map(this::mapToDTO);
    }

    /**
     * Get manager by ID
     */
    public ManagerResponseDTO getManagerById(int id) {
        Manager manager = managerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Manager not found"));
        return mapToDTO(manager);
    }

    /**
     * Block a manager
     */
    @Transactional
    public ManagerResponseDTO blockManager(int id) {
        Manager manager = managerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Manager not found"));
        
        // Cannot block super admin
        if (manager.getRole() == Role.S) {
            throw new RuntimeException("Cannot block super admin");
        }

        manager.setBlocked(true);
        manager = managerRepository.save(manager);

        log.info("Manager blocked: {}", manager.getUsername());

        return mapToDTO(manager);
    }

    /**
     * Unblock a manager
     */
    @Transactional
    public ManagerResponseDTO unblockManager(int id) {
        Manager manager = managerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Manager not found"));

        manager.setBlocked(false);
        manager = managerRepository.save(manager);

        log.info("Manager unblocked: {}", manager.getUsername());

        return mapToDTO(manager);
    }

    /**
     * Get presigned URL for profile image
     */
    public String getProfileImageUrl(int managerId) {
        Manager manager = managerRepository.findById(managerId)
                .orElseThrow(() -> new RuntimeException("Manager not found"));

        if (manager.getPhoto() == null || manager.getPhoto().isEmpty()) {
            return null;
        }

        return minioService.getPresignedUrl(profileImagesBucket, manager.getPhoto(), 60);
    }

    private String generateSecurePassword() {
        SecureRandom secureRandom = new SecureRandom();
        byte[] randomBytes = new byte[16];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    private void sendCredentialsEmail(String email, String password, String name) {
        String subject = "Your Manager Account Has Been Created";
        String body = String.format(
            "<html><body>" +
            "<h2>Welcome to Smartly, %s!</h2>" +
            "<p>Your manager account has been created.</p>" +
            "<p><strong>Login Credentials:</strong></p>" +
            "<ul>" +
            "<li><strong>Username:</strong> %s</li>" +
            "<li><strong>Temporary Password:</strong> %s</li>" +
            "</ul>" +
            "<p><strong>Important:</strong> You will be required to change your password upon first login.</p>" +
            "<p>Best regards,<br>Smartly Team</p>" +
            "</body></html>",
            name, email, password
        );

        try {
            emailService.sendMail("system@smartly.com", email, subject, body);
            log.info("Credentials email sent to: {}", email);
        } catch (Exception e) {
            log.error("Failed to send credentials email to: {}", email, e);
            // Don't throw - manager is still created
        }
    }

    private ManagerResponseDTO mapToDTO(Manager manager) {
        return new ManagerResponseDTO(
            manager.getId(),
            manager.getUsername(),
            manager.getName(),
            manager.getSurname(),
            manager.getPhoneNumber(),
            manager.getPhoto() != null ? 
                minioService.getPresignedUrl(profileImagesBucket, manager.getPhoto(), 60) : null,
            manager.isActive(),
            manager.isBlocked(),
            manager.getRole().name()
        );
    }
}
