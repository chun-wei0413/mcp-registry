package tw.teddysoft.aiscrum.io.springboot.config.orm;

import org.springframework.data.jpa.repository.JpaRepository;
import tw.teddysoft.aiscrum.scrumteam.usecase.port.out.UserData;

/**
 * Spring Data JPA repository for UserData persistence.
 * Users are from upstream Account BC, stored locally for reference.
 */
public interface UserOrmClient extends JpaRepository<UserData, String> {
    // Spring Data JPA will automatically implement basic CRUD operations
}