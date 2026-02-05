package nvt.backend.dto.user;

public class ManagerResponseDTO {

    private int id;
    private String username;
    private String name;
    private String surname;
    private String phoneNumber;
    private String photo;
    private boolean active;
    private boolean blocked;
    private String role;

    public ManagerResponseDTO() {}

    public ManagerResponseDTO(int id, String username, String name, String surname, 
                               String phoneNumber, String photo, boolean active, 
                               boolean blocked, String role) {
        this.id = id;
        this.username = username;
        this.name = name;
        this.surname = surname;
        this.phoneNumber = phoneNumber;
        this.photo = photo;
        this.active = active;
        this.blocked = blocked;
        this.role = role;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSurname() {
        return surname;
    }

    public void setSurname(String surname) {
        this.surname = surname;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getPhoto() {
        return photo;
    }

    public void setPhoto(String photo) {
        this.photo = photo;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isBlocked() {
        return blocked;
    }

    public void setBlocked(boolean blocked) {
        this.blocked = blocked;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
