package nvt.backend.services.warehouse;

import lombok.RequiredArgsConstructor;
import nvt.backend.dto.common.PageResponseDTO;
import nvt.backend.dto.warehouse.*;
import nvt.backend.model.common.City;
import nvt.backend.model.common.Country;
import nvt.backend.model.warehouse.Warehouse;
import nvt.backend.model.warehouse.WarehouseImage;
import nvt.backend.model.warehouse.WarehouseSector;
import nvt.backend.repositories.common.CityRepository;
import nvt.backend.repositories.common.CountryRepository;
import nvt.backend.repositories.warehouse.WarehouseRepository;
import nvt.backend.repositories.warehouse.WarehouseSectorRepository;
import nvt.backend.services.storage.MinioService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WarehouseService {

    private final WarehouseRepository warehouseRepository;
    private final WarehouseSectorRepository sectorRepository;
    private final CountryRepository countryRepository;
    private final CityRepository cityRepository;
    private final WarehouseTelemetryService telemetryService;
    private final MinioService minioService;

    @Value("${minio.bucket.warehouse-images:warehouse-images}")
    private String warehouseImagesBucket;

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "warehousesPage", allEntries = true),
        @CacheEvict(value = "warehouseSearch", allEntries = true)
    })
    public WarehouseResponseDTO create(CreateWarehouseDTO dto, List<MultipartFile> images) throws IOException {
        if (warehouseRepository.existsByName(dto.getName())) {
            throw new RuntimeException("Warehouse with this name already exists");
        }

        Country country = countryRepository.findById(dto.getCountryId())
                .orElseThrow(() -> new RuntimeException("Country not found"));

        City city = cityRepository.findById(dto.getCityId())
                .orElseThrow(() -> new RuntimeException("City not found"));

        if (!city.getCountry().getId().equals(country.getId())) {
            throw new RuntimeException("City does not belong to the selected country");
        }

        Warehouse warehouse = new Warehouse();
        warehouse.setName(dto.getName());
        warehouse.setCountry(country);
        warehouse.setCity(city);
        warehouse.setStreet(dto.getStreet());
        warehouse.setStreetNumber(dto.getStreetNumber());
        warehouse.setLatitude(dto.getLatitude());
        warehouse.setLongitude(dto.getLongitude());
        warehouse.setTotalCapacity(dto.getTotalCapacity());
        warehouse.setActive(true);
        warehouse.setOnline(false);

        if (dto.getSectors() != null && !dto.getSectors().isEmpty()) {
            Set<WarehouseSector> sectors = new HashSet<>();
            for (CreateWarehouseSectorDTO sectorDto : dto.getSectors()) {
                WarehouseSector sector = new WarehouseSector();
                sector.setName(sectorDto.getName());
                sector.setMinTemperature(sectorDto.getMinTemperature());
                sector.setMaxTemperature(sectorDto.getMaxTemperature());
                sector.setCapacity(sectorDto.getCapacity());
                sector.setWarehouse(warehouse);
                sectors.add(sector);
            }
            warehouse.setSectors(sectors);
        }

        warehouse = warehouseRepository.save(warehouse);

        minioService.createBucketIfNotExists(warehouseImagesBucket);

        for (MultipartFile file : images) {
            String minioPath = minioService.uploadFile(file, warehouseImagesBucket, "warehouse-" + warehouse.getId());

            WarehouseImage img = new WarehouseImage();
            img.setOriginalName(file.getOriginalFilename());
            img.setMinioPath(minioPath);
            img.setMinioBucket(warehouseImagesBucket);
            img.setWarehouse(warehouse);
            warehouse.getImages().add(img);
        }

        warehouse = warehouseRepository.save(warehouse);

        return WarehouseResponseDTO.fromEntity(warehouse);
    }

    public List<WarehouseListDTO> getAll() {
        return warehouseRepository.findAllWithDetails().stream()
                .map(WarehouseListDTO::fromEntity)
                .toList();
    }

    @Cacheable(value = "warehousesPage", key = "#page + '-' + #size + '-' + #sortBy + '-' + #sortDir")
    public PageResponseDTO<WarehouseListDTO> getAllPaged(int page, int size, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Long> idPage = warehouseRepository.findAllIds(pageable);
        List<Warehouse> warehouses = idPage.getContent().isEmpty()
                ? List.of()
                : warehouseRepository.findAllByIds(idPage.getContent());

        return PageResponseDTO.<WarehouseListDTO>builder()
                .content(warehouses.stream().map(WarehouseListDTO::fromEntity).toList())
                .page(idPage.getNumber())
                .size(idPage.getSize())
                .totalElements(idPage.getTotalElements())
                .totalPages(idPage.getTotalPages())
                .first(idPage.isFirst())
                .last(idPage.isLast())
                .build();
    }

    @Cacheable(value = "warehouseById", key = "#id")
    public WarehouseResponseDTO getById(Long id) {
        Warehouse warehouse = warehouseRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new RuntimeException("Warehouse not found"));
        return WarehouseResponseDTO.fromEntity(warehouse);
    }

    public List<WarehouseListDTO> search(String query) {
        return warehouseRepository.searchWarehouses(query).stream()
                .map(WarehouseListDTO::fromEntity)
                .toList();
    }

    @Cacheable(value = "warehouseSearch", key = "#query + '-' + #page + '-' + #size")
    public PageResponseDTO<WarehouseListDTO> searchPaged(String query, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);

        Page<Long> idPage = warehouseRepository.searchWarehouseIds(query, pageable);
        List<Warehouse> warehouses = idPage.getContent().isEmpty()
                ? List.of()
                : warehouseRepository.findAllByIds(idPage.getContent());

        return PageResponseDTO.<WarehouseListDTO>builder()
                .content(warehouses.stream().map(WarehouseListDTO::fromEntity).toList())
                .page(idPage.getNumber())
                .size(idPage.getSize())
                .totalElements(idPage.getTotalElements())
                .totalPages(idPage.getTotalPages())
                .first(idPage.isFirst())
                .last(idPage.isLast())
                .build();
    }

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "warehouseById", key = "#id"),
        @CacheEvict(value = "warehousesPage", allEntries = true),
        @CacheEvict(value = "warehouseSearch", allEntries = true)
    })
    public WarehouseResponseDTO update(Long id, UpdateWarehouseDTO dto, List<MultipartFile> newImages) throws IOException {
        Warehouse warehouse = warehouseRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new RuntimeException("Warehouse not found"));

        if (dto.getName() != null && !dto.getName().equals(warehouse.getName())) {
            if (warehouseRepository.existsByNameAndIdNot(dto.getName(), id)) {
                throw new RuntimeException("Warehouse with this name already exists");
            }
            warehouse.setName(dto.getName());
        }

        if (dto.getCountryId() != null) {
            Country country = countryRepository.findById(dto.getCountryId())
                    .orElseThrow(() -> new RuntimeException("Country not found"));
            warehouse.setCountry(country);
        }

        if (dto.getCityId() != null) {
            City city = cityRepository.findById(dto.getCityId())
                    .orElseThrow(() -> new RuntimeException("City not found"));
            if (!city.getCountry().getId().equals(warehouse.getCountry().getId())) {
                throw new RuntimeException("City does not belong to the selected country");
            }
            warehouse.setCity(city);
        }

        if (dto.getStreet() != null) {
            warehouse.setStreet(dto.getStreet());
        }
        if (dto.getStreetNumber() != null) {
            warehouse.setStreetNumber(dto.getStreetNumber());
        }
        if (dto.getLatitude() != null) {
            warehouse.setLatitude(dto.getLatitude());
        }
        if (dto.getLongitude() != null) {
            warehouse.setLongitude(dto.getLongitude());
        }
        if (dto.getTotalCapacity() != null) {
            warehouse.setTotalCapacity(dto.getTotalCapacity());
        }

        if (dto.getSectors() != null) {
            Set<Long> updatedSectorIds = dto.getSectors().stream()
                    .filter(s -> s.getId() != null)
                    .map(UpdateWarehouseSectorDTO::getId)
                    .collect(Collectors.toSet());

            warehouse.getSectors().removeIf(sector ->
                    sector.getId() != null && !updatedSectorIds.contains(sector.getId()));

            for (UpdateWarehouseSectorDTO sectorDto : dto.getSectors()) {
                if (sectorDto.getId() != null) {
                    warehouse.getSectors().stream()
                            .filter(s -> s.getId().equals(sectorDto.getId()))
                            .findFirst()
                            .ifPresent(sector -> {
                                if (sectorDto.getName() != null) sector.setName(sectorDto.getName());
                                if (sectorDto.getMinTemperature() != null) sector.setMinTemperature(sectorDto.getMinTemperature());
                                if (sectorDto.getMaxTemperature() != null) sector.setMaxTemperature(sectorDto.getMaxTemperature());
                                if (sectorDto.getCapacity() != null) sector.setCapacity(sectorDto.getCapacity());
                            });
                } else {
                    WarehouseSector newSector = new WarehouseSector();
                    newSector.setName(sectorDto.getName());
                    newSector.setMinTemperature(sectorDto.getMinTemperature());
                    newSector.setMaxTemperature(sectorDto.getMaxTemperature());
                    newSector.setCapacity(sectorDto.getCapacity());
                    newSector.setWarehouse(warehouse);
                    warehouse.getSectors().add(newSector);
                }
            }
        }

        if (dto.getImagesToDelete() != null && !dto.getImagesToDelete().isEmpty()) {
            List<WarehouseImage> imagesToRemove = warehouse.getImages().stream()
                    .filter(img -> dto.getImagesToDelete().contains(img.getId()))
                    .toList();

            for (WarehouseImage img : imagesToRemove) {
                minioService.deleteFile(img.getMinioBucket(), img.getMinioPath());
                warehouse.getImages().remove(img);
            }
        }

        if (newImages != null && !newImages.isEmpty()) {
            for (MultipartFile file : newImages) {
                String minioPath = minioService.uploadFile(file, warehouseImagesBucket, "warehouse-" + warehouse.getId());

                WarehouseImage img = new WarehouseImage();
                img.setOriginalName(file.getOriginalFilename());
                img.setMinioPath(minioPath);
                img.setMinioBucket(warehouseImagesBucket);
                img.setWarehouse(warehouse);
                warehouse.getImages().add(img);
            }
        }

        warehouse.setUpdatedAt(LocalDateTime.now());
        warehouse = warehouseRepository.save(warehouse);

        return WarehouseResponseDTO.fromEntity(warehouse);
    }

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "warehouseById", key = "#id"),
        @CacheEvict(value = "warehousesPage", allEntries = true),
        @CacheEvict(value = "warehouseSearch", allEntries = true)
    })
    public void delete(Long id) {
        Warehouse warehouse = warehouseRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new RuntimeException("Warehouse not found"));

        for (WarehouseImage img : warehouse.getImages()) {
            minioService.deleteFile(img.getMinioBucket(), img.getMinioPath());
        }

        warehouseRepository.delete(warehouse);
    }

    @Cacheable(value = "countries")
    public List<Country> getAllCountries() {
        return countryRepository.findAll();
    }

    @Cacheable(value = "citiesByCountry", key = "#countryId")
    public List<City> getCitiesByCountry(Long countryId) {
        return cityRepository.findByCountryId(countryId);
    }

    @Cacheable(value = "temperatureStats", key = "#warehouseId + '-' + #sectorId + '-' + #startDate + '-' + #endDate")
    public TemperatureStatisticsDTO getTemperatureStatistics(Long warehouseId, Long sectorId, LocalDate startDate, LocalDate endDate) {
        Warehouse warehouse = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new RuntimeException("Warehouse not found"));

        WarehouseSector sector = sectorRepository.findById(sectorId)
                .orElseThrow(() -> new RuntimeException("Sector not found"));

        if (!sector.getWarehouse().getId().equals(warehouseId)) {
            throw new RuntimeException("Sector does not belong to the specified warehouse");
        }

        return telemetryService.getAggregatedTemperature(warehouseId, warehouse.getName(), sectorId, sector.getName(), startDate, endDate);
    }

    public WarehouseResponseDTO.SectorDTO getSectorWithCurrentTemperature(Long warehouseId, Long sectorId) {
        WarehouseSector sector = sectorRepository.findByIdWithWarehouse(sectorId)
                .orElseThrow(() -> new RuntimeException("Sector not found"));

        if (!sector.getWarehouse().getId().equals(warehouseId)) {
            throw new RuntimeException("Sector does not belong to the specified warehouse");
        }

        return WarehouseResponseDTO.SectorDTO.builder()
                .id(sector.getId())
                .name(sector.getName())
                .minTemperature(sector.getMinTemperature())
                .maxTemperature(sector.getMaxTemperature())
                .currentTemperature(sector.getCurrentTemperature())
                .lastTemperatureUpdate(sector.getLastTemperatureUpdate())
                .capacity(sector.getCapacity())
                .build();
    }

    public WarehouseAvailabilityStatisticsDTO getAvailabilityStatistics(Long warehouseId, Instant startTime, Instant endTime) {
        Warehouse warehouse = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new RuntimeException("Warehouse not found"));

        return telemetryService.getAggregatedAvailability(warehouseId, warehouse.getName(), startTime, endTime);
    }
}
