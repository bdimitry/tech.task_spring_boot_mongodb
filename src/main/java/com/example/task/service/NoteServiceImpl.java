package com.example.task.service;

import com.example.task.dto.NoteRequest;
import com.example.task.dto.NoteResponse;
import com.example.task.dto.NoteStatsResponse;
import com.example.task.dto.NotesPageResponse;
import com.example.task.exception.NotFoundException;
import com.example.task.model.Note;
import com.example.task.model.Tag;
import com.example.task.repository.NoteRepository;
import com.example.task.util.WordStatsCalculator;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

/**
 * Default implementation of {@link NoteService}.
 * <p>
 * Always query notes by (id + userId) to avoid exposing another user's data.
 */
@Service
public class NoteServiceImpl implements NoteService {

    private final NoteRepository repo;
    private final WordStatsCalculator statsCalculator;

    public NoteServiceImpl(NoteRepository repo, WordStatsCalculator statsCalculator) {
        this.repo = repo;
        this.statsCalculator = statsCalculator;
    }

    @Override
    public NoteResponse create(String userId, NoteRequest req) {
        Note note = new Note();
        note.setUserId(userId);
        note.setTitle(req.title());
        note.setText(req.text());
        note.setTags(req.tags() == null ? Set.of() : req.tags());
        note.setCreatedDate(Instant.now());

        Note saved = repo.save(note);
        return toFullResponse(saved);
    }

    @Override
    public NoteResponse update(String userId, String id, NoteRequest req) {
        Note note = repo.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new NotFoundException("Note not found"));

        note.setTitle(req.title());
        note.setText(req.text());
        note.setTags(req.tags() == null ? Set.of() : req.tags());

        Note saved = repo.save(note);
        return toFullResponse(saved);
    }

    @Override
    public void delete(String userId, String id) {
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
    public NoteResponse getText(String userId, String id) {
        Note note = repo.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new NotFoundException("Note not found"));

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
    public NoteStatsResponse getStats(String userId, String id) {
        Note note = repo.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new NotFoundException("Note not found"));

        Map<String, Integer> stats = statsCalculator.countWords(note.getText());
        return new NoteStatsResponse(stats);
    }

    private NoteResponse toFullResponse(Note n) {
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
