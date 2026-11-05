package ru.practicum.shareit.booking;

import ru.practicum.shareit.booking.dto.BookingDto;
import ru.practicum.shareit.booking.dto.NewBookingDto;

import java.util.List;

public interface BookingService {
    BookingDto create(Long userId, NewBookingDto dto);

    BookingDto approve(Long ownerId, Long bookingId, boolean approved);

    BookingDto getById(Long userId, Long bookingId);

    List<BookingDto> getByBooker(Long bookerId, BookingState state);

    List<BookingDto> getByOwner(Long ownerId, BookingState state);
}