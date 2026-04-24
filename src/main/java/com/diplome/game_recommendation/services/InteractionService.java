package com.diplome.game_recommendation.services;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import com.diplome.game_recommendation.dtos.GameDto;
import com.diplome.game_recommendation.dtos.ReviewDto;
import com.diplome.game_recommendation.models.GameEntity;
import com.diplome.game_recommendation.models.InteractionEnum;
import com.diplome.game_recommendation.models.UserEntity;
import com.diplome.game_recommendation.models.UserGames;
import com.diplome.game_recommendation.repositories.GameRepository;
import com.diplome.game_recommendation.repositories.UserGameRepository;
import com.diplome.game_recommendation.repositories.UserRepository;

import jakarta.transaction.Transactional;

@Service
public class InteractionService {

    private final UserRepository userRepository;
    private final GameRepository gameRepository;
    private final UserGameRepository userGamesRepository;
    private final GameService gameService;
    public InteractionService(
            UserRepository userRepository,
            GameRepository gameRepository,
            UserGameRepository userGamesRepository,
            GameService gameService) {
        this.userRepository = userRepository;
        this.gameRepository = gameRepository;
        this.userGamesRepository = userGamesRepository;
        this.gameService = gameService;
    }
    @Transactional
    public void recordView(Authentication authentication, Long gameId) {
        UserEntity user = userRepository.findByEmail(authentication.getName()).orElseThrow();
        GameEntity game = gameRepository.findById(gameId).orElseThrow();
        UserGames interaction = new UserGames();
        interaction.setUser(user);
        interaction.setGame(game);
        interaction.setInteraction(InteractionEnum.Viewed);
        interaction.setTime(LocalDateTime.now());

        userGamesRepository.save(interaction);
    }
    @Transactional
    public ReviewDto getUserReview(Long gameId, Authentication authentication){
        UserEntity user = userRepository.findByEmail(authentication.getName()).orElseThrow();

        UserGames review= userGamesRepository.findByUserIdAndGameIdAndInteraction(user.getId(), gameId, InteractionEnum.Review).orElse(null);
        UserGames rate= userGamesRepository.findByUserIdAndGameIdAndInteraction(user.getId(), gameId, InteractionEnum.Rated).orElse(null);
        ReviewDto dto = new ReviewDto();
        dto.setRating(rate != null ? rate.getRating() : null);
        dto.setReview(review != null ? review.getReview() : null);
        return dto;
    }
    @Transactional
    public void recordRating(Authentication authentication, Long gameId, Integer rating) {

        UserEntity user = userRepository.findByEmail(authentication.getName()).orElseThrow();
        GameEntity game = gameRepository.findById(gameId).orElseThrow();

        UserGames interaction = userGamesRepository.findByUserIdAndGameIdAndInteraction(user.getId(), gameId, InteractionEnum.Rated).orElse(new UserGames());

        interaction.setUser(user);
        interaction.setGame(game);
        interaction.setInteraction(InteractionEnum.Rated);
        interaction.setRating(rating);
        interaction.setTime(LocalDateTime.now());
        gameService.updateLocalRating(gameId);
        userGamesRepository.save(interaction);
    }
    @Transactional
    public void addToFavorites(Authentication authentication, Long gameId) {
        UserEntity user = userRepository.findByEmail(authentication.getName()).orElseThrow();
        GameEntity game = gameRepository.findById(gameId).orElseThrow();
        UserGames interaction = new UserGames();
        interaction.setUser(user);
        interaction.setGame(game);
        interaction.setInteraction(InteractionEnum.Favorite);
        interaction.setTime(LocalDateTime.now());
        userGamesRepository.save(interaction);
    }
    @Transactional
    public void removeFromFavorites(Authentication authentication, Long gameId) {
        UserEntity user = userRepository.findByEmail(authentication.getName()).orElseThrow();
        GameEntity game = gameRepository.findById(gameId).orElseThrow();
        var interaction = userGamesRepository
                .findByUserIdAndGameIdAndInteraction(user.getId(), game.getId(), InteractionEnum.Favorite);

        interaction.ifPresent(userGamesRepository::delete);
    }
    @Transactional
    public boolean isFavorite(Authentication authentication, Long gameId){

        UserEntity user = userRepository.findByEmail(authentication.getName()).orElseThrow();
        GameEntity game = gameRepository.findById(gameId).orElseThrow();

        return userGamesRepository
                .findByUserIdAndGameIdAndInteraction(user.getId(), game.getId(), InteractionEnum.Favorite)
                .isPresent();
    }
    @Transactional
    public List<ReviewDto> getReviewsByGame(Long gameId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        List<UserGames> ratedGames = userGamesRepository
            .findByGameIdAndRatingIsNotNullOrderByTimeDesc(gameId, pageable).getContent();

        List<UserGames> reviewedGames = userGamesRepository
                .findByGameIdAndReviewIsNotNullOrderByTimeDesc(gameId, pageable).getContent();

        Map<Long, UserGames> ratingMap = ratedGames.stream()
            .collect(Collectors.toMap(
                g -> g.getUser().getId(),
                g -> g
            ));

        List<ReviewDto> reviewDtos = reviewedGames.stream()
            .map(review -> {
                UserGames rating = ratingMap.get(review.getUser().getId());
                Integer userRating = rating != null ? rating.getRating() : null;

                ReviewDto dto = new ReviewDto();
                dto.setId(review.getUser().getId());
                dto.setLogin(review.getUser().getUsername());
                dto.setReview(review.getReview());
                dto.setRating(userRating);
                return dto;
            })
            .toList();

        List<ReviewDto> ratingOnly = ratedGames.stream()
            .filter(r -> reviewedGames.stream().noneMatch(rev -> rev.getUser().getId().equals(r.getUser().getId())))
            .map(r -> {
                ReviewDto dto = new ReviewDto();
                dto.setId(r.getUser().getId());
                dto.setLogin(r.getUser().getUsername());
                dto.setReview(null);
                dto.setRating(r.getRating());
                return dto;
            })
            .toList();
        List<ReviewDto> reviews = new ArrayList<>(reviewDtos);
        reviews.addAll(ratingOnly);
        return reviews;
    }
    public void addReview(Authentication authentication, Long gameId, String reviewText) {
        UserEntity user = userRepository.findByEmail(authentication.getName()).orElseThrow();
        GameEntity game = gameRepository.findById(gameId).orElseThrow();

        UserGames ug = userGamesRepository
            .findByUserIdAndGameIdAndInteraction(user.getId(), gameId, InteractionEnum.Review)
            .orElse(new UserGames(user, game, InteractionEnum.Review, null, LocalDateTime.now(), reviewText));

        ug.setReview(reviewText);
        ug.setTime(LocalDateTime.now());

        userGamesRepository.save(ug);
    }
    public Optional<UserGames> getUserInteraction(Authentication authentication, Long gameId, String type) {
        Optional<UserEntity> user = userRepository.findByEmail(authentication.getName());
        return userGamesRepository.findByUserIdAndGameIdAndInteraction(user.get().getId(), gameId, InteractionEnum.valueOf(type));
    }
    @Transactional
    public List<GameDto> getUserFavorites(Authentication authentication) {
        UserEntity user = userRepository.findByEmail(authentication.getName())
            .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        Pageable pageable = PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "time"));

            List<UserGames> favorites = userGamesRepository
                    .findByUserIdAndInteraction(user.getId(), InteractionEnum.Favorite, pageable)
                    .getContent();

            return favorites.stream()
                    .map(fav -> {
                        GameEntity game = fav.getGame();
                        GameDto dto = new GameDto();
                        dto.setId(game.getId());
                        dto.setName(game.getName());
                        dto.setPosterUrl(game.getPosterUrl());
                        return dto;
                    })
                    .collect(Collectors.toList());
        }
    
    @Transactional
    public List<ReviewDto> getUserReviews(Authentication authentication, int page, int size) {
        UserEntity user = userRepository.findByEmail(authentication.getName()).orElseThrow();

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "time"));

        List<UserGames> reviewEntities = userGamesRepository
                .findByUserIdAndInteraction(user.getId(), InteractionEnum.Review, pageable)
                .getContent();

        List<UserGames> ratingEntities = userGamesRepository
                .findByUserIdAndInteraction(user.getId(), InteractionEnum.Rated)
                .stream()
                .toList();

        Map<Long, Integer> ratingMap = ratingEntities.stream()
                .collect(Collectors.toMap(
                    r -> r.getGame().getId(),
                    UserGames::getRating
                ));

        return reviewEntities.stream()
                .map(review -> {
                    ReviewDto dto = new ReviewDto();
                    dto.setId(review.getGame().getId());
                    dto.setLogin(user.getUsername());
                    dto.setGameTitile(review.getGame().getName());
                    dto.setReview(review.getReview());
                    dto.setRating(ratingMap.get(review.getGame().getId())); 
                    return dto;
                })
                .collect(Collectors.toList());
    }

}