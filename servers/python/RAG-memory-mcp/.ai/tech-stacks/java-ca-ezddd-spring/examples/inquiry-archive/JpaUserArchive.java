package tw.teddysoft.aiscrum.scrumteam.adapter.out.database.springboot.archive;

import tw.teddysoft.aiscrum.io.springboot.config.orm.UserOrmClient;
import tw.teddysoft.aiscrum.scrumteam.usecase.port.out.UserData;
import tw.teddysoft.aiscrum.scrumteam.usecase.port.out.archive.UserArchive;

import java.util.Objects;
import java.util.Optional;

/**
 * JPA implementation of UserArchive for database operations.
 * Manages User data from upstream Account BC.
 * Note: No @Repository annotation needed - Spring Data JPA manages the bean.
 */
public class JpaUserArchive implements UserArchive {
    
    private final UserOrmClient userOrmClient;
    
    public JpaUserArchive(UserOrmClient userOrmClient) {
        Objects.requireNonNull(userOrmClient, "userOrmClient cannot be null");
        this.userOrmClient = userOrmClient;
    }
    
    @Override
    public Optional<UserData> findById(String userId) {
        Objects.requireNonNull(userId, "userId cannot be null");
        return userOrmClient.findById(userId);
    }

    @Override
    public void save(UserData userData) {
        Objects.requireNonNull(userData, "userData cannot be null");
        userOrmClient.saveAndFlush(userData);
    }
    
    @Override
    public void delete(UserData userData) {
        Objects.requireNonNull(userData, "userData cannot be null");
        userOrmClient.delete(userData);
        userOrmClient.flush();
    }
}