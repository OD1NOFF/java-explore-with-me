package ru.practicum.integration;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.category.dto.NewCategoryDto;
import ru.practicum.client.StatsClient;
import ru.practicum.event.dto.LocationDto;
import ru.practicum.event.dto.NewEventDto;
import ru.practicum.user.dto.NewUserRequest;

/**
 * Сквозной happy-path через MockMvc + H2: пользователь -> категория -> событие
 * (создание, публикация администратором) -> публичный список -> заявка на участие
 * -> подтверждение инициатором. Проверяет, что весь стек (контроллер, сервис,
 * репозиторий, JPQL-агрегаты, маппинг) реально стыкуется, а не только компилируется.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class MainServiceHappyPathTest {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private StatsClient statsClient;

    @BeforeEach
    void stubStatsClient() {
        Mockito.when(statsClient.getStats(ArgumentMatchers.any(), ArgumentMatchers.any(),
                        ArgumentMatchers.any(), ArgumentMatchers.anyBoolean()))
                .thenReturn(List.of());
    }

    @Test
    void fullLifecycle_userCreatesEvent_adminPublishes_otherUserParticipates() throws Exception {
        // 1. Создаём инициатора и участника
        Long initiatorId = createUser("Организатор", "organizer@example.com");
        Long participantId = createUser("Участник", "participant@example.com");

        // 2. Категория
        NewCategoryDto newCategory = NewCategoryDto.builder().name("Концерты").build();
        String categoryResponse = mockMvc.perform(post("/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newCategory)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long categoryId = objectMapper.readTree(categoryResponse).get("id").asLong();

        // 3. Событие от инициатора (минимум через 3 часа, чтобы прошло и создание, и публикацию за час)
        String eventDate = LocalDateTime.now().plusHours(3).format(FMT);
        NewEventDto newEvent = NewEventDto.builder()
                .title("Концерт группы Java Core")
                .annotation("Аннотация события должна быть не короче двадцати символов")
                .description("Подробное описание события тоже должно быть не короче двадцати символов")
                .category(categoryId)
                .eventDate(eventDate)
                .location(LocationDto.builder().lat(55.75).lon(37.62).build())
                .paid(false)
                .participantLimit(1)
                .requestModeration(true)
                .build();

        String eventResponse = mockMvc.perform(post("/users/{userId}/events", initiatorId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newEvent)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.state").value("PENDING"))
                .andReturn().getResponse().getContentAsString();
        Long eventId = objectMapper.readTree(eventResponse).get("id").asLong();

        // 4. Админ публикует событие
        mockMvc.perform(patch("/admin/events/{eventId}", eventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"stateAction\":\"PUBLISH_EVENT\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("PUBLISHED"));

        // 5. Событие видно в публичном списке
        mockMvc.perform(get("/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(eventId));

        // 6. Участник подаёт заявку -> т.к. requestModeration=true и лимит=1, статус PENDING
        String requestResponse = mockMvc.perform(post("/users/{userId}/requests", participantId)
                        .param("eventId", String.valueOf(eventId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn().getResponse().getContentAsString();
        Long requestId = objectMapper.readTree(requestResponse).get("id").asLong();

        // 7. Инициатор подтверждает заявку
        String confirmBody = "{\"requestIds\":[" + requestId + "],\"status\":\"CONFIRMED\"}";
        mockMvc.perform(patch("/users/{userId}/events/{eventId}/requests", initiatorId, eventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(confirmBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.confirmedRequests", hasSize(1)))
                .andExpect(jsonPath("$.rejectedRequests", hasSize(0)));

        // 8. confirmedRequests в карточке события теперь 1
        mockMvc.perform(get("/events/{id}", eventId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.confirmedRequests").value(1));
    }

    @Test
    void categoriesCompilationsAndErrorPaths() throws Exception {
        NewCategoryDto newCategory = NewCategoryDto.builder().name("Театр").build();
        String categoryResponse = mockMvc.perform(post("/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newCategory)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long categoryId = objectMapper.readTree(categoryResponse).get("id").asLong();

        mockMvc.perform(get("/categories"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/categories/{catId}", categoryId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Театр"));

        mockMvc.perform(patch("/admin/categories/{catId}", categoryId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Театр и опера\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Театр и опера"));

        mockMvc.perform(get("/categories/{catId}", 999_999))
                .andExpect(status().isNotFound());

        Long initiatorId = createUser("Организатор2", "organizer2@example.com");
        String eventDate = LocalDateTime.now().plusHours(3).format(FMT);
        NewEventDto newEvent = NewEventDto.builder()
                .title("Спектакль Чайка")
                .annotation("Аннотация события должна быть не короче двадцати символов")
                .description("Подробное описание события тоже должно быть не короче двадцати символов")
                .category(categoryId)
                .eventDate(eventDate)
                .location(LocationDto.builder().lat(55.75).lon(37.62).build())
                .build();
        String eventResponse = mockMvc.perform(post("/users/{userId}/events", initiatorId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newEvent)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long eventId = objectMapper.readTree(eventResponse).get("id").asLong();

        mockMvc.perform(delete("/admin/categories/{catId}", categoryId))
                .andExpect(status().isConflict());

        NewEventDto tooSoonEvent = NewEventDto.builder()
                .title("Слишком скорый спектакль")
                .annotation("Аннотация события должна быть не короче двадцати символов")
                .description("Подробное описание события тоже должно быть не короче двадцати символов")
                .category(categoryId)
                .eventDate(LocalDateTime.now().plusMinutes(5).format(FMT))
                .location(LocationDto.builder().lat(55.75).lon(37.62).build())
                .build();
        mockMvc.perform(post("/users/{userId}/events", initiatorId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(tooSoonEvent)))
                .andExpect(status().isConflict());

        mockMvc.perform(get("/users/{userId}/events", initiatorId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
        mockMvc.perform(get("/users/{userId}/events/{eventId}", initiatorId, eventId))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/admin/events/{eventId}", eventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"stateAction\":\"PUBLISH_EVENT\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(patch("/admin/events/{eventId}", eventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"stateAction\":\"REJECT_EVENT\"}"))
                .andExpect(status().isConflict());

        mockMvc.perform(get("/admin/events").param("categories", String.valueOf(categoryId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        mockMvc.perform(get("/events")
                        .param("rangeStart", "2030-01-01 00:00:00")
                        .param("rangeEnd", "2020-01-01 00:00:00"))
                .andExpect(status().isBadRequest());

        String newCompilation = "{\"title\":\"Премьеры сезона\",\"pinned\":true,\"events\":[" + eventId + "]}";
        String compResponse = mockMvc.perform(post("/admin/compilations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(newCompilation))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.events", hasSize(1)))
                .andReturn().getResponse().getContentAsString();
        Long compId = objectMapper.readTree(compResponse).get("id").asLong();

        mockMvc.perform(get("/compilations").param("pinned", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
        mockMvc.perform(get("/compilations").param("pinned", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
        mockMvc.perform(get("/compilations/{compId}", compId))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/admin/compilations/{compId}", compId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pinned\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pinned").value(false));

        mockMvc.perform(delete("/admin/compilations/{compId}", compId))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/compilations/{compId}", compId))
                .andExpect(status().isNotFound());

        Long throwawayUserId = createUser("Одноразовый", "throwaway@example.com");
        mockMvc.perform(get("/admin/users").param("ids", String.valueOf(throwawayUserId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
        mockMvc.perform(delete("/admin/users/{userId}", throwawayUserId))
                .andExpect(status().isNoContent());
        mockMvc.perform(delete("/admin/users/{userId}", throwawayUserId))
                .andExpect(status().isNotFound());
    }

    private Long createUser(String name, String email) throws Exception {
        NewUserRequest request = NewUserRequest.builder().name(name).email(email).build();
        String response = mockMvc.perform(post("/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }
}