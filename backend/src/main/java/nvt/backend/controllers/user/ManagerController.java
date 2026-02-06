package nvt.backend.controllers.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Manager", description = "Manager user management endpoints (ADMIN only)")
public class ManagerController {

    private final ManagerService managerService;
    private final ObjectMapper objectMapper;

    @Operation(
            summary = "Create a new manager",
            description = "Creates a new manager user with optional profile image. Requires ADMIN role."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Manager created successfully",
                    content = @Content(schema = @Schema(implementation = ManagerResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid manager data"),
            @ApiResponse(responseCode = "403", description = "Access denied - ADMIN role required")
    })
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ManagerResponseDTO> createManager(
            @Parameter(description = "Manager data as JSON string", required = true)
            @RequestPart("manager") String managerJson,
            @Parameter(description = "Profile image (optional)")
            @RequestPart(value = "profileImage", required = false) MultipartFile profileImage) {
        try {
            CreateManagerDTO dto = objectMapper.readValue(managerJson, CreateManagerDTO.class);
            ManagerResponseDTO response = managerService.createManager(dto, profileImage);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create manager: " + e.getMessage());
        }
    }

    @Operation(
            summary = "Get all managers (paginated)",
            description = "Retrieves all managers with pagination and sorting. Requires ADMIN role."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Managers retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "Access denied - ADMIN role required")
    })
    @GetMapping("/paged")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Page<ManagerResponseDTO>> getAllManagersPaged(
            @Parameter(description = "Page number (0-based)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Number of items per page", example = "10")
            @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Field to sort by", example = "name")
            @RequestParam(defaultValue = "name") String sortBy,
            @Parameter(description = "Sort direction (asc/desc)", example = "asc")
            @RequestParam(defaultValue = "asc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("desc")
            ? Sort.by(sortBy).descending()
            : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        return ResponseEntity.ok(managerService.getAllManagers(pageable));
    }

    @Operation(
            summary = "Get all managers",
            description = "Retrieves all managers without pagination. Requires ADMIN role."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Managers retrieved successfully",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = ManagerResponseDTO.class)))),
            @ApiResponse(responseCode = "403", description = "Access denied - ADMIN role required")
    })
    @GetMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<List<ManagerResponseDTO>> getAllManagers() {
        return ResponseEntity.ok(managerService.getAllManagers());
    }

    @Operation(
            summary = "Search managers",
            description = "Searches managers by query string with pagination. Requires ADMIN role."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Search results retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "Access denied - ADMIN role required")
    })
    @GetMapping("/search")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Page<ManagerResponseDTO>> searchManagers(
            @Parameter(description = "Search query", required = true)
            @RequestParam String query,
            @Parameter(description = "Page number (0-based)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Number of items per page", example = "10")
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(managerService.searchManagers(query, pageable));
    }

    @Operation(
            summary = "Get manager by ID",
            description = "Retrieves a specific manager by their ID. Requires ADMIN role."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Manager retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ManagerResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Manager not found"),
            @ApiResponse(responseCode = "403", description = "Access denied - ADMIN role required")
    })
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ManagerResponseDTO> getManagerById(
            @Parameter(description = "Manager ID", required = true)
            @PathVariable int id) {
        return ResponseEntity.ok(managerService.getManagerById(id));
    }

    @Operation(
            summary = "Block a manager",
            description = "Blocks a manager account, preventing them from logging in. Requires ADMIN role."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Manager blocked successfully",
                    content = @Content(schema = @Schema(implementation = ManagerResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Manager not found"),
            @ApiResponse(responseCode = "403", description = "Access denied - ADMIN role required")
    })
    @PutMapping("/{id}/block")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ManagerResponseDTO> blockManager(
            @Parameter(description = "Manager ID", required = true)
            @PathVariable int id) {
        return ResponseEntity.ok(managerService.blockManager(id));
    }

    @Operation(
            summary = "Unblock a manager",
            description = "Unblocks a previously blocked manager account. Requires ADMIN role."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Manager unblocked successfully",
                    content = @Content(schema = @Schema(implementation = ManagerResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Manager not found"),
            @ApiResponse(responseCode = "403", description = "Access denied - ADMIN role required")
    })
    @PutMapping("/{id}/unblock")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ManagerResponseDTO> unblockManager(
            @Parameter(description = "Manager ID", required = true)
            @PathVariable int id) {
        return ResponseEntity.ok(managerService.unblockManager(id));
    }

    @Operation(
            summary = "Get manager profile image URL",
            description = "Retrieves the presigned URL for a manager's profile image. Requires ADMIN role."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Profile image URL retrieved successfully",
                    content = @Content(schema = @Schema(implementation = String.class))),
            @ApiResponse(responseCode = "404", description = "Manager or profile image not found"),
            @ApiResponse(responseCode = "403", description = "Access denied - ADMIN role required")
    })
    @GetMapping("/{id}/profile-image")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<String> getProfileImageUrl(
            @Parameter(description = "Manager ID", required = true)
            @PathVariable int id) {
        String url = managerService.getProfileImageUrl(id);
        if (url == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(url);
    }
}
