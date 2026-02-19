package com.example.test.service;

import com.example.test.BaseMongoIT;
import com.example.task.dto.NoteRequest;
import com.example.task.dto.NoteResponse;
import com.example.task.dto.NoteStatsResponse;
import com.example.task.dto.NotesPageResponse;
import com.example.task.model.Tag;
import com.example.task.repository.NoteRepository;
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

/**
 * Integration tests for Notes REST API.
 * <p>
 * The tests call HTTP endpoints and verify MongoDB persistence through Testcontainers.
 */
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
        var req = new NoteRequest("My first note", "note is just a note", Set.of(Tag.BUSINESS, Tag.IMPORTANT));

        ResponseEntity<NoteResponse> resp = rest.postForEntity(baseUrl(U1), req, NoteResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().userId()).isEqualTo(U1);
        assertThat(resp.getBody().title()).isEqualTo("My first note");
        assertThat(resp.getBody().createdDate()).isNotNull();
        assertThat(repo.findById(resp.getBody().id())).isPresent();
    }

    @Test
    void create_shouldReturn400_whenTitleIsBlank() {
        var req = new NoteRequest("   ", "text", Set.of(Tag.BUSINESS));

        ResponseEntity<String> resp = rest.postForEntity(baseUrl(U1), req, String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void create_shouldReturn400_whenTextIsBlank() {
        var req = new NoteRequest("title", "   ", Set.of(Tag.BUSINESS));

        ResponseEntity<String> resp = rest.postForEntity(baseUrl(U1), req, String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void create_shouldReturn400_whenTagIsInvalidEnumValue() {
        String rawJson = """
                {
                  "title":"t",
                  "text":"hello",
                  "tags":["BUSINESS","BAD_TAG"]
                }
                """;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<String> resp = rest.postForEntity(
                baseUrl(U1),
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

        NotesPageResponse u1 = rest.getForObject(baseUrl(U1) + "?page=0&size=10", NotesPageResponse.class);
        NotesPageResponse u2 = rest.getForObject(baseUrl(U2) + "?page=0&size=10", NotesPageResponse.class);

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
                baseUrl(U1) + "?page=0&size=10&tag=BUSINESS",
                NotesPageResponse.class
        );

        assertThat(business.items()).extracting(NoteResponse::title)
                .containsExactlyInAnyOrder("b1", "b2");
    }

    @Test
    void list_shouldSupportPagination() throws Exception {
        create(U1, "t1", "a", Set.of(Tag.BUSINESS));
        Thread.sleep(5);
        create(U1, "t2", "b", Set.of(Tag.BUSINESS));
        Thread.sleep(5);
        create(U1, "t3", "c", Set.of(Tag.BUSINESS));

        NotesPageResponse page0 = rest.getForObject(baseUrl(U1) + "?page=0&size=2", NotesPageResponse.class);
        NotesPageResponse page1 = rest.getForObject(baseUrl(U1) + "?page=1&size=2", NotesPageResponse.class);

        assertThat(page0.items()).hasSize(2);
        assertThat(page0.totalItems()).isEqualTo(3);
        assertThat(page0.totalPages()).isEqualTo(2);

        assertThat(page1.items()).hasSize(1);
        assertThat(page1.totalItems()).isEqualTo(3);
        assertThat(page1.totalPages()).isEqualTo(2);
    }

    @Test
    void update_shouldReturn200_andUpdateFields() {
        String id = create(U1, "old", "old text", Set.of(Tag.PERSONAL));

        var upd = new NoteRequest("new", "new text", Set.of(Tag.BUSINESS));

        ResponseEntity<NoteResponse> resp = rest.exchange(
                baseUrl(U1) + "/" + id,
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

        ResponseEntity<NoteResponse> resp = rest.getForEntity(
                baseUrl(U1) + "/" + id + "/text",
                NoteResponse.class
        );

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().userId()).isEqualTo(U1);
        assertThat(resp.getBody().id()).isEqualTo(id);
        assertThat(resp.getBody().text()).isEqualTo("note is just a note");
    }

    @Test
    void stats_shouldReturnWordCounts_sortedDesc() {
        String id = create(U1, "t", "note is just a note", Set.of());

        ResponseEntity<NoteStatsResponse> resp = rest.getForEntity(
                baseUrl(U1) + "/" + id + "/stats",
                NoteStatsResponse.class
        );

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isNotNull();

        Map<String, Integer> stats = resp.getBody().stats();
        assertThat(stats.get("note")).isEqualTo(2);
        assertThat(stats.get("is")).isEqualTo(1);
        assertThat(stats.get("just")).isEqualTo(1);
        assertThat(stats.get("a")).isEqualTo(1);

        String firstKey = new ArrayList<>(stats.keySet()).get(0);
        assertThat(firstKey).isEqualTo("note");
    }

    @Test
    void delete_shouldReturn204_andRemoveNote() {
        String id = create(U1, "t", "text", Set.of(Tag.BUSINESS));

        ResponseEntity<Void> resp = rest.exchange(
                baseUrl(U1) + "/" + id,
                HttpMethod.DELETE,
                HttpEntity.EMPTY,
                Void.class
        );

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(repo.findById(id)).isEmpty();

        ResponseEntity<String> textResp = rest.getForEntity(baseUrl(U1) + "/" + id + "/text", String.class);
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
                baseUrl(U1) + "?page=0&size=10",
                NotesPageResponse.class
        );

        assertThat(page.items()).extracting(NoteResponse::id).containsExactly(id3, id2, id1);
        assertThat(page.items()).allMatch(n -> n.text() == null);
    }

    private String create(String userId, String title, String text, Set<Tag> tags) {
        var req = new NoteRequest(title, text, tags);
        ResponseEntity<NoteResponse> resp = rest.postForEntity(baseUrl(userId), req, NoteResponse.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return resp.getBody().id();
    }

    private String baseUrl(String userId) {
        return "/v1/users/" + userId + "/notes";
    }
}
