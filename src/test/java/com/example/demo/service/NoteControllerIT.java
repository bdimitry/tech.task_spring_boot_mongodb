package com.example.demo.service;

import com.example.demo.BaseMongoIT;
import com.example.demo.dto.NoteRequest;
import com.example.demo.dto.NoteResponse;
import com.example.demo.dto.NoteStatsResponse;
import com.example.demo.dto.NotesPageResponse;
import com.example.demo.model.Tag;
import com.example.demo.repository.NoteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class NoteControllerIT extends BaseMongoIT {

    @Autowired TestRestTemplate rest;
    @Autowired NoteRepository repo;

    private static final String U1 = "u1";
    private static final String U2 = "u2";

    @BeforeEach
    void clean() {
        repo.deleteAll();
    }

    @Test
    void create_shouldReturn201_andPersist() {
        var req = new NoteRequest(U1, "My first note", "note is just a note", Set.of(Tag.BUSINESS, Tag.IMPORTANT));

        ResponseEntity<NoteResponse> resp = rest.postForEntity("/api/notes", req, NoteResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(resp.getBody()).isNotNull();

        assertThat(resp.getBody().userId()).isEqualTo(U1);
        assertThat(resp.getBody().title()).isEqualTo("My first note");
        assertThat(resp.getBody().createdDate()).isNotNull();

        var saved = repo.findById(resp.getBody().id()).orElseThrow();
        assertThat(saved.getUserId()).isEqualTo(U1);
    }

    @Test
    void create_shouldReturn400_whenTitleIsBlank() {
        var req = new NoteRequest(U1, "   ", "text", Set.of(Tag.BUSINESS));

        ResponseEntity<String> resp = rest.postForEntity("/api/notes", req, String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void create_shouldReturn400_whenTextIsBlank() {
        var req = new NoteRequest(U1, "title", "   ", Set.of(Tag.BUSINESS));

        ResponseEntity<String> resp = rest.postForEntity("/api/notes", req, String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void create_shouldReturn400_whenTagIsInvalidEnumValue() {
        // Пытаемся отправить несуществующий enum в JSON -> Jackson должен вернуть 400
        String rawJson = """
            {
              "userId":"u1",
              "title":"t",
              "text":"hello",
              "tags":["BUSINESS","BAD_TAG"]
            }
            """;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<String> resp = rest.postForEntity(
                "/api/notes",
                new HttpEntity<>(rawJson, headers),
                String.class
        );

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void list_shouldReturnOnlyUsersNotes() {
        create(U1, "u1-1", "a", Set.of(Tag.BUSINESS));
        create(U1, "u1-2", "b", Set.of(Tag.PERSONAL));
        create(U2, "u2-1", "c", Set.of(Tag.BUSINESS));

        NotesPageResponse u1 = rest.getForObject("/api/notes?userId=" + U1 + "&page=0&size=10", NotesPageResponse.class);
        NotesPageResponse u2 = rest.getForObject("/api/notes?userId=" + U2 + "&page=0&size=10", NotesPageResponse.class);

        assertThat(u1.items()).hasSize(2);
        assertThat(u1.items()).allMatch(n -> U1.equals(n.userId()));

        assertThat(u2.items()).hasSize(1);
        assertThat(u2.items()).allMatch(n -> U2.equals(n.userId()));
    }

    @Test
    void list_shouldFilterByTag() {
        create(U1, "b1", "x", Set.of(Tag.BUSINESS));
        create(U1, "p1", "y", Set.of(Tag.PERSONAL));
        create(U1, "b2", "z", Set.of(Tag.BUSINESS, Tag.IMPORTANT));

        NotesPageResponse business = rest.getForObject(
                "/api/notes?userId=" + U1 + "&page=0&size=10&tag=BUSINESS",
                NotesPageResponse.class
        );

        assertThat(business.items()).extracting(NoteResponse::title)
                .containsExactlyInAnyOrder("b1", "b2");
    }

    @Test
    void list_shouldSupportPagination() throws Exception {
        // 3 заметки, size=2 => totalPages=2
        create(U1, "t1", "a", Set.of(Tag.BUSINESS));
        Thread.sleep(5);
        create(U1, "t2", "b", Set.of(Tag.BUSINESS));
        Thread.sleep(5);
        create(U1, "t3", "c", Set.of(Tag.BUSINESS));

        NotesPageResponse page0 = rest.getForObject("/api/notes?userId=" + U1 + "&page=0&size=2", NotesPageResponse.class);
        NotesPageResponse page1 = rest.getForObject("/api/notes?userId=" + U1 + "&page=1&size=2", NotesPageResponse.class);

        assertThat(page0).isNotNull();
        assertThat(page0.items()).hasSize(2);
        assertThat(page0.totalItems()).isEqualTo(3);
        assertThat(page0.totalPages()).isEqualTo(2);

        assertThat(page1).isNotNull();
        assertThat(page1.items()).hasSize(1);
        assertThat(page1.totalItems()).isEqualTo(3);
        assertThat(page1.totalPages()).isEqualTo(2);
    }

    @Test
    void update_shouldReturn200_andUpdateFields() {
        String id = create(U1, "old", "old text", Set.of(Tag.PERSONAL));

        var upd = new NoteRequest(U1, "new", "new text", Set.of(Tag.BUSINESS));

        ResponseEntity<NoteResponse> resp = rest.exchange(
                "/api/notes/" + id + "?userId=" + U1,
                HttpMethod.PUT,
                new HttpEntity<>(upd),
                NoteResponse.class
        );

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isNotNull();

        assertThat(resp.getBody().userId()).isEqualTo(U1);
        assertThat(resp.getBody().title()).isEqualTo("new");
        assertThat(resp.getBody().text()).isEqualTo("new text");
        assertThat(resp.getBody().tags()).containsExactly(Tag.BUSINESS);
    }

    @Test
    void text_shouldReturnTextOnly() {
        String id = create(U1, "t", "note is just a note", Set.of());

        ResponseEntity<NoteResponse> resp =
                rest.getForEntity("/api/notes/" + id + "/text?userId=" + U1, NoteResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isNotNull();

        assertThat(resp.getBody().userId()).isEqualTo(U1);
        assertThat(resp.getBody().id()).isEqualTo(id);
        assertThat(resp.getBody().text()).isEqualTo("note is just a note");
    }

    @Test
    void stats_shouldReturnWordCounts_sortedDesc() {
        String id = create(U1, "t", "note is just a note", Set.of());

        ResponseEntity<NoteStatsResponse> resp =
                rest.getForEntity("/api/notes/" + id + "/stats?userId=" + U1, NoteStatsResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isNotNull();

        Map<String, Integer> stats = resp.getBody().stats();
        assertThat(stats.get("note")).isEqualTo(2);
        assertThat(stats.get("is")).isEqualTo(1);
        assertThat(stats.get("just")).isEqualTo(1);
        assertThat(stats.get("a")).isEqualTo(1);

        // Проверяем порядок (должно начинаться с "note":2)
        String firstKey = new ArrayList<>(stats.keySet()).get(0);
        assertThat(firstKey).isEqualTo("note");
    }

    @Test
    void delete_shouldReturn204_andRemoveNote() {
        String id = create(U1, "t", "text", Set.of(Tag.BUSINESS));

        ResponseEntity<Void> resp = rest.exchange(
                "/api/notes/" + id + "?userId=" + U1,
                HttpMethod.DELETE,
                HttpEntity.EMPTY,
                Void.class
        );

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(repo.findById(id)).isEmpty();

        // И endpoint текста теперь должен отдавать 404
        ResponseEntity<String> textResp =
                rest.getForEntity("/api/notes/" + id + "/text?userId=" + U1, String.class);

        assertThat(textResp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void list_shouldReturnNewestFirst_andWithoutText() throws Exception {
        String id1 = create(U1, "t1", "a", Set.of(Tag.BUSINESS));
        Thread.sleep(5);
        String id2 = create(U1, "t2", "b", Set.of(Tag.BUSINESS));
        Thread.sleep(5);
        String id3 = create(U1, "t3", "c", Set.of(Tag.BUSINESS));

        NotesPageResponse page = rest.getForObject(
                "/api/notes?userId=" + U1 + "&page=0&size=10",
                NotesPageResponse.class
        );

        assertThat(page.items()).extracting(NoteResponse::id).containsExactly(id3, id2, id1);

        // В list() сервис специально обнуляет text
        assertThat(page.items()).allMatch(n -> n.text() == null);
    }

    private String create(String userId, String title, String text, Set<Tag> tags) {
        var req = new NoteRequest(userId, title, text, tags);
        ResponseEntity<NoteResponse> resp = rest.postForEntity("/api/notes", req, NoteResponse.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return resp.getBody().id();
    }
}
