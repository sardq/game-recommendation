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
import com.diplome.game_recommendation.models.ReactionType;
import com.diplome.game_recommendation.models.ReviewReaction;
import com.diplome.game_recommendation.models.UserEntity;
import com.diplome.game_recommendation.models.UserGames;
import com.diplome.game_recommendation.repositories.GameRepository;
import com.diplome.game_recommendation.repositories.ReviewReactionRepository;
import com.diplome.game_recommendation.repositories.UserGameRepository;
import com.diplome.game_recommendation.repositories.UserRepository;

import jakarta.transaction.Transactional;

@Service
public class InteractionService {

    private final UserRepository userRepository;
    private final GameRepository gameRepository;
    private final UserGameRepository userGamesRepository;
    private final ReviewReactionRepository reactionRepository;
    private final GameService gameService;
    public InteractionService(
            UserRepository userRepository,
            GameRepository gameRepository,
            UserGameRepository userGamesRepository,
            ReviewReactionRepository reactionRepository,
            GameService gameService) {
        this.userRepository = userRepository;
        this.gameRepository = gameRepository;
        this.userGamesRepository = userGamesRepository;
        this.reactionRepository = reactionRepository;
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
        userGamesRepository.saveAndFlush(interaction); 
        gameService.updateLocalRating(gameId);
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
    public List<ReviewDto> getReviewsByGame(Long gameId, int page, int size, Authentication authentication) {
        Pageable pageable = PageRequest.of(page, size);
        
        UserEntity currentUser = null;
        if (authentication != null) {
            currentUser = userRepository.findByEmail(authentication.getName()).orElse(null);
        }
        final UserEntity finalUser = currentUser;

        List<UserGames> reviewedGames = userGamesRepository
                .findByGameIdAndReviewIsNotNullOrderByTimeDesc(gameId, pageable).getContent();

        List<UserGames> ratedGames = userGamesRepository
                .findByGameIdAndRatingIsNotNullOrderByTimeDesc(gameId, pageable).getContent();

        Map<Long, UserGames> ratingMap = ratedGames.stream()
                .collect(Collectors.toMap(g -> g.getUser().getId(), g -> g, (existing, replacement) -> existing));

        return reviewedGames.stream()
                .map(review -> {
                    UserGames rating = ratingMap.get(review.getUser().getId());
                    ReviewDto dto = new ReviewDto();
                    
                    dto.setId(review.getId()); 
                    dto.setLogin(review.getUser().getUsername());
                    dto.setReview(review.getReview());
                    dto.setRating(rating != null ? rating.getRating() : null);
                    dto.setAuthorId(review.getUser().getId());
                    List<ReviewReaction> reactions = reactionRepository.findByReviewId(review.getId());
                    
                    dto.setLikesCount(reactions.stream().filter(r -> r.getType() == ReactionType.LIKE).count());
                    dto.setDislikesCount(reactions.stream().filter(r -> r.getType() == ReactionType.DISLIKE).count());
                    dto.setFunnyCount(reactions.stream().filter(r -> r.getType() == ReactionType.FUNNY).count());

                    if (finalUser != null) {
                        reactions.stream()
                            .filter(r -> r.getUser().getId().equals(finalUser.getId()))
                            .findFirst()
                            .ifPresent(r -> dto.setCurrentUserReaction(r.getType().name()));
                    }

                    return dto;
                })
                .collect(Collectors.toList());
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
    @Transactional
    public void reactToReview(Authentication authentication, Long reviewId, String typeString) {
        ReactionType type = ReactionType.valueOf(typeString);
        Long userId = userRepository.findByEmail(authentication.getName()).get().getId();
        Optional<ReviewReaction> existing = reactionRepository.findByUserIdAndReviewId(userId, reviewId);
        
        if (existing.isPresent()) {
            ReviewReaction reaction = existing.get();
            if (reaction.getType() == type) {
                reactionRepository.delete(reaction); 
            } else {
                reaction.setType(type); 
                reactionRepository.save(reaction);
            }
        } else {
            ReviewReaction newReaction = new ReviewReaction();
            newReaction.setUser(userRepository.getReferenceById(userId));
            newReaction.setReview(userGamesRepository.getReferenceById(reviewId));
            newReaction.setType(type);
            reactionRepository.save(newReaction);
        }
    }
}