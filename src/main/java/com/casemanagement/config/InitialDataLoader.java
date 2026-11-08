package com.casemanagement.config;

import com.casemanagement.entity.User;
import com.casemanagement.enums.UserRole;
import com.casemanagement.repository.UserRepository;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class InitialDataLoader implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JsonMapper jsonMapper;

    @Override
    public void run(ApplicationArguments args) {

        try {
            ClassPathResource resource = new ClassPathResource("data/users.json");

            UserSeedData[] users = jsonMapper.readValue(resource.getInputStream(), UserSeedData[].class);

            int insertedCount = 0;

            for (UserSeedData seedUser : users) {

                if (userRepository.existsByEmail(seedUser.email())) {
                    continue;
                }

                User user = new User(
                        seedUser.email(),
                        passwordEncoder.encode(seedUser.password()),
                        UserRole.valueOf(seedUser.role().toUpperCase())
                );

                userRepository.save(user);
                insertedCount++;
            }

            System.out.println("Initial user loading completed. Users inserted: " + insertedCount);

        } catch (IOException exception) {
            throw new IllegalStateException("Failed to load initial users from data/users.json", exception);
        }
    }

    private record UserSeedData(
            String email,
            String password,
            String role
    ) {
    }


}

