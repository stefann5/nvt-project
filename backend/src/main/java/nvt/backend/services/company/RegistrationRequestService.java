package nvt.backend.services.company;

import lombok.RequiredArgsConstructor;
import nvt.backend.dto.company.CreateRequestDTO;
import nvt.backend.model.common.City;
import nvt.backend.model.common.Country;
import nvt.backend.model.company.CompanyDocument;
import nvt.backend.model.company.CompanyImage;
import nvt.backend.model.company.RegistrationRequest;
import nvt.backend.model.user.User;
import nvt.backend.repositories.common.CityRepository;
import nvt.backend.repositories.common.CountryRepository;
import nvt.backend.repositories.company.RegistrationRequestRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RegistrationRequestService {

    private final RegistrationRequestRepository requestRepository;
    private final CountryRepository countryRepository;
    private final CityRepository cityRepository;

    @Value("${app.upload.path:./uploads}")
    private String uploadPath;

    @Transactional
    public RegistrationRequest create(CreateRequestDTO dto,
                                      List<MultipartFile> images,
                                      List<MultipartFile> documents,
                                      User owner) throws IOException {

        Country country = countryRepository.findById(dto.getCountryId())
                .orElseThrow(() -> new RuntimeException("Country not found"));

        City city = cityRepository.findById(dto.getCityId())
                .orElseThrow(() -> new RuntimeException("City not found"));

        RegistrationRequest request = new RegistrationRequest();
        request.setName(dto.getName());
        request.setCountry(country);
        request.setCity(city);
        request.setStreet(dto.getStreet());
        request.setLatitude(dto.getLatitude());
        request.setLongitude(dto.getLongitude());
        request.setOwner(owner);

        request = requestRepository.save(request);

        for (MultipartFile file : images) {
            String path = saveFile(file, "images");
            CompanyImage img = new CompanyImage();
            img.setOriginalName(file.getOriginalFilename());
            img.setPath(path);
            img.setRequest(request);
            request.getImages().add(img);
        }

        for (MultipartFile file : documents) {
            String path = saveFile(file, "documents");
            CompanyDocument doc = new CompanyDocument();
            doc.setOriginalName(file.getOriginalFilename());
            doc.setPath(path);
            doc.setContentType(file.getContentType());
            doc.setRequest(request);
            request.getDocuments().add(doc);
        }

        return requestRepository.save(request);
    }

    private String saveFile(MultipartFile file, String folder) throws IOException {
        Path dir = Paths.get(uploadPath, folder);
        Files.createDirectories(dir);

        String filename = UUID.randomUUID() + "_" + file.getOriginalFilename();
        Path filePath = dir.resolve(filename);
        Files.copy(file.getInputStream(), filePath);

        return folder + "/" + filename;
    }
}