package com.example.demo.service;

import com.example.demo.dto.*;
import com.example.demo.model.Tag;

public interface NoteService {
    NoteResponse create(NoteRequest req);

    NoteResponse update(String id, String userId, NoteRequest req);

    void delete(String id, String userId);

    NotesPageResponse list(String userId, int page, int size, Tag tag);

    NoteResponse getText(String id, String userId);

    NoteStatsResponse getStats(String id, String userId);

}
