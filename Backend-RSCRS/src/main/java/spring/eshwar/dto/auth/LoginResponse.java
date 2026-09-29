package spring.eshwar.dto.auth;

import spring.eshwar.entity.Role;

public class LoginResponse {

    private String token;
    private String type = "Bearer";
    private Long userId;
    private String name;
    private String email;
    private Role role;
    private String message;
    private boolean success;

    public LoginResponse() {
    }

    public LoginResponse(String token, Long userId, String name, String email, Role role, String message, boolean success) {
        this.token = token;
        this.type = "Bearer";
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.role = role;
        this.message = message;
        this.success = success;
    }

    public static LoginResponse success(String token, Long userId, String name, String email, Role role, String message) {
        return new LoginResponse(token, userId, name, email, role, message, true);
    }

    public static LoginResponse failure(String message) {
        LoginResponse resp = new LoginResponse();
        resp.setSuccess(false);
        resp.setMessage(message);
        return resp;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }
}
