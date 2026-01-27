package nvt.backend.controllers.factory;

import lombok.RequiredArgsConstructor;
import nvt.backend.dto.factory.FactorySimpleDTO;
import nvt.backend.repositories.factory.FactoryRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/factories")
@RequiredArgsConstructor
public class FactoryController {

    private final FactoryRepository factoryRepository;

    @GetMapping("/simple")
    @PreAuthorize("hasAnyAuthority('MANAGER', 'ADMIN')")
    public ResponseEntity<List<FactorySimpleDTO>> getAllSimple() {
        List<FactorySimpleDTO> factories = factoryRepository.findAllActiveOrderByName()
                .stream()
                .map(FactorySimpleDTO::fromEntity)
                .toList();
        return ResponseEntity.ok(factories);
    }
}
