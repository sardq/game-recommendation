package com.diplome.game_recommendation.helpers.configuration;

import com.diplome.game_recommendation.dtos.UserDto;
import com.diplome.game_recommendation.dtos.UserSignupDto;
import com.diplome.game_recommendation.models.UserEntity;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "token", ignore = true)
    UserDto toUserDto(UserEntity user);

    UserEntity signUpToUser(UserSignupDto userEntity);

}
