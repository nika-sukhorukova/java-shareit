package ru.practicum.shareit.user;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.shareit.user.dto.UpdateUserDto;
import ru.practicum.shareit.user.dto.UserDto;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
class UserControllerTest {
    @Autowired
    private MockMvc mvc;

    @MockBean
    private UserClient userClient;

    @Test
    void create_shouldProxyToServer() throws Exception {
        when(userClient.create(any(UserDto.class)))
                .thenReturn(ResponseEntity.status(201).body(Map.of("id", 1, "email", "anna@mail.ru")));

        mvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Анна\", \"email\": \"anna@mail.ru\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void create_withInvalidEmail_shouldReturn400() throws Exception {
        mvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Анна\", \"email\": \"user.com\"}"))
                .andExpect(status().isBadRequest());

        verify(userClient, never()).create(any());
    }

    @Test
    void create_withoutEmail_shouldReturn400() throws Exception {
        mvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Анна\"}"))
                .andExpect(status().isBadRequest());

        verify(userClient, never()).create(any());
    }

    @Test
    void update_shouldProxyToServer() throws Exception {
        when(userClient.update(eq(1L), any(UpdateUserDto.class)))
                .thenReturn(ResponseEntity.ok(Map.of("id", 1, "name", "Аня")));

        mvc.perform(patch("/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Аня\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Аня"));
    }

    @Test
    void update_withInvalidEmail_shouldReturn400() throws Exception {
        mvc.perform(patch("/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"user.com\"}"))
                .andExpect(status().isBadRequest());

        verify(userClient, never()).update(eq(1L), any());
    }

    @Test
    void getById_shouldProxyToServer() throws Exception {
        when(userClient.getById(1L)).thenReturn(ResponseEntity.ok(Map.of("id", 1)));

        mvc.perform(get("/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void getAll_shouldProxyToServer() throws Exception {
        when(userClient.getAll()).thenReturn(ResponseEntity.ok(List.of(Map.of("id", 1))));

        mvc.perform(get("/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    void delete_shouldProxyToServer() throws Exception {
        when(userClient.delete(1L)).thenReturn(ResponseEntity.ok().build());

        mvc.perform(delete("/users/1"))
                .andExpect(status().isOk());

        verify(userClient).delete(1L);
    }
}
