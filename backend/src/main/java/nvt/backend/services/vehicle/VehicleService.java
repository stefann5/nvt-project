package nvt.backend.services.vehicle;

import lombok.RequiredArgsConstructor;
import nvt.backend.dto.vehicle.AvailabilityStatisticsDTO;
import nvt.backend.dto.vehicle.CreateVehicleDTO;
import nvt.backend.dto.vehicle.DistanceStatisticsDTO;
import nvt.backend.dto.vehicle.UpdateVehicleDTO;
import nvt.backend.dto.vehicle.VehicleLocationDTO;
import nvt.backend.dto.vehicle.VehicleResponseDTO;
import nvt.backend.model.vehicle.Vehicle;
import nvt.backend.model.vehicle.VehicleBrand;
import nvt.backend.model.vehicle.VehicleImage;
import nvt.backend.model.vehicle.VehicleLocation;
import nvt.backend.model.vehicle.VehicleModel;
import nvt.backend.repositories.vehicle.VehicleBrandRepository;
import nvt.backend.repositories.vehicle.VehicleLocationRepository;
import nvt.backend.repositories.vehicle.VehicleModelRepository;
import nvt.backend.repositories.vehicle.VehicleRepository;
import nvt.backend.services.storage.MinioService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VehicleService {

    private final VehicleRepository vehicleRepository;
    private final VehicleBrandRepository brandRepository;
    private final VehicleModelRepository modelRepository;
    private final VehicleLocationRepository locationRepository;
    private final VehicleTelemetryService telemetryService;
    private final MinioService minioService;

    @Value("${minio.bucket.vehicle-images}")
    private String vehicleImagesBucket;

    @Transactional
    public VehicleResponseDTO create(CreateVehicleDTO dto, List<MultipartFile> images) throws IOException {
        if (vehicleRepository.existsByLicensePlate(dto.getLicensePlate())) {
            throw new RuntimeException("Vehicle with this license plate already exists");
        }

        VehicleBrand brand = brandRepository.findById(dto.getBrandId())
                .orElseThrow(() -> new RuntimeException("Brand not found"));

        VehicleModel model = modelRepository.findById(dto.getModelId())
                .orElseThrow(() -> new RuntimeException("Model not found"));

        if (!model.getBrand().getId().equals(brand.getId())) {
            throw new RuntimeException("Model does not belong to the selected brand");
        }

        Vehicle vehicle = new Vehicle();
        vehicle.setLicensePlate(dto.getLicensePlate());
        vehicle.setWeightLimit(dto.getWeightLimit());
        vehicle.setBrand(brand);
        vehicle.setModel(model);

        vehicle = vehicleRepository.save(vehicle);

        minioService.createBucketIfNotExists(vehicleImagesBucket);

        for (MultipartFile file : images) {
            String minioPath = minioService.uploadFile(file, vehicleImagesBucket, "vehicle-" + vehicle.getId());

            VehicleImage img = new VehicleImage();
            img.setOriginalName(file.getOriginalFilename());
            img.setMinioPath(minioPath);
            img.setMinioBucket(vehicleImagesBucket);
            img.setVehicle(vehicle);
            vehicle.getImages().add(img);
        }

        vehicle = vehicleRepository.save(vehicle);

        return VehicleResponseDTO.fromEntity(vehicle);
    }

    public List<VehicleResponseDTO> getAll() {
        return vehicleRepository.findAllWithDetails().stream()
                .map(VehicleResponseDTO::fromEntity)
                .toList();
    }

    public VehicleResponseDTO getById(Long id) {
        Vehicle vehicle = vehicleRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new RuntimeException("Vehicle not found"));
        return VehicleResponseDTO.fromEntity(vehicle);
    }

    public List<VehicleResponseDTO> search(String query) {
        return vehicleRepository.searchVehicles(query).stream()
                .map(VehicleResponseDTO::fromEntity)
                .toList();
    }

    @Transactional
    public VehicleResponseDTO update(Long id, UpdateVehicleDTO dto, List<MultipartFile> newImages) throws IOException {
        Vehicle vehicle = vehicleRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new RuntimeException("Vehicle not found"));

        if (dto.getLicensePlate() != null && !dto.getLicensePlate().equals(vehicle.getLicensePlate())) {
            if (vehicleRepository.existsByLicensePlateAndIdNot(dto.getLicensePlate(), id)) {
                throw new RuntimeException("Vehicle with this license plate already exists");
            }
            vehicle.setLicensePlate(dto.getLicensePlate());
        }

        if (dto.getWeightLimit() != null) {
            vehicle.setWeightLimit(dto.getWeightLimit());
        }

        if (dto.getBrandId() != null) {
            VehicleBrand brand = brandRepository.findById(dto.getBrandId())
                    .orElseThrow(() -> new RuntimeException("Brand not found"));
            vehicle.setBrand(brand);
        }

        if (dto.getModelId() != null) {
            VehicleModel model = modelRepository.findById(dto.getModelId())
                    .orElseThrow(() -> new RuntimeException("Model not found"));

            if (!model.getBrand().getId().equals(vehicle.getBrand().getId())) {
                throw new RuntimeException("Model does not belong to the selected brand");
            }
            vehicle.setModel(model);
        }

        if (dto.getImagesToDelete() != null && !dto.getImagesToDelete().isEmpty()) {
            List<VehicleImage> imagesToRemove = vehicle.getImages().stream()
                    .filter(img -> dto.getImagesToDelete().contains(img.getId()))
                    .toList();

            for (VehicleImage img : imagesToRemove) {
                minioService.deleteFile(img.getMinioBucket(), img.getMinioPath());
                vehicle.getImages().remove(img);
            }
        }

        if (newImages != null && !newImages.isEmpty()) {
            for (MultipartFile file : newImages) {
                String minioPath = minioService.uploadFile(file, vehicleImagesBucket, "vehicle-" + vehicle.getId());

                VehicleImage img = new VehicleImage();
                img.setOriginalName(file.getOriginalFilename());
                img.setMinioPath(minioPath);
                img.setMinioBucket(vehicleImagesBucket);
                img.setVehicle(vehicle);
                vehicle.getImages().add(img);
            }
        }

        vehicle.setUpdatedAt(LocalDateTime.now());
        vehicle = vehicleRepository.save(vehicle);

        return VehicleResponseDTO.fromEntity(vehicle);
    }

    @Transactional
    public void delete(Long id) {
        Vehicle vehicle = vehicleRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new RuntimeException("Vehicle not found"));

        for (VehicleImage img : vehicle.getImages()) {
            minioService.deleteFile(img.getMinioBucket(), img.getMinioPath());
        }

        vehicleRepository.delete(vehicle);
    }

    public List<VehicleBrand> getAllBrands() {
        return brandRepository.findAll();
    }

    public List<VehicleModel> getModelsByBrand(Long brandId) {
        return modelRepository.findByBrandId(brandId);
    }

    public VehicleLocationDTO getLastLocation(Long vehicleId) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new RuntimeException("Vehicle not found"));

        return locationRepository.findByVehicleId(vehicleId)
                .map(VehicleLocationDTO::fromEntity)
                .orElse(null);
    }

    public DistanceStatisticsDTO getDistanceStatistics(Long vehicleId, LocalDate startDate, LocalDate endDate) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new RuntimeException("Vehicle not found"));

        return telemetryService.getAggregatedDistance(vehicleId, vehicle.getLicensePlate(), startDate, endDate);
    }

    public AvailabilityStatisticsDTO getAvailabilityStatistics(Long vehicleId, Instant startTime, Instant endTime) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new RuntimeException("Vehicle not found"));

        return telemetryService.getAggregatedAvailability(vehicleId, vehicle.getLicensePlate(), startTime, endTime);
    }
}