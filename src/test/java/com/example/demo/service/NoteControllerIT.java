package com.example.demo.service;

import com.example.demo.BaseMongoIT;
import com.example.demo.dto.*;
import com.example.demo.model.Tag;
import com.example.demo.repository.NoteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class NoteControllerIT extends BaseMongoIT {

    @Autowired TestRestTemplate rest;
    @Autowired NoteRepository repo;

    @BeforeEach
    void clean() {
        repo.deleteAll();
    }

    // -------- CREATE + VALIDATION --------

    @Test
    void create_shouldReturn201_andPersist() {
        var req = new NoteRequest("My first note", "note is just a note", Set.of(Tag.BUSINESS, Tag.IMPORTANT));

        ResponseEntity<NoteResponse> resp = rest.postForEntity("/api/notes", req, NoteResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().id()).isNotBlank();
        assertThat(resp.getBody().title()).isEqualTo("My first note");
        assertThat(resp.getBody().createdDate()).isNotNull();
        assertThat(resp.getBody().text()).isEqualTo("note is just a note");
        assertThat(resp.getBody().tags()).contains(Tag.BUSINESS, Tag.IMPORTANT);
        assertThat(repo.count()).isEqualTo(1);
    }

    @Test
    void create_shouldReturn400_whenTitleBlank() {
        var req = new NoteRequest("   ", "text", Set.of(Tag.BUSINESS));

        ResponseEntity<String> resp = rest.postForEntity("/api/notes", req, String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(repo.count()).isZero();
    }

    @Test
    void create_shouldReturn400_whenTextBlank() {
        var req = new NoteRequest("Title", "   ", Set.of(Tag.BUSINESS));

        ResponseEntity<String> resp = rest.postForEntity("/api/notes", req, String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(repo.count()).isZero();
    }

    @Test
    void create_shouldReturn400_whenTagInvalidEnum() {
        String json = """
            {"title":"Hello","text":"World","tags":["FUN"]}
            """;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<String> resp = rest.exchange(
                "/api/notes",
                HttpMethod.POST,
                new HttpEntity<>(json, headers),
                String.class
        );

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(repo.count()).isZero();
    }

    // -------- UPDATE --------

    @Test
    void update_shouldReturn404_whenNoteNotFound() {
        var req = new NoteRequest("new", "new text", Set.of(Tag.BUSINESS));

        ResponseEntity<String> resp = rest.exchange(
                "/api/notes/missing-id",
                HttpMethod.PUT,
                new HttpEntity<>(req),
                String.class
        );

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void update_shouldReturn200_andUpdateFields() {
        String id = createNote("old", "old text", Set.of(Tag.PERSONAL));

        var req = new NoteRequest("new", "new text", Set.of(Tag.BUSINESS));

        ResponseEntity<NoteResponse> resp = rest.exchange(
                "/api/notes/" + id,
                HttpMethod.PUT,
                new HttpEntity<>(req),
                NoteResponse.class
        );

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().id()).isEqualTo(id);
        assertThat(resp.getBody().title()).isEqualTo("new");
        assertThat(resp.getBody().text()).isEqualTo("new text");

        var fromDb = repo.findById(id).orElseThrow();
        assertThat(fromDb.getTitle()).isEqualTo("new");
        assertThat(fromDb.getText()).isEqualTo("new text");
        assertThat(fromDb.getTags()).containsExactlyInAnyOrder(Tag.BUSINESS);
    }

    // -------- DELETE --------

    @Test
    void delete_shouldReturn404_whenMissing() {
        ResponseEntity<String> resp = rest.exchange(
                "/api/notes/missing-id",
                HttpMethod.DELETE,
                HttpEntity.EMPTY,
                String.class
        );

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void delete_shouldReturn204_andRemove() {
        String id = createNote("t", "text", Set.of(Tag.IMPORTANT));

        ResponseEntity<Void> resp = rest.exchange(
                "/api/notes/" + id,
                HttpMethod.DELETE,
                HttpEntity.EMPTY,
                Void.class
        );

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(repo.findById(id)).isEmpty();
    }

    // -------- LIST: newest first + pagination + filter --------

    @Test
    void list_shouldReturnNewestFirst_andWithoutText() throws Exception {
        String id1 = createNote("t1", "a", Set.of(Tag.BUSINESS));
        Thread.sleep(5);
        String id2 = createNote("t2", "b", Set.of(Tag.BUSINESS));
        Thread.sleep(5);
        String id3 = createNote("t3", "c", Set.of(Tag.BUSINESS));

        ResponseEntity<NotesPageResponse> resp = rest.getForEntity(
                "/api/notes?page=0&size=10",
                NotesPageResponse.class
        );

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isNotNull();

        List<NoteResponse> items = resp.getBody().items();
        assertThat(items).extracting(NoteResponse::id).containsExactly(id3, id2, id1);

        assertThat(items.get(0).createdDate()).isNotNull();
        assertThat(items.get(0).title()).isNotBlank();
        assertThat(items.get(0).text()).isNull();
    }

    @Test
    void list_shouldSupportPagination() {
        createNote("t1", "a", Set.of(Tag.BUSINESS));
        createNote("t2", "b", Set.of(Tag.BUSINESS));
        createNote("t3", "c", Set.of(Tag.BUSINESS));

        NotesPageResponse page0 = rest.getForObject("/api/notes?page=0&size=2", NotesPageResponse.class);
        NotesPageResponse page1 = rest.getForObject("/api/notes?page=1&size=2", NotesPageResponse.class);

        assertThat(page0.items()).hasSize(2);
        assertThat(page1.items()).hasSize(1);
        assertThat(page0.totalItems()).isEqualTo(3);
        assertThat(page0.totalPages()).isEqualTo(2);
    }

    @Test
    void list_shouldFilterByTag() {
        createNote("b1", "x", Set.of(Tag.BUSINESS));
        createNote("p1", "y", Set.of(Tag.PERSONAL));
        createNote("b2", "z", Set.of(Tag.BUSINESS, Tag.IMPORTANT));

        NotesPageResponse business =
                rest.getForObject("/api/notes?page=0&size=10&tag=BUSINESS", NotesPageResponse.class);

        assertThat(business.items()).extracting(NoteResponse::title)
                .containsExactlyInAnyOrder("b1", "b2");
    }

    // -------- TEXT --------

    @Test
    void text_shouldReturn404_whenMissing() {
        ResponseEntity<String> resp = rest.getForEntity("/api/notes/missing/text", String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void text_shouldReturnTextOnly() {
        String id = createNote("t", "note is just a note", Set.of());

        ResponseEntity<NoteResponse> resp = rest.getForEntity("/api/notes/" + id + "/text", NoteResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().id()).isEqualTo(id);
        assertThat(resp.getBody().text()).isEqualTo("note is just a note");

        assertThat(resp.getBody().title()).isNull();
        assertThat(resp.getBody().createdDate()).isNull();
    }

    // -------- STATS --------

    @Test
    void stats_shouldCountWords_andSortDesc() {
        String id = createNote("t", "note is just a note", Set.of());

        ResponseEntity<NoteStatsResponse> resp =
                rest.getForEntity("/api/notes/" + id + "/stats", NoteStatsResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isNotNull();

        Map<String, Integer> stats = resp.getBody().stats();
        assertThat(stats).containsEntry("note", 2)
                .containsEntry("is", 1)
                .containsEntry("just", 1)
                .containsEntry("a", 1);

        assertThat(stats.keySet().stream().toList().get(0)).isEqualTo("note");
    }

    @Test
    void stats_shouldNormalizeCase_andIgnorePunctuation() {
        String id = createNote("t", "Note, note!", Set.of());

        NoteStatsResponse resp =
                rest.getForObject("/api/notes/" + id + "/stats", NoteStatsResponse.class);

        assertThat(resp.stats()).containsEntry("note", 2);
        assertThat(resp.stats()).doesNotContainKey("Note");
    }

    private String createNote(String title, String text, Set<Tag> tags) {
        var req = new NoteRequest(title, text, tags);
        ResponseEntity<NoteResponse> resp = rest.postForEntity("/api/notes", req, NoteResponse.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return resp.getBody().id();
    }
}
