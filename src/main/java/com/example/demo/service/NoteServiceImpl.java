package com.example.demo.service.impl;

import com.example.demo.dto.*;
import com.example.demo.exception.NotFoundException;
import com.example.demo.model.Note;
import com.example.demo.model.Tag;
import com.example.demo.repository.NoteRepository;
import com.example.demo.service.NoteService;
import com.example.demo.util.WordStatsCalculator;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Service
public class NoteServiceImpl implements NoteService {

    private final NoteRepository repo;
    private final WordStatsCalculator statsCalculator = new WordStatsCalculator();

    public NoteServiceImpl(NoteRepository repo) {
        this.repo = repo;
    }

    @Override
    public NoteResponse create(NoteRequest req) {
        Note note = new Note();
        note.setTitle(req.title());
        note.setText(req.text());
        note.setCreatedDate(Instant.now());
        note.setTags(normalizeTags(req.tags()));

        Note saved = repo.save(note);
        return toFullResponse(saved);
    }

    @Override
    public NoteResponse update(String id, NoteRequest req) {
        Note note = repo.findById(id).orElseThrow(() -> new NotFoundException("Note not found: " + id));
        note.setTitle(req.title());
        note.setText(req.text());
        note.setTags(normalizeTags(req.tags()));

        Note saved = repo.save(note);
        return toFullResponse(saved);
    }

    @Override
    public void delete(String id) {
        if (!repo.existsById(id)) {
            throw new NotFoundException("Note not found: " + id);
        }
        repo.deleteById(id);
    }

    @Override
    public NotesPageResponse list(int page, int size, Tag tag) {
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdDate"));

        var resultPage = (tag == null)
                ? repo.findAll(pageable)
                : repo.findByTagsContaining(tag, pageable);

        var items = resultPage.getContent().stream()
                .map(this::toListItemResponse)
                .toList();

        return new NotesPageResponse(
                items,
                page,
                size,
                resultPage.getTotalElements(),
                resultPage.getTotalPages()
        );
    }

    @Override
    public NoteResponse getText(String id) {
        Note note = repo.findById(id).orElseThrow(() -> new NotFoundException("Note not found: " + id));
        return new NoteResponse(note.getId(), null, null, note.getText(), null);
    }

    @Override
    public NoteStatsResponse getStats(String id) {
        Note note = repo.findById(id).orElseThrow(() -> new NotFoundException("Note not found: " + id));
        return new NoteStatsResponse(statsCalculator.countWords(note.getText()));
    }

    // -------- mapping helpers --------

    private NoteResponse toFullResponse(Note note) {
        return new NoteResponse(
                note.getId(),
                note.getTitle(),
                note.getCreatedDate(),
                note.getText(),
                note.getTags()
        );
    }

    private NoteResponse toListItemResponse(Note note) {
        return new NoteResponse(
                note.getId(),
                note.getTitle(),
                note.getCreatedDate(),
                null,
                null
        );
    }

    private Set<Tag> normalizeTags(Set<Tag> tags) {
        return tags == null ? new HashSet<>() : tags;
    }
}
