package com.example.demo.repository;

import com.example.demo.model.Note;
import com.example.demo.model.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface NoteRepository extends MongoRepository<Note, String> {
    Page<Note> findAllByUserId(String userId, Pageable pageable);

    Page<Note> findAllByUserIdAndTagsContaining(String userId, Tag tag, Pageable pageable);

    Optional<Note> findByIdAndUserId(String id, String userId);

    boolean existsByIdAndUserId(String id, String userId);

    void deleteByIdAndUserId(String id, String userId);

}
