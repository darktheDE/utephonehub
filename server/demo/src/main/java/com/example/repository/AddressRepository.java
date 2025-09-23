package com.example.repository;

import com.example.model.Address;
import jakarta.persistence.EntityManagerFactory;

import java.util.List;

public class AddressRepository extends BaseRepository<Address, Long> {

    public AddressRepository(EntityManagerFactory emf) {
        super(emf, Address.class);
    }

    public List<Address> findByUserId(Long userId) {
        return execute(em ->
                em.createQuery("SELECT a FROM Address a WHERE a.user.id = :userId ORDER BY a.isDefault DESC, a.id ASC", Address.class)
                        .setParameter("userId", userId)
                        .getResultList()
        );
    }

    public void setDefaultAddress(Long userId, Long addressId) {
        executeInsideTransaction(em -> {
            // Unset previous default
            em.createQuery("UPDATE Address a SET a.isDefault = false WHERE a.user.id = :userId AND a.isDefault = true")
                    .setParameter("userId", userId)
                    .executeUpdate();

            // Set new default
            em.createQuery("UPDATE Address a SET a.isDefault = true WHERE a.id = :addressId AND a.user.id = :userId")
                    .setParameter("addressId", addressId)
                    .setParameter("userId", userId)
                    .executeUpdate();
            return null; // No return value needed for this operation
        });
    }
}
