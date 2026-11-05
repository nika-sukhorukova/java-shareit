package ru.practicum.shareit.booking;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.shareit.booking.dto.BookingState;
import ru.practicum.shareit.booking.dto.NewBookingDto;
import ru.practicum.shareit.common.RequestHeaders;

@Slf4j
@RestController
@RequestMapping(path = "/bookings")
@RequiredArgsConstructor
public class BookingController {
    private final BookingClient bookingClient;

    @PostMapping
    public ResponseEntity<Object> create(@RequestHeader(RequestHeaders.USER_ID) long userId,
                                         @Valid @RequestBody NewBookingDto dto) {
        log.info("Создание бронирования {}, userId={}", dto, userId);
        return bookingClient.create(userId, dto);
    }

    @PatchMapping("/{bookingId}")
    public ResponseEntity<Object> approve(@RequestHeader(RequestHeaders.USER_ID) long ownerId,
                                          @PathVariable long bookingId,
                                          @RequestParam boolean approved) {
        log.info("Подтверждение бронирования id={}: {}, ownerId={}", bookingId, approved, ownerId);
        return bookingClient.approve(ownerId, bookingId, approved);
    }

    @GetMapping("/{bookingId}")
    public ResponseEntity<Object> getById(@RequestHeader(RequestHeaders.USER_ID) long userId,
                                          @PathVariable long bookingId) {
        return bookingClient.getById(userId, bookingId);
    }

    @GetMapping
    public ResponseEntity<Object> getByBooker(@RequestHeader(RequestHeaders.USER_ID) long userId,
                                              @RequestParam(defaultValue = "all") String state) {
        return bookingClient.getByBooker(userId, parseState(state));
    }

    @GetMapping("/owner")
    public ResponseEntity<Object> getByOwner(@RequestHeader(RequestHeaders.USER_ID) long ownerId,
                                             @RequestParam(defaultValue = "all") String state) {
        return bookingClient.getByOwner(ownerId, parseState(state));
    }

    private BookingState parseState(String state) {
        return BookingState.from(state)
                .orElseThrow(() -> new IllegalArgumentException("Unknown state: " + state));
    }
}
