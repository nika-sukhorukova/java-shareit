package ru.practicum.shareit.booking;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.booking.dto.BookingDto;
import ru.practicum.shareit.booking.dto.NewBookingDto;
import ru.practicum.shareit.exception.ForbiddenException;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.exception.ValidationException;
import ru.practicum.shareit.item.ItemRepository;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.user.User;
import ru.practicum.shareit.user.UserRepository;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookingServiceImpl implements BookingService {
    private final BookingRepository bookingRepository;
    private final ItemRepository itemRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public BookingDto create(Long userId, NewBookingDto dto) {
        User booker = findUserOrThrow(userId);
        Item item = findItemOrThrow(dto.getItemId());

        if (!item.getAvailable()) {
            throw new ValidationException("Вещь с id=" + item.getId() + " недоступна для бронирования");
        }
        if (!dto.getStart().isBefore(dto.getEnd())) {
            throw new ValidationException("Дата начала бронирования должна быть раньше даты окончания");
        }
        // Клиент присылает даты с точностью до секунды, поэтому и "сейчас" сравниваем без миллисекунд
        if (dto.getStart().isBefore(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS))) {
            throw new ValidationException("Дата начала бронирования не может быть в прошлом");
        }

        Booking booking = BookingMapper.toBooking(dto, item, booker);
        return BookingMapper.toBookingDto(bookingRepository.save(booking));
    }

    @Override
    @Transactional
    public BookingDto approve(Long ownerId, Long bookingId, boolean approved) {
        Booking existing = findBookingOrThrow(bookingId);

        if (!existing.getItem().getOwner().getId().equals(ownerId)) {
            throw new ForbiddenException("Пользователь с id=" + ownerId + " не владелец");
        }

        if (!existing.getStatus().equals(BookingStatus.WAITING)) {
            throw new ValidationException("Бронирование с id=" + bookingId + " уже подтверждено или отклонено");
        }

        existing.setStatus(approved ? BookingStatus.APPROVED : BookingStatus.REJECTED);
        return BookingMapper.toBookingDto(bookingRepository.save(existing));
    }

    @Override
    public BookingDto getById(Long userId, Long bookingId) {
        Booking existing = findBookingOrThrow(bookingId);

        boolean isBooker = existing.getBooker().getId().equals(userId);
        boolean isOwner = existing.getItem().getOwner().getId().equals(userId);

        if (!isBooker && !isOwner) {
            throw new ForbiddenException("У вас нет прав для просмотра бронирования с id=" + bookingId);
        }

        return BookingMapper.toBookingDto(existing);
    }

    @Override
    public List<BookingDto> getByBooker(Long bookerId, BookingState state) {
        findUserOrThrow(bookerId);
        LocalDateTime now = LocalDateTime.now();

        List<Booking> bookings = switch (state) {
            case ALL -> bookingRepository.findByBookerIdOrderByStartDesc(bookerId);
            case CURRENT -> bookingRepository
                    .findByBookerIdAndStartBeforeAndEndAfterOrderByStartDesc(bookerId, now, now);
            case PAST -> bookingRepository.findByBookerIdAndEndBeforeOrderByStartDesc(bookerId, now);
            case FUTURE -> bookingRepository.findByBookerIdAndStartAfterOrderByStartDesc(bookerId, now);
            case WAITING -> bookingRepository
                    .findByBookerIdAndStatusOrderByStartDesc(bookerId, BookingStatus.WAITING);
            case REJECTED -> bookingRepository
                    .findByBookerIdAndStatusOrderByStartDesc(bookerId, BookingStatus.REJECTED);
        };

        return bookings.stream()
                .map(BookingMapper::toBookingDto)
                .toList();
    }

    @Override
    public List<BookingDto> getByOwner(Long ownerId, BookingState state) {
        findUserOrThrow(ownerId);
        LocalDateTime now = LocalDateTime.now();

        List<Booking> bookings = switch (state) {
            case ALL -> bookingRepository.findByItem_Owner_IdOrderByStartDesc(ownerId);
            case CURRENT -> bookingRepository
                    .findByItem_Owner_IdAndStartBeforeAndEndAfterOrderByStartDesc(ownerId, now, now);
            case PAST -> bookingRepository.findByItem_Owner_IdAndEndBeforeOrderByStartDesc(ownerId, now);
            case FUTURE -> bookingRepository.findByItem_Owner_IdAndStartAfterOrderByStartDesc(ownerId, now);
            case WAITING -> bookingRepository
                    .findByItem_Owner_IdAndStatusOrderByStartDesc(ownerId, BookingStatus.WAITING);
            case REJECTED -> bookingRepository
                    .findByItem_Owner_IdAndStatusOrderByStartDesc(ownerId, BookingStatus.REJECTED);
        };

        return bookings.stream()
                .map(BookingMapper::toBookingDto)
                .toList();
    }

    private Booking findBookingOrThrow(Long bookingId) {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NotFoundException("Бронирование с id=" + bookingId + " не найдено"));
    }

    private Item findItemOrThrow(Long itemId) {
        return itemRepository.findById(itemId)
                .orElseThrow(() -> new NotFoundException("Вещь с id=" + itemId + " не найдена"));
    }

    private User findUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id=" + userId + " не найден"));
    }

}