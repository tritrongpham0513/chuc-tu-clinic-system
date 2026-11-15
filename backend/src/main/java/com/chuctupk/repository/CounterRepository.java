package com.chuctupk.repository;

import com.chuctupk.entity.Counter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CounterRepository extends JpaRepository<Counter, String> {

    @Query(value = "SELECT * FROM counters WHERE name = :name FOR UPDATE", nativeQuery = true)
    Optional<Counter> findByNameForUpdate(@Param("name") String name);
}

