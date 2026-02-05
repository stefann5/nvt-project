package nvt.backend.controllers.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import nvt.backend.dto.user.CreateManagerDTO;
import nvt.backend.dto.user.ManagerResponseDTO;
import nvt.backend.services.user.ManagerService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/managers")
@RequiredArgsConstructor
@CrossOrigin
public class ManagerController {

    private final ManagerService managerService;
    private final ObjectMapper objectMapper;

    /**
     * Create a new manager (only ADMIN/Super Manager can do this)
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ManagerResponseDTO> createManager(
            @RequestPart("manager") String managerJson,
            @RequestPart(value = "profileImage", required = false) MultipartFile profileImage) {
        try {
            CreateManagerDTO dto = objectMapper.readValue(managerJson, CreateManagerDTO.class);
            ManagerResponseDTO response = managerService.createManager(dto, profileImage);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create manager: " + e.getMessage());
        }
    }

    /**
     * Get all managers with pagination
     */
    @GetMapping("/paged")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Page<ManagerResponseDTO>> getAllManagersPaged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        
        Sort sort = sortDir.equalsIgnoreCase("desc") 
            ? Sort.by(sortBy).descending() 
            : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        return ResponseEntity.ok(managerService.getAllManagers(pageable));
    }

    /**
     * Get all managers (no pagination)
     */
    @GetMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<List<ManagerResponseDTO>> getAllManagers() {
        return ResponseEntity.ok(managerService.getAllManagers());
    }

    /**
     * Search managers
     */
    @GetMapping("/search")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Page<ManagerResponseDTO>> searchManagers(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(managerService.searchManagers(query, pageable));
    }

    /**
     * Get manager by ID
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ManagerResponseDTO> getManagerById(@PathVariable int id) {
        return ResponseEntity.ok(managerService.getManagerById(id));
    }

    /**
     * Block a manager
     */
    @PutMapping("/{id}/block")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ManagerResponseDTO> blockManager(@PathVariable int id) {
        return ResponseEntity.ok(managerService.blockManager(id));
    }

    /**
     * Unblock a manager
     */
    @PutMapping("/{id}/unblock")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ManagerResponseDTO> unblockManager(@PathVariable int id) {
        return ResponseEntity.ok(managerService.unblockManager(id));
    }

    /**
     * Get profile image URL
     */
    @GetMapping("/{id}/profile-image")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<String> getProfileImageUrl(@PathVariable int id) {
        String url = managerService.getProfileImageUrl(id);
        if (url == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(url);
    }
}
