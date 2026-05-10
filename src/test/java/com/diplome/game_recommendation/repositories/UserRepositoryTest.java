package com.diplome.game_recommendation.repositories;

import com.diplome.game_recommendation.integration.GigachatService;
import com.diplome.game_recommendation.models.UserEntity;
import com.diplome.game_recommendation.services.FileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
    "SPRING_MAIL_USERNAME=test@gmail.com",
    "SPRING_MAIL_PASSWORD=test",
    "GIGACHAT_KEY=test_key",
    "RAWG_API_KEY=test_key",
    "VIDEO_API_KEY=test_key",
    "NEWS_API_KEY=test_key",
    "spring.quartz.job-store-type=memory",
    "DB_USERNAME=postgres",
    "DB_PASSWORD=postgres",
    "spring.main.allow-bean-definition-overriding=true"
})
@ActiveProfiles("dev")
@Transactional
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @MockitoBean
    private GigachatService gigachatService;

    @MockitoBean
    private FileService fileService;

    private UserEntity testUser;
    private String uniqueEmail;
    private String uniqueLogin;

    @BeforeEach
    void setUp() {
        String uniqueSuffix = UUID.randomUUID().toString().substring(0, 8);
        uniqueEmail = uniqueSuffix + "@test.com"; // Длина < 30 символов
        uniqueLogin = "User_" + uniqueSuffix;

        testUser = new UserEntity();
        testUser.setUsername(uniqueLogin);
        testUser.setEmail(uniqueEmail);
        testUser.setPasswordHash("encoded_password");
        testUser.setRegistrationDate(LocalDate.now());
        
        testUser = userRepository.save(testUser);
    }

    @Test
    void findByEmail_ShouldReturnUserWhenEmailMatchesExactly() {
        Optional<UserEntity> found = userRepository.findByEmail(uniqueEmail);
        
        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo(uniqueEmail);
    }

    @Test
    void findByEmailIgnoreCase_ShouldReturnUserEvenWithDifferentCase() {
        // Проверяем поиск в верхнем регистре
        String upperEmail = uniqueEmail.toUpperCase();
        
        Optional<UserEntity> found = userRepository.findByEmailIgnoreCase(upperEmail);
        
        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo(uniqueEmail);
    }

    @Test
    void findByUsername_ShouldReturnUserWhenUsernameMatchesExactly() {
        Optional<UserEntity> found = userRepository.findByUsername(uniqueLogin);
        
        assertThat(found).isPresent();
        assertThat(found.get().getUsername()).isEqualTo(uniqueLogin);
    }

    @Test
    void findByUsernameIgnoreCase_ShouldReturnUserEvenWithDifferentCase() {
        // Проверяем поиск логина в нижнем регистре
        String lowerLogin = uniqueLogin.toLowerCase();
        
        Optional<UserEntity> found = userRepository.findByUsernameIgnoreCase(lowerLogin);
        
        assertThat(found).isPresent();
        assertThat(found.get().getUsername()).isEqualTo(uniqueLogin);
    }

    @Test
    void existsByEmail_ShouldReturnTrueForExistingEmail() {
        boolean exists = userRepository.existsByEmail(uniqueEmail);
        assertThat(exists).isTrue();
    }

    @Test
    void existsByEmail_ShouldReturnFalseForNonExistentEmail() {
        boolean exists = userRepository.existsByEmail("non_existent@mail.com");
        assertThat(exists).isFalse();
    }

    @Test
    void findByEmail_WithNonExistentEmail_ShouldReturnEmptyOptional() {
        Optional<UserEntity> found = userRepository.findByEmail("unknown@test.com");
        assertThat(found).isEmpty();
    }

    @Test
    void save_ShouldUpdateExistingUser() {
        // Arrange
        testUser.setUsername("UpdatedLogin");
        
        // Act
        UserEntity updated = userRepository.save(testUser);
        userRepository.flush();
        
        // Assert
        Optional<UserEntity> found = userRepository.findById(testUser.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getUsername()).isEqualTo("UpdatedLogin");
    }
}