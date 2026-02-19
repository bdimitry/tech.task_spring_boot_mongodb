package com.example.demo.controller;

import com.example.demo.dto.NoteRequest;
import com.example.demo.dto.NoteResponse;
import com.example.demo.dto.NoteStatsResponse;
import com.example.demo.dto.NotesPageResponse;
import com.example.demo.model.Tag;
import com.example.demo.service.NoteService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

/**
 * REST controller for notes.
 * <p>
 * Notes are scoped to a user and are addressed as:
 * /v1/users/{userId}/notes.
 */
@RestController
@RequestMapping("/v1/users/{userId}/notes")
public class NoteController {

    private final NoteService service;

    public NoteController(NoteService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<NoteResponse> create(
            @PathVariable("userId") String userId,
            @Valid @RequestBody NoteRequest req
    ) {
        NoteResponse created = service.create(userId, req);
        return ResponseEntity
                .created(URI.create("/v1/users/" + userId + "/notes/" + created.id()))
                .body(created);
    }

    @GetMapping
    public ResponseEntity<NotesPageResponse> list(
            @PathVariable("userId") String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Tag tag
    ) {
        return ResponseEntity.ok(service.list(userId, page, size, tag));
    }

    @GetMapping("/{id}/text")
    public ResponseEntity<NoteResponse> text(
            @PathVariable("userId") String userId,
            @PathVariable("id") String id
    ) {
        return ResponseEntity.ok(service.getText(userId, id));
    }

    @GetMapping("/{id}/stats")
    public ResponseEntity<NoteStatsResponse> stats(
            @PathVariable("userId") String userId,
            @PathVariable("id") String id
    ) {
        return ResponseEntity.ok(service.getStats(userId, id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<NoteResponse> update(
            @PathVariable("userId") String userId,
            @PathVariable("id") String id,
            @Valid @RequestBody NoteRequest req
    ) {
        return ResponseEntity.ok(service.update(userId, id, req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable("userId") String userId,
            @PathVariable("id") String id
    ) {
        service.delete(userId, id);
        return ResponseEntity.noContent().build();
    }
}
