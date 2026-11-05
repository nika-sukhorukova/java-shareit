package ru.practicum.shareit.item;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.shareit.common.RequestHeaders;
import ru.practicum.shareit.item.dto.CommentDto;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.dto.UpdateItemDto;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ItemController.class)
class ItemControllerTest {
    @Autowired
    private MockMvc mvc;

    @MockBean
    private ItemClient itemClient;

    @Test
    void create_shouldProxyToServer() throws Exception {
        when(itemClient.create(eq(1L), any(ItemDto.class)))
                .thenReturn(ResponseEntity.status(201).body(Map.of("id", 1, "requestId", 5)));

        mvc.perform(post("/items")
                        .header(RequestHeaders.USER_ID, 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Дрель\", \"description\": \"Ударная\", \"available\": true, \"requestId\": 5}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.requestId").value(5));
    }

    @Test
    void create_withoutName_shouldReturn400() throws Exception {
        mvc.perform(post("/items")
                        .header(RequestHeaders.USER_ID, 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\": \"Ударная\", \"available\": true}"))
                .andExpect(status().isBadRequest());

        verify(itemClient, never()).create(anyLong(), any());
    }

    @Test
    void create_withoutAvailable_shouldReturn400() throws Exception {
        mvc.perform(post("/items")
                        .header(RequestHeaders.USER_ID, 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Дрель\", \"description\": \"Ударная\"}"))
                .andExpect(status().isBadRequest());

        verify(itemClient, never()).create(anyLong(), any());
    }

    @Test
    void create_withoutUserHeader_shouldReturn400() throws Exception {
        mvc.perform(post("/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Дрель\", \"description\": \"Ударная\", \"available\": true}"))
                .andExpect(status().isBadRequest());

        verify(itemClient, never()).create(anyLong(), any());
    }

    @Test
    void update_shouldProxyToServer() throws Exception {
        when(itemClient.update(eq(1L), eq(2L), any(UpdateItemDto.class)))
                .thenReturn(ResponseEntity.ok(Map.of("id", 2, "available", false)));

        mvc.perform(patch("/items/2")
                        .header(RequestHeaders.USER_ID, 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"available\": false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(false));
    }

    @Test
    void getById_shouldProxyToServer() throws Exception {
        when(itemClient.getById(1L, 2L)).thenReturn(ResponseEntity.ok(Map.of("id", 2)));

        mvc.perform(get("/items/2").header(RequestHeaders.USER_ID, 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2));
    }

    @Test
    void getById_whenServerReturns404_shouldReturn404() throws Exception {
        when(itemClient.getById(1L, 99L)).thenReturn(ResponseEntity.status(404).body(Map.of("error", "Не найдено")));

        mvc.perform(get("/items/99").header(RequestHeaders.USER_ID, 1L))
                .andExpect(status().isNotFound());
    }

    @Test
    void getByOwner_shouldProxyToServer() throws Exception {
        when(itemClient.getByOwner(1L)).thenReturn(ResponseEntity.ok(List.of(Map.of("id", 2))));

        mvc.perform(get("/items").header(RequestHeaders.USER_ID, 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(2));
    }

    @Test
    void search_shouldProxyToServer() throws Exception {
        when(itemClient.search(1L, "дрель")).thenReturn(ResponseEntity.ok(List.of(Map.of("id", 2))));

        mvc.perform(get("/items/search").param("text", "дрель").header(RequestHeaders.USER_ID, 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(2));
    }

    @Test
    void search_withBlankText_shouldReturnEmptyListWithoutServerCall() throws Exception {
        mvc.perform(get("/items/search").param("text", " ").header(RequestHeaders.USER_ID, 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        verify(itemClient, never()).search(anyLong(), anyString());
    }

    @Test
    void addComment_shouldProxyToServer() throws Exception {
        when(itemClient.addComment(eq(2L), eq(1L), any(CommentDto.class)))
                .thenReturn(ResponseEntity.ok(Map.of("id", 1, "text", "Отлично")));

        mvc.perform(post("/items/1/comment")
                        .header(RequestHeaders.USER_ID, 2L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\": \"Отлично\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text").value("Отлично"));
    }

    @Test
    void addComment_withBlankText_shouldReturn400() throws Exception {
        mvc.perform(post("/items/1/comment")
                        .header(RequestHeaders.USER_ID, 2L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\": \" \"}"))
                .andExpect(status().isBadRequest());

        verify(itemClient, never()).addComment(anyLong(), anyLong(), any());
    }
}
