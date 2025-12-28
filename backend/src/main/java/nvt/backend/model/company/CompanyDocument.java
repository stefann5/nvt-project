package nvt.backend.model.company;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "company_documents")
@Data
public class CompanyDocument {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String originalName;
    private String path;
    private String contentType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_id")
    private RegistrationRequest request;
}