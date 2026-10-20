package ru.practicum.shareit.item;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.shareit.common.RequestHeaders;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.exception.ValidationException;
import ru.practicum.shareit.item.dto.CommentDto;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.dto.ItemWithBookingsDto;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
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

    @Autowired
    private ObjectMapper mapper;

    @MockBean
    private ItemService itemService;

    private final ItemDto itemDto = ItemDto.builder()
            .id(1L).name("Дрель").description("Ударная дрель").available(true).build();

    @Test
    void create_shouldReturn201() throws Exception {
        when(itemService.create(eq(1L), any(ItemDto.class))).thenReturn(itemDto);

        mvc.perform(post("/items")
                        .header(RequestHeaders.USER_ID, 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(itemDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Дрель"));
    }

    @Test
    void create_withEmptyName_shouldReturn400() throws Exception {
        ItemDto invalid = ItemDto.builder().name("").description("Ударная дрель").available(true).build();

        mvc.perform(post("/items")
                        .header(RequestHeaders.USER_ID, 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());

        verify(itemService, never()).create(anyLong(), any());
    }

    @Test
    void create_withoutAvailable_shouldReturn400() throws Exception {
        ItemDto invalid = ItemDto.builder().name("Дрель").description("Ударная дрель").build();

        mvc.perform(post("/items")
                        .header(RequestHeaders.USER_ID, 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_withoutUserHeader_shouldReturn400() throws Exception {
        mvc.perform(post("/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(itemDto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void update_byNotOwner_shouldReturn404() throws Exception {
        when(itemService.update(eq(2L), eq(1L), any(ItemDto.class))).thenThrow(new NotFoundException("Не найдена"));

        mvc.perform(patch("/items/1")
                        .header(RequestHeaders.USER_ID, 2L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Пила\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getById_shouldReturnItemWithBookingsAndComments() throws Exception {
        ItemWithBookingsDto dto = ItemWithBookingsDto.builder()
                .id(1L).name("Дрель").description("Ударная дрель").available(true)
                .comments(List.of(CommentDto.builder().id(7L).text("Отлично").authorName("Анна").build()))
                .build();
        when(itemService.getById(2L, 1L)).thenReturn(dto);

        mvc.perform(get("/items/1").header(RequestHeaders.USER_ID, 2L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lastBooking").isEmpty())
                .andExpect(jsonPath("$.nextBooking").isEmpty())
                .andExpect(jsonPath("$.comments.length()").value(1))
                .andExpect(jsonPath("$.comments[0].authorName").value("Анна"));
    }

    @Test
    void getByOwner_shouldReturnList() throws Exception {
        when(itemService.getByOwner(1L)).thenReturn(List.of(ItemWithBookingsDto.builder().id(1L).build()));

        mvc.perform(get("/items").header(RequestHeaders.USER_ID, 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void search_shouldPassText() throws Exception {
        when(itemService.search("дрель")).thenReturn(List.of(itemDto));

        mvc.perform(get("/items/search").param("text", "дрель"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Дрель"));
    }

    @Test
    void addComment_shouldReturnComment() throws Exception {
        CommentDto saved = CommentDto.builder()
                .id(7L).text("Отлично").authorName("Анна").created(LocalDateTime.now()).build();
        when(itemService.addComment(eq(2L), eq(1L), any(CommentDto.class))).thenReturn(saved);

        mvc.perform(post("/items/1/comment")
                        .header(RequestHeaders.USER_ID, 2L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\": \"Отлично\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text").value("Отлично"))
                .andExpect(jsonPath("$.authorName").value("Анна"));
    }

    @Test
    void addComment_withBlankText_shouldReturn400() throws Exception {
        mvc.perform(post("/items/1/comment")
                        .header(RequestHeaders.USER_ID, 2L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\": \" \"}"))
                .andExpect(status().isBadRequest());

        verify(itemService, never()).addComment(anyLong(), anyLong(), any());
    }

    @Test
    void addComment_withoutFinishedBooking_shouldReturn400() throws Exception {
        when(itemService.addComment(eq(2L), eq(1L), any(CommentDto.class)))
                .thenThrow(new ValidationException("Не брал в аренду"));

        mvc.perform(post("/items/1/comment")
                        .header(RequestHeaders.USER_ID, 2L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\": \"Отлично\"}"))
                .andExpect(status().isBadRequest());
    }
}
