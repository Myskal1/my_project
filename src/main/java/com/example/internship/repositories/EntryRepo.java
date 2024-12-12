package com.example.internship.repositories;

import com.example.internship.entities.Entry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EntryRepo extends JpaRepository<Entry, Long> {
    List<Entry> findByUserId(Long userId);
    Optional<Entry> findByIdAndUserId(Long id, Long userId);
}
