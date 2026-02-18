package com.example.demo.repository;

import com.example.demo.model.Note;
import com.example.demo.model.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface NoteRepository extends MongoRepository<Note, String> {
    Page<Note> findByTagsContaining(Tag tag, Pageable pageable);
}
