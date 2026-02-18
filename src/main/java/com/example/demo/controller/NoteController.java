package com.example.demo.controller;

import com.example.demo.dto.*;
import com.example.demo.model.Tag;
import com.example.demo.service.NoteService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/notes")
public class NoteController {

    private final NoteService service;

    public NoteController(NoteService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<NoteResponse> create(@Valid @RequestBody NoteRequest req) {
        NoteResponse created = service.create(req);
        return ResponseEntity.created(URI.create("/api/notes/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<NoteResponse> update(@PathVariable String id, @Valid @RequestBody NoteRequest req) {
        return ResponseEntity.ok(service.update(id, req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<NotesPageResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Tag tag
    ) {
        return ResponseEntity.ok(service.list(page, size, tag));
    }

    @GetMapping("/{id}/text")
    public ResponseEntity<NoteResponse> text(@PathVariable String id) {
        return ResponseEntity.ok(service.getText(id));
    }

    @GetMapping("/{id}/stats")
    public ResponseEntity<NoteStatsResponse> stats(@PathVariable String id) {
        return ResponseEntity.ok(service.getStats(id));
    }
}
