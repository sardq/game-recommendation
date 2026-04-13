package com.diplome.game_recommendation.services.librec;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import org.springframework.stereotype.Service;

import com.diplome.game_recommendation.models.UserGames;
import com.diplome.game_recommendation.repositories.UserGameRepository;

@Service
public class LibrecDataBuilder {

    private final UserGameRepository userGameRepository;

    public LibrecDataBuilder(UserGameRepository userGameRepository) {
        this.userGameRepository = userGameRepository;
    }

    public File buildFile() throws IOException {
        List<UserGames> all = userGameRepository.findAll();

        File file = File.createTempFile("librec-data", ".txt");

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            for (UserGames ug : all) {
                if (ug.getRating() != null) {
                    writer.write(
                        ug.getUser().getId() + " " +
                        ug.getGame().getId() + " " +
                        ug.getRating()
                    );
                    writer.newLine();
                }
            }
        }

        return file;
    }
}