package com.example.demo.service;

import com.example.demo.dto.*;
import com.example.demo.model.Tag;

/**
 * Notes business API.
 * <p>
 * The userId comes from the URL path and must be used to scope every operation
 * to the note owner.
 */
public interface NoteService {

    NoteResponse create(String userId, NoteRequest req);

    NoteResponse update(String userId, String id, NoteRequest req);

    void delete(String userId, String id);

    NotesPageResponse list(String userId, int page, int size, Tag tag);

    NoteResponse getText(String userId, String id);

    NoteStatsResponse getStats(String userId, String id);
}
