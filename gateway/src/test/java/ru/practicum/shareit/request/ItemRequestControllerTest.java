package ru.practicum.shareit.request;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.shareit.common.RequestHeaders;
import ru.practicum.shareit.request.dto.ItemRequestDto;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ItemRequestController.class)
class ItemRequestControllerTest {
    @Autowired
    private MockMvc mvc;

    @MockBean
    private ItemRequestClient requestClient;

    @Test
    void create_shouldProxyToServer() throws Exception {
        when(requestClient.create(eq(1L), any(ItemRequestDto.class)))
                .thenReturn(ResponseEntity.status(201).body(Map.of("id", 1, "description", "Нужна дрель")));

        mvc.perform(post("/requests")
                        .header(RequestHeaders.USER_ID, 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\": \"Нужна дрель\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.description").value("Нужна дрель"));
    }

    @Test
    void create_withBlankDescription_shouldReturn400() throws Exception {
        mvc.perform(post("/requests")
                        .header(RequestHeaders.USER_ID, 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\": \"\"}"))
                .andExpect(status().isBadRequest());

        verify(requestClient, never()).create(anyLong(), any());
    }

    @Test
    void create_withoutBody_shouldReturn400() throws Exception {
        mvc.perform(post("/requests")
                        .header(RequestHeaders.USER_ID, 1L)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getOwn_shouldProxyToServer() throws Exception {
        when(requestClient.getOwn(1L)).thenReturn(ResponseEntity.ok(List.of(Map.of("id", 1))));

        mvc.perform(get("/requests").header(RequestHeaders.USER_ID, 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    void getAll_shouldProxyToServer() throws Exception {
        when(requestClient.getAll(2L)).thenReturn(ResponseEntity.ok(List.of(Map.of("id", 1))));

        mvc.perform(get("/requests/all").header(RequestHeaders.USER_ID, 2L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    void getById_shouldProxyToServer() throws Exception {
        when(requestClient.getById(2L, 1L)).thenReturn(ResponseEntity.ok(Map.of("id", 1)));

        mvc.perform(get("/requests/1").header(RequestHeaders.USER_ID, 2L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void getById_withoutUserHeader_shouldReturn400() throws Exception {
        mvc.perform(get("/requests/1"))
                .andExpect(status().isBadRequest());
    }
}
