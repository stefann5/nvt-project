package nvt.backend.dto.user.auth;

public class LoginResponseDTO {
    private String accessToken;
    private String refreshToken;
    private boolean mustChangePassword;

    public LoginResponseDTO(String accessToken, String refreshToken) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.mustChangePassword = false;
    }

    public LoginResponseDTO(String accessToken, String refreshToken, boolean mustChangePassword) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.mustChangePassword = mustChangePassword;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getRefreshToken() {return  refreshToken;}

    public void setRefreshToken(String refreshToken) {this.refreshToken = refreshToken;}

    public boolean isMustChangePassword() {
        return mustChangePassword;
    }

    public void setMustChangePassword(boolean mustChangePassword) {
        this.mustChangePassword = mustChangePassword;
    }
}