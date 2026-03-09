// package com.diplome.game_recommendation.controllers;


// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;
// import org.springframework.data.domain.Page;
// import org.springframework.http.ResponseEntity;
// import org.springframework.security.core.Authentication;
// import org.springframework.security.core.context.SecurityContextHolder;
// import org.springframework.web.bind.annotation.*;

// import jakarta.validation.Valid;

// import java.util.List;
// import java.util.Map;

// @RestController
// @RequestMapping
// public class UserController {
//     //public static final String URL = Constants.API_URL + "/users";
//     //private final UserAuthenticationProvider authProvider;
//     private static final Logger logger = LoggerFactory.getLogger(UserController.class);

//     // private final UserService userService;
//     // private final ModelMapper modelMapper;

//     // public UserController(UserService userService, ModelMapper modelMapper, UserAuthenticationProvider authProvider) {
//     //     this.userService = userService;
//     //     this.modelMapper = modelMapper;
//     //     this.authProvider = authProvider;
//     // }

//     // private UserDto toDto(UserEntity entity) {
//     //     return modelMapper.map(entity, UserDto.class);
//     // }

//     // private UserEntity toEntity(UserDto dto) {
//     //     return modelMapper.map(dto, UserEntity.class);
//     // }

//     @GetMapping
//     public List<UserDto> getAll(
//             @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "5") int pageSize,) {
//         logger.info("Запрос на получение всех пользователей: page={}", page, pageSize);

//         Page<UserEntity> result = userService.getAll(page, 100);
//         return result.getContent()
//                 .stream()
//                 .map(this::toDto)
//                 .toList();
//     }

//     @GetMapping("/filter")
//     public Page<UserDto> getAllByFilter(
//             @RequestParam(name = "search", defaultValue = "") String search,
//             @RequestParam(name = "role", defaultValue = "") String role,
//             @RequestParam(defaultValue = "0") int page,
//             @RequestParam(defaultValue = "5") int pageSize) {
//         logger.info("Фильтрация пользователей выполнена, page={}, pageSize={}", page, pageSize);

//         Page<UserEntity> result = userService.getAllByFilters(search, role, page, pageSize);
//         return result.map(this::toDto);
//     }

//     @GetMapping("/{id}")
//     public UserDto getById(@PathVariable Long id) {
//         logger.info("Запрос на получение пользователя: {}", id);
//         UserEntity user = userService.get(id);
//         return toDto(user);
//     }

//     @PostMapping("/create")
//     public UserDto create(@RequestBody @Valid UserDto dto) {
//         logger.info("Запрос на создание пользователя: {}", dto);
//         UserEntity entity = toEntity(dto);
//         entity.setPassword("");
//         return toDto(userService.create(entity));
//     }

//     @PostMapping("/update/{id}")
//     public UserDto update(@PathVariable Long id, @RequestBody @Valid UserDto dto) {
//         logger.info("Запрос на обновление пользователя {}: {}", id, dto);
//         return toDto(userService.update(id, toEntity(dto)));
//     }

//     @PostMapping("/delete/{id}")
//     public UserDto delete(@PathVariable Long id) {
//         logger.info("Запрос на удаление пользователя {}", id);
//         return toDto(userService.delete(id));
//     }

//     @GetMapping("/me")
//     public ResponseEntity<UserDto> getCurrentUser() {
//         Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

//         if (authentication == null || !authentication.isAuthenticated()) {
//             return ResponseEntity.status(401).build();
//         }

//         Object principal = authentication.getPrincipal();

//         if (principal instanceof UserEntity user) {
//             UserDto dto = new UserDto();
//             dto.setId(user.getId());
//             dto.setEmail(user.getEmail());
//             dto.setLogin(user.getLogin());
//             dto.setFirstName(user.getFirstName());
//             dto.setLastName(user.getLastName());
//             dto.setRole(user.getRole().name());

//             return ResponseEntity.ok(dto);
//         }

//         return ResponseEntity.status(401).build();
//     }
// }