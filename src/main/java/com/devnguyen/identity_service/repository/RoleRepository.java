package com.devnguyen.identity_service.repository;

import com.devnguyen.identity_service.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/*
 * JpaRepository<Role, String>:
 * PK của Role là String (name), nên generic thứ 2 là String
 * Inherited methods: findById(String name), save(role), deleteById(String name)...
 */
@Repository
public interface RoleRepository extends JpaRepository<Role, String> {
}