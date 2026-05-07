package com.devnguyen.identity_service.repository;

import com.devnguyen.identity_service.entity.InvalidatedToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Date;

@Repository
public interface InvalidatedTokenRepository extends JpaRepository<InvalidatedToken, String> {

    void deleteByExpiryTimeBefore(Date date);

    long countByExpiryTimeBefore(Date date);
}