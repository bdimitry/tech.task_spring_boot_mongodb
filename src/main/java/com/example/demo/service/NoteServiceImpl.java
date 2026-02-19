package com.example.demo.service;

import com.example.demo.dto.NoteRequest;
import com.example.demo.dto.NoteResponse;
import com.example.demo.dto.NoteStatsResponse;
import com.example.demo.dto.NotesPageResponse;
import com.example.demo.exception.NotFoundException;
import com.example.demo.model.Note;
import com.example.demo.model.Tag;
import com.example.demo.repository.NoteRepository;
import com.example.demo.util.WordStatsCalculator;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

@Service
public class NoteServiceImpl implements NoteService {

    private final NoteRepository repo;
    private final WordStatsCalculator statsCalculator;

    public NoteServiceImpl(NoteRepository repo, WordStatsCalculator statsCalculator) {
        this.repo = repo;
        this.statsCalculator = statsCalculator;
    }

    @Override
    public NoteResponse create(NoteRequest req) {
        Note note = new Note();
        note.setUserId(req.userId());
        note.setTitle(req.title());
        note.setText(req.text());
        note.setTags(req.tags() == null ? Set.of() : req.tags());
        note.setCreatedDate(Instant.now());

        Note saved = repo.save(note);
        return toFullResponse(saved);
    }

    @Override
    public NoteResponse update(String id, String userId, NoteRequest req) {
        Note note = repo.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new NotFoundException("Note not found"));

        note.setTitle(req.title());
        note.setText(req.text());
        note.setTags(req.tags() == null ? Set.of() : req.tags());

        Note saved = repo.save(note);
        return toFullResponse(saved);
    }

    @Override
    public void delete(String id, String userId) {
        if (!repo.existsByIdAndUserId(id, userId)) {
            throw new NotFoundException("Note not found");
        }
        repo.deleteByIdAndUserId(id, userId);
    }

    @Override
    public NotesPageResponse list(String userId, int page, int size, Tag tag) {
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdDate"));

        var p = (tag == null)
                ? repo.findAllByUserId(userId, pageable)
                : repo.findAllByUserIdAndTagsContaining(userId, tag, pageable);

        var items = p.getContent().stream()
                .map(this::toListItemResponse)
                .toList();

        return new NotesPageResponse(items, page, size, p.getTotalElements(), p.getTotalPages());
    }

    @Override
    public NoteResponse getText(String id, String userId) {
        Note note = repo.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new NotFoundException("Note not found"));

        // NoteResponse(id, title, userId, createdDate, text, tags)
        return new NoteResponse(
                note.getId(),
                null,
                note.getUserId(),
                null,
                note.getText(),
                null
        );
    }

    @Override
    public NoteStatsResponse getStats(String id, String userId) {
        Note note = repo.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new NotFoundException("Note not found"));

        Map<String, Integer> stats = statsCalculator.countWords(note.getText());
        return new NoteStatsResponse(stats);
    }

    private NoteResponse toFullResponse(Note n) {
        // NoteResponse(id, title, userId, createdDate, text, tags)
        return new NoteResponse(
                n.getId(),
                n.getTitle(),
                n.getUserId(),
                n.getCreatedDate(),
                n.getText(),
                n.getTags()
        );
    }

    private NoteResponse toListItemResponse(Note n) {
        // list должен быть без text
        return new NoteResponse(
                n.getId(),
                n.getTitle(),
                n.getUserId(),
                n.getCreatedDate(),
                null,
                n.getTags()
        );
    }
}
