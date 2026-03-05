package com.diplome.game_recommendation.models;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
@Entity
@Table(name = "users")
public class UserEntity extends BaseEntity{
    @Column(nullable = false)
    @Size(min = 4, max = 50)
    private String username;
    @Column(nullable = false, unique = true)
    @Size(min = 5, max = 30)
    @Email
    private String email;
    @Column(nullable = false)
    private String passwordHash;
    @Column(nullable = false)
    private LocalDate registrationDate;
    private LocalDate lastLogin;
    @Column(nullable = false)
    private Integer age;
    public UserEntity() {
    }
    public UserEntity(String username, String email, String password, LocalDate registrationDate, LocalDate lastLogin, Integer age)
    {
        this.username = username;
        this.email = email;
        this.passwordHash = password;
        this.registrationDate = registrationDate;
        this.lastLogin = lastLogin;
        this.age = age;
    }
    public String getUsername() {
    return username;
}

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public LocalDate getRegistrationDate() {
        return registrationDate;
    }

    public void setRegistrationDate(LocalDate registrationDate) {
        this.registrationDate = registrationDate;
    }

    public LocalDate getLastLogin() {
        return lastLogin;
    }

    public void setLastLogin(LocalDate lastLogin) {
        this.lastLogin = lastLogin;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }
}
