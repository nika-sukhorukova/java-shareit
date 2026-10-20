package ru.practicum.shareit.booking;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.shareit.booking.dto.BookingDto;
import ru.practicum.shareit.booking.dto.NewBookingDto;
import ru.practicum.shareit.common.RequestHeaders;
import ru.practicum.shareit.exception.ForbiddenException;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.exception.ValidationException;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.user.dto.UserDto;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
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

@WebMvcTest(BookingController.class)
class BookingControllerTest {
    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper mapper;

    @MockBean
    private BookingService bookingService;

    private final LocalDateTime start = LocalDateTime.now().plusDays(1).truncatedTo(ChronoUnit.SECONDS);
    private final LocalDateTime end = start.plusDays(1);

    @Test
    void create_shouldReturn201WithBooking() throws Exception {
        when(bookingService.create(eq(2L), any(NewBookingDto.class))).thenReturn(bookingDto(BookingStatus.WAITING));

        mvc.perform(post("/bookings")
                        .header(RequestHeaders.USER_ID, 2L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(new NewBookingDto(10L, start, end))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("WAITING"))
                .andExpect(jsonPath("$.booker.id").value(2))
                .andExpect(jsonPath("$.item.id").value(10))
                .andExpect(jsonPath("$.item.name").value("Дрель"));
    }

    @Test
    void create_withoutUserHeader_shouldReturn400() throws Exception {
        mvc.perform(post("/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(new NewBookingDto(10L, start, end))))
                .andExpect(status().isBadRequest());

        verify(bookingService, never()).create(anyLong(), any());
    }

    @Test
    void create_withStartInPast_shouldReturn400() throws Exception {
        NewBookingDto dto = new NewBookingDto(10L, LocalDateTime.now().minusDays(1), end);

        mvc.perform(post("/bookings")
                        .header(RequestHeaders.USER_ID, 2L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());

        verify(bookingService, never()).create(anyLong(), any());
    }

    @Test
    void create_withoutEnd_shouldReturn400() throws Exception {
        mvc.perform(post("/bookings")
                        .header(RequestHeaders.USER_ID, 2L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(new NewBookingDto(10L, start, null))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_whenServiceRejects_shouldReturn400() throws Exception {
        when(bookingService.create(eq(2L), any(NewBookingDto.class)))
                .thenThrow(new ValidationException("Вещь недоступна"));

        mvc.perform(post("/bookings")
                        .header(RequestHeaders.USER_ID, 2L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(new NewBookingDto(10L, start, end))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.description").value("Вещь недоступна"));
    }

    @Test
    void approve_shouldPassParamsToService() throws Exception {
        when(bookingService.approve(1L, 5L, true)).thenReturn(bookingDto(BookingStatus.APPROVED));

        mvc.perform(patch("/bookings/5")
                        .header(RequestHeaders.USER_ID, 1L)
                        .param("approved", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    void approve_byNotOwner_shouldReturn403() throws Exception {
        when(bookingService.approve(3L, 5L, true)).thenThrow(new ForbiddenException("Не владелец"));

        mvc.perform(patch("/bookings/5")
                        .header(RequestHeaders.USER_ID, 3L)
                        .param("approved", "true"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getById_shouldReturnBooking() throws Exception {
        when(bookingService.getById(2L, 1L)).thenReturn(bookingDto(BookingStatus.WAITING));

        mvc.perform(get("/bookings/1").header(RequestHeaders.USER_ID, 2L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void getById_unknownBooking_shouldReturn404() throws Exception {
        when(bookingService.getById(2L, 99L)).thenThrow(new NotFoundException("Не найдено"));

        mvc.perform(get("/bookings/99").header(RequestHeaders.USER_ID, 2L))
                .andExpect(status().isNotFound());
    }

    @Test
    void getByBooker_withoutState_shouldUseAll() throws Exception {
        when(bookingService.getByBooker(2L, BookingState.ALL)).thenReturn(List.of(bookingDto(BookingStatus.WAITING)));

        mvc.perform(get("/bookings").header(RequestHeaders.USER_ID, 2L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        verify(bookingService).getByBooker(2L, BookingState.ALL);
    }

    @Test
    void getByOwner_withState_shouldPassState() throws Exception {
        when(bookingService.getByOwner(1L, BookingState.FUTURE)).thenReturn(List.of());

        mvc.perform(get("/bookings/owner")
                        .header(RequestHeaders.USER_ID, 1L)
                        .param("state", "FUTURE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        verify(bookingService).getByOwner(1L, BookingState.FUTURE);
    }

    private BookingDto bookingDto(BookingStatus status) {
        return BookingDto.builder()
                .id(1L)
                .start(start)
                .end(end)
                .status(status)
                .booker(UserDto.builder().id(2L).name("Арендатор").email("booker@mail.ru").build())
                .item(ItemDto.builder().id(10L).name("Дрель").description("Ударная").available(true).build())
                .build();
    }
}
