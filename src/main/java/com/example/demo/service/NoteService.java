package com.example.demo.service;

import com.example.demo.dto.*;
import com.example.demo.model.Tag;

public interface NoteService {
    NoteResponse create(NoteRequest req);
    NoteResponse update(String id, NoteRequest req);
    void delete(String id);
    NotesPageResponse list(int page, int size, Tag tag);
    NoteResponse getText(String id);
    NoteStatsResponse getStats(String id);
}
