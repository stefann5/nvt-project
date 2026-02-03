package nvt.backend.services.factory;

import jakarta.persistence.OptimisticLockException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nvt.backend.dto.common.PageResponseDTO;
import nvt.backend.dto.factory.*;
import nvt.backend.model.common.City;
import nvt.backend.model.common.Country;
import nvt.backend.model.factory.Factory;
import nvt.backend.model.factory.FactoryImage;
import nvt.backend.model.product.Product;
import nvt.backend.repositories.common.CityRepository;
import nvt.backend.repositories.common.CountryRepository;
import nvt.backend.repositories.factory.FactoryImageRepository;
import nvt.backend.repositories.factory.FactoryRepository;
import nvt.backend.repositories.product.ProductRepository;
import nvt.backend.services.storage.MinioService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class FactoryService {

    private final FactoryRepository factoryRepository;
    private final FactoryImageRepository factoryImageRepository;
    private final CountryRepository countryRepository;
    private final CityRepository cityRepository;
    private final ProductRepository productRepository;
    private final MinioService minioService;
    private final FactoryTelemetryService telemetryService;

    @Value("${minio.bucket.factory-images:factory-images}")
    private String factoryImagesBucket;

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "factoriesPage", allEntries = true),
        @CacheEvict(value = "factorySearch", allEntries = true),
        @CacheEvict(value = "factoryCount", allEntries = true)
    })
    @Retryable(
        retryFor = {OptimisticLockException.class, OptimisticLockingFailureException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 100, multiplier = 2)
    )
    public FactoryResponseDTO create(CreateFactoryDTO dto, List<MultipartFile> images) throws IOException {
        log.debug("Creating factory: {}", dto.getName());

        if (factoryRepository.existsByName(dto.getName())) {
            throw new RuntimeException("Factory with this name already exists");
        }

        Country country = countryRepository.findById(dto.getCountryId())
                .orElseThrow(() -> new RuntimeException("Country not found"));

        City city = cityRepository.findById(dto.getCityId())
                .orElseThrow(() -> new RuntimeException("City not found"));

        if (!city.getCountry().getId().equals(country.getId())) {
            throw new RuntimeException("City does not belong to the selected country");
        }

        Factory factory = new Factory();
        factory.setName(dto.getName());
        factory.setDescription(dto.getDescription());
        factory.setCountry(country);
        factory.setCity(city);
        factory.setStreet(dto.getStreet());
        factory.setStreetNumber(dto.getStreetNumber());
        factory.setLatitude(dto.getLatitude());
        factory.setLongitude(dto.getLongitude());
        factory.setActive(true);
        factory.setOnline(false);

        factory = factoryRepository.save(factory);

        // Upload images
        minioService.createBucketIfNotExists(factoryImagesBucket);

        for (MultipartFile file : images) {
            String minioPath = minioService.uploadFile(file, factoryImagesBucket, "factory-" + factory.getId());

            FactoryImage img = new FactoryImage();
            img.setOriginalName(file.getOriginalFilename());
            img.setMinioPath(minioPath);
            img.setMinioBucket(factoryImagesBucket);
            img.setContentType(file.getContentType());
            img.setFactory(factory);
            factory.getImages().add(img);
        }

        factory = factoryRepository.save(factory);

        return FactoryResponseDTO.fromEntity(factory);
    }

    @Transactional(readOnly = true)
    public List<FactoryListDTO> getAll() {
        return factoryRepository.findAllActiveWithDetails().stream()
                .map(FactoryListDTO::fromEntity)
                .toList();
    }

    @Cacheable(value = "factoryCount")
    public long getTotalFactoryCount() {
        return factoryRepository.countByActiveTrue();
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "factoriesPage", key = "#page + '-' + #size + '-' + #sortBy + '-' + #sortDir")
    public PageResponseDTO<FactoryListDTO> getAllPaged(int page, int size, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Long> idsPage = factoryRepository.findAllActiveIds(pageable);
        List<Factory> factories = factoryRepository.findAllByIdsWithDetails(idsPage.getContent());

        List<FactoryListDTO> dtos = factories.stream()
                .map(FactoryListDTO::fromEntity)
                .toList();

        return PageResponseDTO.<FactoryListDTO>builder()
                .content(dtos)
                .page(page)
                .size(size)
                .totalElements(idsPage.getTotalElements())
                .totalPages(idsPage.getTotalPages())
                .first(idsPage.isFirst())
                .last(idsPage.isLast())
                .build();
    }

    @Transactional(readOnly = true)
    public FactoryResponseDTO getById(Long id) {
        Factory factory = factoryRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new RuntimeException("Factory not found"));
        return FactoryResponseDTO.fromEntity(factory);
    }

    @Transactional(readOnly = true)
    public List<FactoryListDTO> search(String query) {
        return factoryRepository.searchByName(query).stream()
                .map(FactoryListDTO::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "factorySearch", key = "#query + '-' + #page + '-' + #size")
    public PageResponseDTO<FactoryListDTO> searchPaged(String query, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);

        Page<Long> idsPage = factoryRepository.searchFactoryIds(query, pageable);
        List<Factory> factories = factoryRepository.findAllByIdsWithDetails(idsPage.getContent());

        List<FactoryListDTO> dtos = factories.stream()
                .map(FactoryListDTO::fromEntity)
                .toList();

        return PageResponseDTO.<FactoryListDTO>builder()
                .content(dtos)
                .page(page)
                .size(size)
                .totalElements(idsPage.getTotalElements())
                .totalPages(idsPage.getTotalPages())
                .first(idsPage.isFirst())
                .last(idsPage.isLast())
                .build();
    }

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "factoriesPage", allEntries = true),
        @CacheEvict(value = "factorySearch", allEntries = true)
    })
    @Retryable(
        retryFor = {OptimisticLockException.class, OptimisticLockingFailureException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 100, multiplier = 2)
    )
    public FactoryResponseDTO update(Long id, UpdateFactoryDTO dto, List<MultipartFile> newImages) throws IOException {
        log.debug("Updating factory: {}", id);

        Factory factory = factoryRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new RuntimeException("Factory not found"));

        // Check for duplicate name (excluding current factory)
        if (!factory.getName().equals(dto.getName()) && factoryRepository.existsByName(dto.getName())) {
            throw new RuntimeException("Factory with this name already exists");
        }

        Country country = countryRepository.findById(dto.getCountryId())
                .orElseThrow(() -> new RuntimeException("Country not found"));

        City city = cityRepository.findById(dto.getCityId())
                .orElseThrow(() -> new RuntimeException("City not found"));

        if (!city.getCountry().getId().equals(country.getId())) {
            throw new RuntimeException("City does not belong to the selected country");
        }

        factory.setName(dto.getName());
        factory.setDescription(dto.getDescription());
        factory.setCountry(country);
        factory.setCity(city);
        factory.setStreet(dto.getStreet());
        factory.setStreetNumber(dto.getStreetNumber());
        factory.setLatitude(dto.getLatitude());
        factory.setLongitude(dto.getLongitude());

        // Delete specified images
        if (dto.getImagesToDelete() != null && !dto.getImagesToDelete().isEmpty()) {
            List<FactoryImage> imagesToRemove = factory.getImages().stream()
                    .filter(img -> dto.getImagesToDelete().contains(img.getId()))
                    .toList();

            for (FactoryImage img : imagesToRemove) {
                try {
                    minioService.deleteFile(img.getMinioBucket(), img.getMinioPath());
                } catch (Exception e) {
                    log.warn("Failed to delete image from MinIO: {}", img.getMinioPath(), e);
                }
                factory.getImages().remove(img);
            }
        }

        // Add new images
        if (newImages != null && !newImages.isEmpty()) {
            minioService.createBucketIfNotExists(factoryImagesBucket);

            for (MultipartFile file : newImages) {
                String minioPath = minioService.uploadFile(file, factoryImagesBucket, "factory-" + factory.getId());

                FactoryImage img = new FactoryImage();
                img.setOriginalName(file.getOriginalFilename());
                img.setMinioPath(minioPath);
                img.setMinioBucket(factoryImagesBucket);
                img.setContentType(file.getContentType());
                img.setFactory(factory);
                factory.getImages().add(img);
            }
        }

        factory = factoryRepository.save(factory);

        return FactoryResponseDTO.fromEntity(factory);
    }

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "factoriesPage", allEntries = true),
        @CacheEvict(value = "factorySearch", allEntries = true),
        @CacheEvict(value = "factoryCount", allEntries = true)
    })
    public void delete(Long id) {
        log.debug("Deleting factory: {}", id);

        Factory factory = factoryRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new RuntimeException("Factory not found"));

        // Remove factory from all products
        for (Product product : factory.getProducts()) {
            product.getFactories().remove(factory);
        }

        // Delete images from MinIO
        for (FactoryImage img : factory.getImages()) {
            try {
                minioService.deleteFile(img.getMinioBucket(), img.getMinioPath());
            } catch (Exception e) {
                log.warn("Failed to delete image from MinIO: {}", img.getMinioPath(), e);
            }
        }

        // Physical delete
        factoryRepository.delete(factory);
    }

    public List<Country> getAllCountries() {
        return countryRepository.findAllByOrderByNameAsc();
    }

    public List<City> getCitiesByCountry(Long countryId) {
        return cityRepository.findByCountryIdOrderByNameAsc(countryId);
    }

    @Transactional
    public void updateHeartbeat(Long factoryId) {
        Factory factory = factoryRepository.findById(factoryId)
                .orElseThrow(() -> new RuntimeException("Factory not found"));
        factory.setOnline(true);
        factory.setLastHeartbeat(LocalDateTime.now());
        factoryRepository.save(factory);
    }

    @Transactional
    public void setOffline(Long factoryId) {
        Factory factory = factoryRepository.findById(factoryId).orElse(null);
        if (factory != null) {
            factory.setOnline(false);
            factoryRepository.save(factory);
        }
    }

    public List<FactorySimpleDTO> getAllSimple() {
        return factoryRepository.findAllActiveOrderByName().stream()
                .map(FactorySimpleDTO::fromEntity)
                .toList();
    }

    public FactoryProductionStatisticsDTO getProductionStatistics(Long factoryId, Long productId, 
                                                                   LocalDate startDate, LocalDate endDate) {
        Factory factory = factoryRepository.findByIdWithDetails(factoryId)
                .orElseThrow(() -> new RuntimeException("Factory not found"));

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        // Verify product belongs to factory
        if (!factory.getProducts().contains(product)) {
            throw new RuntimeException("Product does not belong to this factory");
        }

        return telemetryService.getAggregatedProduction(
                factory.getId(), 
                factory.getName(), 
                product.getId(), 
                product.getName(), 
                startDate, 
                endDate
        );
    }

    public FactoryAvailabilityStatisticsDTO getAvailabilityStatistics(Long factoryId, 
                                                                       LocalDate startDate, LocalDate endDate) {
        Factory factory = factoryRepository.findByIdWithDetails(factoryId)
                .orElseThrow(() -> new RuntimeException("Factory not found"));

        return telemetryService.getAvailabilityStatistics(
                factory.getId(),
                factory.getName(),
                startDate,
                endDate
        );
    }
}
