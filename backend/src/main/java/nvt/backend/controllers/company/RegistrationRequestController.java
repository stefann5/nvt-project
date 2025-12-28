package nvt.backend.controllers.company;


import lombok.RequiredArgsConstructor;
import nvt.backend.dto.company.CreateRequestDTO;
import nvt.backend.model.company.RegistrationRequest;
import nvt.backend.model.user.User;
import nvt.backend.repositories.user.UserRepository;
import nvt.backend.services.company.RegistrationRequestService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/registration-requests")
@RequiredArgsConstructor
public class RegistrationRequestController {

    private final RegistrationRequestService service;
    private final UserRepository userRepository;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> create(
            @RequestPart("data") CreateRequestDTO dto,
            @RequestPart("images") List<MultipartFile> images,
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
}