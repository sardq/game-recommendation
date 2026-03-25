package com.diplome.game_recommendation.models;

import java.time.LocalDate;
import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
@Entity
@Table(name = "users")
@Getter @Setter
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
    private Date birthDate;
    public UserEntity() {
    }
    public UserEntity(String username, String email, String password, LocalDate registrationDate, LocalDate lastLogin, Date birthDate)
    {
        this.username = username;
        this.email = email;
        this.passwordHash = password;
        this.registrationDate = registrationDate;
        this.lastLogin = lastLogin;
        this.birthDate = birthDate;
    }
}
