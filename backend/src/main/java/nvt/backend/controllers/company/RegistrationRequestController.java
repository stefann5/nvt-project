package nvt.backend.controllers.company;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import nvt.backend.dto.common.PageResponseDTO;
import nvt.backend.dto.company.CreateRequestDTO;
import nvt.backend.dto.company.ProcessRequestDTO;
import nvt.backend.dto.company.RegistrationRequestListDTO;
import nvt.backend.dto.company.RegistrationRequestResponseDTO;
import nvt.backend.model.company.RegistrationRequest;
import nvt.backend.model.user.User;
import nvt.backend.repositories.user.UserRepository;
import nvt.backend.services.company.RegistrationRequestService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("api/v1/registration-requests")
@RequiredArgsConstructor
@Tag(name = "Company Registration", description = "Company registration request management endpoints")
public class RegistrationRequestController {

    private final RegistrationRequestService service;
    private final UserRepository userRepository;

    @Operation(
            summary = "Create a company registration request",
            description = "Submits a new company registration request with images and documents. Requires authentication."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Registration request created successfully",
                    content = @Content(schema = @Schema(implementation = Long.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> create(
            @Parameter(description = "Company registration data", required = true)
            @RequestPart("data") CreateRequestDTO dto,
            @Parameter(description = "Company images", required = true)
            @RequestPart("images") List<MultipartFile> images,
            @Parameter(description = "Company documents", required = true)
            @RequestPart("documents") List<MultipartFile> documents,
            @AuthenticationPrincipal UserDetails userDetails) {

        try {
            User owner = userRepository.findByUsername(userDetails.getUsername()).orElseThrow();
            RegistrationRequest request = service.create(dto, images, documents, owner);
            return ResponseEntity.status(HttpStatus.CREATED).body(request.getId());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @Operation(
            summary = "Get all pending registration requests",
            description = "Retrieves all pending company registration requests. Requires MANAGER or ADMIN role."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List of pending requests retrieved successfully",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = RegistrationRequestListDTO.class)))),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @GetMapping("/pending")
    @PreAuthorize("hasAnyAuthority('MANAGER','ADMIN')")
    public ResponseEntity<List<RegistrationRequestListDTO>> getPendingRequests() {
        return ResponseEntity.ok(service.getPendingRequests());
    }

    @Operation(
            summary = "Get pending registration requests with pagination",
            description = "Retrieves pending company registration requests with pagination and sorting. Requires MANAGER or ADMIN role."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Paginated list of pending requests retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @GetMapping("/pending/paged")
    @PreAuthorize("hasAnyAuthority('MANAGER','ADMIN')")
    public ResponseEntity<PageResponseDTO<RegistrationRequestListDTO>> getPendingRequestsPaged(
            @Parameter(description = "Page number (0-based)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Number of items per page", example = "20")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Field to sort by", example = "createdAt")
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "Sort direction (asc/desc)", example = "desc")
            @RequestParam(defaultValue = "desc") String sortDir) {
        return ResponseEntity.ok(service.getPendingRequestsPaged(page, size, sortBy, sortDir));
    }

    @Operation(
            summary = "Get count of pending registration requests",
            description = "Returns the total number of pending company registration requests. Requires MANAGER or ADMIN role."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Count retrieved successfully",
                    content = @Content(schema = @Schema(implementation = Long.class))),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @GetMapping("/pending/count")
    @PreAuthorize("hasAnyAuthority('MANAGER','ADMIN')")
    public ResponseEntity<Long> getPendingRequestsCount() {
        return ResponseEntity.ok(service.getPendingRequestsCount());
    }

    @Operation(
            summary = "Get all registration requests",
            description = "Retrieves all company registration requests regardless of status. Requires MANAGER or ADMIN role."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List of all requests retrieved successfully",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = RegistrationRequestListDTO.class)))),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @GetMapping("/all")
    @PreAuthorize("hasAnyAuthority('MANAGER','ADMIN')")
    public ResponseEntity<List<RegistrationRequestListDTO>> getAllRequests() {
        return ResponseEntity.ok(service.getAllRequests());
    }

    @Operation(
            summary = "Get all registration requests with pagination",
            description = "Retrieves all company registration requests with pagination and sorting. Requires MANAGER or ADMIN role."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Paginated list of all requests retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @GetMapping("/all/paged")
    @PreAuthorize("hasAnyAuthority('MANAGER','ADMIN')")
    public ResponseEntity<PageResponseDTO<RegistrationRequestListDTO>> getAllRequestsPaged(
            @Parameter(description = "Page number (0-based)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Number of items per page", example = "20")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Field to sort by", example = "createdAt")
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "Sort direction (asc/desc)", example = "desc")
            @RequestParam(defaultValue = "desc") String sortDir) {
        return ResponseEntity.ok(service.getAllRequestsPaged(page, size, sortBy, sortDir));
    }

    @Operation(
            summary = "Get registration request by ID",
            description = "Retrieves a specific company registration request by its ID. Requires MANAGER or ADMIN role."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Request retrieved successfully",
                    content = @Content(schema = @Schema(implementation = RegistrationRequestResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Request not found"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('MANAGER','ADMIN')")
    public ResponseEntity<?> getRequestById(
            @Parameter(description = "Registration request ID", required = true)
            @PathVariable Long id) {
        try {
            return ResponseEntity.ok(service.getRequestById(id));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @Operation(
            summary = "Process a registration request",
            description = "Approves or rejects a company registration request. Requires MANAGER or ADMIN role."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Request processed successfully",
                    content = @Content(schema = @Schema(implementation = RegistrationRequestResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid processing data"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @PutMapping("/{id}/process")
    @PreAuthorize("hasAnyAuthority('MANAGER','ADMIN')")
    public ResponseEntity<?> processRequest(
            @Parameter(description = "Registration request ID", required = true)
            @PathVariable Long id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Processing decision details",
                    required = true,
                    content = @Content(schema = @Schema(implementation = ProcessRequestDTO.class))
            )
            @RequestBody ProcessRequestDTO dto) {
        try {
            RegistrationRequestResponseDTO response = service.processRequest(id, dto);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @Operation(
            summary = "Get my approved companies",
            description = "Retrieves all approved companies owned by the authenticated customer. Requires CUSTOMER role."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List of approved companies retrieved successfully",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = RegistrationRequestListDTO.class)))),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @GetMapping("/my-companies")
    @PreAuthorize("hasAnyAuthority('CUSTOMER')")
    public ResponseEntity<List<RegistrationRequestListDTO>> getMyApprovedCompanies(
            @AuthenticationPrincipal UserDetails userDetails) {
        User owner = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        return ResponseEntity.ok(service.getApprovedCompaniesByOwner(owner.getId()));
    }
}