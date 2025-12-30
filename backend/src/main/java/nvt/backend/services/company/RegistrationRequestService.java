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
import nvt.backend.services.storage.MinioService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RegistrationRequestService {

    private final RegistrationRequestRepository requestRepository;
    private final CountryRepository countryRepository;
    private final CityRepository cityRepository;
    private final MinioService minioService;

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

        // Upload images to MinIO
        String imagesBucket = minioService.getCompanyImagesBucket();
        for (MultipartFile file : images) {
            String minioPath = minioService.uploadFile(file, imagesBucket, "company-" + request.getId());

            CompanyImage img = new CompanyImage();
            img.setOriginalName(file.getOriginalFilename());
            img.setMinioPath(minioPath);
            img.setMinioBucket(imagesBucket);
            img.setRequest(request);
            request.getImages().add(img);
        }

        // Upload documents to MinIO
        String documentsBucket = minioService.getCompanyDocumentsBucket();
        for (MultipartFile file : documents) {
            String minioPath = minioService.uploadFile(file, documentsBucket, "company-" + request.getId());

            CompanyDocument doc = new CompanyDocument();
            doc.setOriginalName(file.getOriginalFilename());
            doc.setMinioPath(minioPath);
            doc.setMinioBucket(documentsBucket);
            doc.setContentType(file.getContentType());
            doc.setRequest(request);
            request.getDocuments().add(doc);
        }

        return requestRepository.save(request);
    }
}