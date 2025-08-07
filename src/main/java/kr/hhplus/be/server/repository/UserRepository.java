package kr.hhplus.be.server.repository;

import jakarta.persistence.LockModeType;
import kr.hhplus.be.server.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByIdAndStatus(long userId, String status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM User u WHERE u.id = :userId")
    Optional<User> findByIdAndStatusWithLock(@Param("userId") Long userId, String status);

}
