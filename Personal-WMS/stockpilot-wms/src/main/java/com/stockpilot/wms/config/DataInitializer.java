package com.stockpilot.wms.config;

import com.stockpilot.wms.domain.AppUser;
import com.stockpilot.wms.domain.Role;
import com.stockpilot.wms.repository.AppUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Creates the starter accounts the first time the app runs.
 * These passwords are for learning only; change them before anyone else can reach the server.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(AppUserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        createIfMissing("admin", "admin123", "Alex Admin", Role.ADMIN);
        createIfMissing("picker", "picker123", "Pat Picker", Role.WAREHOUSE);
        createIfMissing("connector", "connector123", "Web store connector", Role.ADMIN);
    }

    private void createIfMissing(String username, String password, String fullName, Role role) {
        if (!userRepository.existsByUsername(username)) {
            userRepository.save(new AppUser(username, passwordEncoder.encode(password), fullName, role));
            log.info("Created starter user '{}' ({})", username, role);
        }
    }
}
