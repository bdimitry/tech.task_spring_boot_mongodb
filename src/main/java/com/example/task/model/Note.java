package com.example.task.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Data
@Document("notes")
public class Note {

    @Indexed
    private String userId;

    @Id
    private String id;

    private String title;
    private Instant createdDate;
    private String text;
    private Set<Tag> tags = new HashSet<>();

}