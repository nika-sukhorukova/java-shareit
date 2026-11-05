package ru.practicum.shareit.booking;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.shareit.booking.dto.BookingState;
import ru.practicum.shareit.booking.dto.NewBookingDto;
import ru.practicum.shareit.common.RequestHeaders;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

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
    private BookingClient bookingClient;

    private final LocalDateTime start = LocalDateTime.now().plusDays(1);
    private final LocalDateTime end = LocalDateTime.now().plusDays(2);

    @Test
    void create_shouldProxyToServer() throws Exception {
        when(bookingClient.create(eq(2L), any(NewBookingDto.class)))
                .thenReturn(ResponseEntity.status(201).body(Map.of("id", 1, "status", "WAITING")));

        mvc.perform(post("/bookings")
                        .header(RequestHeaders.USER_ID, 2L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(new NewBookingDto(10L, start, end))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("WAITING"));
    }

    @Test
    void create_withStartInPast_shouldReturn400() throws Exception {
        expectBadRequest(new NewBookingDto(10L, LocalDateTime.now().minusDays(1), end));
    }

    @Test
    void create_withEndInPast_shouldReturn400() throws Exception {
        expectBadRequest(new NewBookingDto(10L, LocalDateTime.now().minusDays(2), LocalDateTime.now().minusDays(1)));
    }

    @Test
    void create_withEndBeforeStart_shouldReturn400() throws Exception {
        expectBadRequest(new NewBookingDto(10L, end, start));
    }

    @Test
    void create_withStartEqualsEnd_shouldReturn400() throws Exception {
        expectBadRequest(new NewBookingDto(10L, start, start));
    }

    @Test
    void create_withoutStart_shouldReturn400() throws Exception {
        expectBadRequest(new NewBookingDto(10L, null, end));
    }

    @Test
    void create_withoutEnd_shouldReturn400() throws Exception {
        expectBadRequest(new NewBookingDto(10L, start, null));
    }

    @Test
    void approve_shouldProxyToServer() throws Exception {
        when(bookingClient.approve(1L, 5L, true)).thenReturn(ResponseEntity.ok(Map.of("status", "APPROVED")));

        mvc.perform(patch("/bookings/5").param("approved", "true").header(RequestHeaders.USER_ID, 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    void approve_withoutApprovedParam_shouldReturn400() throws Exception {
        mvc.perform(patch("/bookings/5").header(RequestHeaders.USER_ID, 1L))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getById_shouldProxyToServer() throws Exception {
        when(bookingClient.getById(1L, 5L)).thenReturn(ResponseEntity.ok(Map.of("id", 5)));

        mvc.perform(get("/bookings/5").header(RequestHeaders.USER_ID, 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5));
    }

    @Test
    void getByBooker_withDefaultState_shouldUseAll() throws Exception {
        when(bookingClient.getByBooker(2L, BookingState.ALL)).thenReturn(ResponseEntity.ok(List.of(Map.of("id", 5))));

        mvc.perform(get("/bookings").header(RequestHeaders.USER_ID, 2L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(5));
    }

    @Test
    void getByOwner_withLowerCaseState_shouldParseState() throws Exception {
        when(bookingClient.getByOwner(1L, BookingState.WAITING)).thenReturn(ResponseEntity.ok(List.of()));

        mvc.perform(get("/bookings/owner").param("state", "waiting").header(RequestHeaders.USER_ID, 1L))
                .andExpect(status().isOk());

        verify(bookingClient).getByOwner(1L, BookingState.WAITING);
    }

    @Test
    void getByBooker_withUnknownState_shouldReturn400() throws Exception {
        mvc.perform(get("/bookings").param("state", "UNSUPPORTED").header(RequestHeaders.USER_ID, 2L))
                .andExpect(status().isBadRequest());

        verify(bookingClient, never()).getByBooker(anyLong(), any());
    }

    private void expectBadRequest(NewBookingDto dto) throws Exception {
        mvc.perform(post("/bookings")
                        .header(RequestHeaders.USER_ID, 2L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());

        verify(bookingClient, never()).create(anyLong(), any());
    }
}
