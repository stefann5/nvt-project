package nvt.backend.model.user;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@DiscriminatorValue("Manager")
@Data
@EqualsAndHashCode(callSuper = true)
public class Manager extends User {

    @Column(columnDefinition = "boolean default true")
    private Boolean mustChangePassword = true;

    @Column(columnDefinition = "boolean default false")
    private Boolean blocked = false;

    public boolean isMustChangePassword() {
        return mustChangePassword != null && mustChangePassword;
    }

    public boolean isBlocked() {
        return blocked != null && blocked;
    }
}
