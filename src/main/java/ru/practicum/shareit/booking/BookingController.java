package ru.practicum.shareit.booking;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.shareit.booking.dto.BookingDto;
import ru.practicum.shareit.booking.dto.NewBookingDto;
import ru.practicum.shareit.common.RequestHeaders;

import java.util.List;

@RestController
@RequestMapping(path = "/bookings")
@RequiredArgsConstructor
public class BookingController {
    private final BookingService bookingService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BookingDto create(@RequestHeader(RequestHeaders.USER_ID) Long userId,
                             @Valid @RequestBody NewBookingDto dto) {
        return bookingService.create(userId, dto);
    }

    @GetMapping
    public List<BookingDto> getByBooker(@RequestHeader(RequestHeaders.USER_ID) Long userId,
                                        @RequestParam(defaultValue = "ALL") BookingState state) {
        return bookingService.getByBooker(userId, state);
    }

    @PatchMapping("/{bookingId}")
    public BookingDto approve(@RequestHeader(RequestHeaders.USER_ID) Long ownerId,
                              @PathVariable Long bookingId,
                              @RequestParam boolean approved) {
        return bookingService.approve(ownerId, bookingId, approved);
    }

    @GetMapping("/{bookingId}")
    public BookingDto getById(@RequestHeader(RequestHeaders.USER_ID) Long userId,
                              @PathVariable Long bookingId) {
        return bookingService.getById(userId, bookingId);
    }

    @GetMapping("/owner")
    public List<BookingDto> getByOwner(@RequestHeader(RequestHeaders.USER_ID) Long ownerId,
                                       @RequestParam(defaultValue = "ALL") BookingState state) {
        return bookingService.getByOwner(ownerId, state);
    }
}