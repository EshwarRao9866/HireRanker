package spring.eshwar.dto;

import spring.eshwar.entity.Role;

public class AuthResponse {

    private boolean success;
    private String message;
    private Long id;
    private String name;
    private String email;
    private Role role;
    private String token;

    public AuthResponse() {
    }

    public static AuthResponse success(Long id, String name, String email, Role role, String message) {
        AuthResponse resp = new AuthResponse();
        resp.setSuccess(true);
        resp.setId(id);
        resp.setName(name);
        resp.setEmail(email);
        resp.setRole(role);
        resp.setMessage(message);
        return resp;
    }

    public static AuthResponse failure(String message) {
        AuthResponse resp = new AuthResponse();
        resp.setSuccess(false);
        resp.setMessage(message);
        return resp;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
