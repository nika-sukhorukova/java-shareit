package ru.practicum.shareit.request;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.shareit.common.RequestHeaders;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.request.dto.ItemAnswerDto;
import ru.practicum.shareit.request.dto.ItemRequestDto;
import ru.practicum.shareit.request.dto.ItemRequestWithItemsDto;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ItemRequestController.class)
class ItemRequestControllerTest {
    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper mapper;

    @MockBean
    private ItemRequestService requestService;

    private final LocalDateTime created = LocalDateTime.of(2026, 11, 5, 12, 0, 0);

    private final ItemRequestWithItemsDto requestWithItems = ItemRequestWithItemsDto.builder()
            .id(1L).description("Нужна дрель").created(created)
            .items(List.of(new ItemAnswerDto(10L, "Дрель", 2L)))
            .build();

    @Test
    void create_shouldReturn201() throws Exception {
        ItemRequestDto saved = new ItemRequestDto(1L, "Нужна дрель", created);
        when(requestService.create(eq(1L), any(ItemRequestDto.class))).thenReturn(saved);

        mvc.perform(post("/requests")
                        .header(RequestHeaders.USER_ID, 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\": \"Нужна дрель\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.description").value("Нужна дрель"))
                .andExpect(jsonPath("$.created").value("2026-11-05T12:00:00"));
    }

    @Test
    void create_withoutUserHeader_shouldReturn400() throws Exception {
        mvc.perform(post("/requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\": \"Нужна дрель\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getOwn_shouldReturnRequestsWithItems() throws Exception {
        when(requestService.getOwn(1L)).thenReturn(List.of(requestWithItems));

        mvc.perform(get("/requests").header(RequestHeaders.USER_ID, 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].items[0].id").value(10))
                .andExpect(jsonPath("$[0].items[0].name").value("Дрель"))
                .andExpect(jsonPath("$[0].items[0].ownerId").value(2));
    }

    @Test
    void getAll_shouldReturnOtherUsersRequests() throws Exception {
        when(requestService.getAll(2L)).thenReturn(List.of(requestWithItems));

        mvc.perform(get("/requests/all").header(RequestHeaders.USER_ID, 2L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].description").value("Нужна дрель"));
    }

    @Test
    void getById_shouldReturnRequest() throws Exception {
        when(requestService.getById(2L, 1L)).thenReturn(requestWithItems);

        mvc.perform(get("/requests/1").header(RequestHeaders.USER_ID, 2L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.items[0].ownerId").value(2));
    }

    @Test
    void getById_whenNotFound_shouldReturn404() throws Exception {
        when(requestService.getById(2L, 99L)).thenThrow(new NotFoundException("Запрос не найден"));

        mvc.perform(get("/requests/99").header(RequestHeaders.USER_ID, 2L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.description").value("Запрос не найден"));
    }
}
