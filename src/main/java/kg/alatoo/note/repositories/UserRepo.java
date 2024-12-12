package kg.alatoo.note.repositories;

import kg.alatoo.note.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepo extends JpaRepository<User, Long> {
    Optional<User>findByEmail(String email);

    Boolean existsUserByUsername(String username);
    Boolean existsUserByEmail(String email);

}
