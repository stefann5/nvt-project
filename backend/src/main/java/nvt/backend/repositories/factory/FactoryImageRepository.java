package nvt.backend.repositories.factory;

import nvt.backend.model.factory.FactoryImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FactoryImageRepository extends JpaRepository<FactoryImage, Long> {
    List<FactoryImage> findByFactoryId(Long factoryId);
}
