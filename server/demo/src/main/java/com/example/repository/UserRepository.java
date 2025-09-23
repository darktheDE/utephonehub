package com.example.repository;

import com.example.model.User;
import jakarta.persistence.EntityManagerFactory;

import java.util.List;
import java.util.Optional;

public class UserRepository extends BaseRepository<User, Long> {

    public UserRepository(EntityManagerFactory emf) {
        super(emf, User.class);
    }

    public Optional<User> findByEmail(String email) {
        return execute(em ->
                em.createQuery("SELECT u FROM User u WHERE u.email = :email", User.class)
                        .setParameter("email", email)
                        .getResultStream()
                        .findFirst()
        );
    }

    public boolean existsByEmail(String email) {
        return findByEmail(email).isPresent();
    }

    public Optional<User> findByRefreshToken(String token) {
        return execute(em ->
                em.createQuery("SELECT u FROM User u WHERE u.refreshToken = :token", User.class)
                        .setParameter("token", token)
                        .getResultStream()
                        .findFirst()
        );
    }
}