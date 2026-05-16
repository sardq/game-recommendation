package com.diplome.game_recommendation.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class UserSignupDto {
    @NotBlank
    @Size(min = 4, max = 50)
    private String username;
    @NotBlank
    @Size(min = 5, max = 100)
    private String email;
    @NotBlank
    @Size(min = 3, max = 30)
    private String password;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
