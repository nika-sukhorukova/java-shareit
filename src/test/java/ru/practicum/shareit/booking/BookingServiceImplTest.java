package ru.practicum.shareit.booking;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.booking.dto.BookingDto;
import ru.practicum.shareit.booking.dto.NewBookingDto;
import ru.practicum.shareit.exception.ForbiddenException;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.exception.ValidationException;
import ru.practicum.shareit.item.ItemService;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.user.UserService;
import ru.practicum.shareit.user.dto.UserDto;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
@RequiredArgsConstructor(onConstructor_ = @Autowired)
class BookingServiceImplTest {
    private final BookingService bookingService;
    private final ItemService itemService;
    private final UserService userService;

    private Long ownerId;
    private Long bookerId;
    private Long strangerId;
    private Long itemId;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        ownerId = createUser("Владелец", "owner@mail.ru");
        bookerId = createUser("Арендатор", "booker@mail.ru");
        strangerId = createUser("Посторонний", "stranger@mail.ru");
        itemId = createItem(true);
        now = LocalDateTime.now();
    }

    @Test
    void create_shouldSaveBookingWithWaitingStatus() {
        BookingDto booking = bookingService.create(bookerId, booking(now.plusDays(1), now.plusDays(2)));

        assertThat(booking.getId()).isNotNull();
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.WAITING);
        assertThat(booking.getBooker().getId()).isEqualTo(bookerId);
        assertThat(booking.getItem().getId()).isEqualTo(itemId);
    }

    @Test
    void create_unavailableItem_shouldThrowValidation() {
        Long unavailableItemId = createItem(false);
        NewBookingDto dto = new NewBookingDto(unavailableItemId, now.plusDays(1), now.plusDays(2));

        assertThatThrownBy(() -> bookingService.create(bookerId, dto))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void create_startEqualsEnd_shouldThrowValidation() {
        LocalDateTime moment = now.plusDays(1);

        assertThatThrownBy(() -> bookingService.create(bookerId, booking(moment, moment)))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void create_unknownItem_shouldThrowNotFound() {
        NewBookingDto dto = new NewBookingDto(999_999L, now.plusDays(1), now.plusDays(2));

        assertThatThrownBy(() -> bookingService.create(bookerId, dto))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void create_unknownUser_shouldThrowNotFound() {
        assertThatThrownBy(() -> bookingService.create(999_999L, booking(now.plusDays(1), now.plusDays(2))))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void approve_byOwner_shouldSetApproved() {
        Long bookingId = createBooking(now.plusDays(1), now.plusDays(2));

        BookingDto approved = bookingService.approve(ownerId, bookingId, true);

        assertThat(approved.getStatus()).isEqualTo(BookingStatus.APPROVED);
    }

    @Test
    void approve_withFalse_shouldSetRejected() {
        Long bookingId = createBooking(now.plusDays(1), now.plusDays(2));

        BookingDto rejected = bookingService.approve(ownerId, bookingId, false);

        assertThat(rejected.getStatus()).isEqualTo(BookingStatus.REJECTED);
    }

    @Test
    void approve_byNotOwner_shouldThrowForbidden() {
        Long bookingId = createBooking(now.plusDays(1), now.plusDays(2));

        assertThatThrownBy(() -> bookingService.approve(bookerId, bookingId, true))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void approve_twice_shouldThrowValidation() {
        Long bookingId = createBooking(now.plusDays(1), now.plusDays(2));
        bookingService.approve(ownerId, bookingId, true);

        assertThatThrownBy(() -> bookingService.approve(ownerId, bookingId, false))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void getById_byBookerAndOwner_shouldReturnBooking() {
        Long bookingId = createBooking(now.plusDays(1), now.plusDays(2));

        assertThat(bookingService.getById(bookerId, bookingId).getId()).isEqualTo(bookingId);
        assertThat(bookingService.getById(ownerId, bookingId).getId()).isEqualTo(bookingId);
    }

    @Test
    void getById_byStranger_shouldThrowForbidden() {
        Long bookingId = createBooking(now.plusDays(1), now.plusDays(2));

        assertThatThrownBy(() -> bookingService.getById(strangerId, bookingId))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void getById_unknownBooking_shouldThrowNotFound() {
        assertThatThrownBy(() -> bookingService.getById(bookerId, 999_999L))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void getByBooker_shouldFilterByState() {
        Long past = createBooking(now.minusDays(3), now.minusDays(2));
        Long current = createBooking(now.minusDays(1), now.plusDays(1));
        Long future = createBooking(now.plusDays(2), now.plusDays(3));
        Long rejected = createBooking(now.plusDays(4), now.plusDays(5));
        bookingService.approve(ownerId, rejected, false);

        assertThat(bookingService.getByBooker(bookerId, BookingState.ALL))
                .extracting(BookingDto::getId)
                .containsExactly(rejected, future, current, past);
        assertThat(bookingService.getByBooker(bookerId, BookingState.PAST))
                .extracting(BookingDto::getId).containsExactly(past);
        assertThat(bookingService.getByBooker(bookerId, BookingState.CURRENT))
                .extracting(BookingDto::getId).containsExactly(current);
        assertThat(bookingService.getByBooker(bookerId, BookingState.FUTURE))
                .extracting(BookingDto::getId).containsExactly(rejected, future);
        assertThat(bookingService.getByBooker(bookerId, BookingState.WAITING))
                .extracting(BookingDto::getId).containsExactly(future, current, past);
        assertThat(bookingService.getByBooker(bookerId, BookingState.REJECTED))
                .extracting(BookingDto::getId).containsExactly(rejected);
    }

    @Test
    void getByOwner_shouldFilterByState() {
        Long past = createBooking(now.minusDays(3), now.minusDays(2));
        Long current = createBooking(now.minusDays(1), now.plusDays(1));
        Long future = createBooking(now.plusDays(2), now.plusDays(3));
        Long rejected = createBooking(now.plusDays(4), now.plusDays(5));
        bookingService.approve(ownerId, rejected, false);

        assertThat(bookingService.getByOwner(ownerId, BookingState.ALL))
                .extracting(BookingDto::getId)
                .containsExactly(rejected, future, current, past);
        assertThat(bookingService.getByOwner(ownerId, BookingState.PAST))
                .extracting(BookingDto::getId).containsExactly(past);
        assertThat(bookingService.getByOwner(ownerId, BookingState.CURRENT))
                .extracting(BookingDto::getId).containsExactly(current);
        assertThat(bookingService.getByOwner(ownerId, BookingState.FUTURE))
                .extracting(BookingDto::getId).containsExactly(rejected, future);
        assertThat(bookingService.getByOwner(ownerId, BookingState.WAITING))
                .extracting(BookingDto::getId).containsExactly(future, current, past);
        assertThat(bookingService.getByOwner(ownerId, BookingState.REJECTED))
                .extracting(BookingDto::getId).containsExactly(rejected);
    }

    @Test
    void getByOwner_forUserWithoutItems_shouldReturnEmptyList() {
        createBooking(now.plusDays(1), now.plusDays(2));

        assertThat(bookingService.getByOwner(bookerId, BookingState.ALL)).isEmpty();
    }

    @Test
    void getByOwner_unknownUser_shouldThrowNotFound() {
        assertThatThrownBy(() -> bookingService.getByOwner(999_999L, BookingState.ALL))
                .isInstanceOf(NotFoundException.class);
    }

    private Long createUser(String name, String email) {
        return userService.create(UserDto.builder().name(name).email(email).build()).getId();
    }

    private Long createItem(boolean available) {
        ItemDto item = ItemDto.builder().name("Дрель").description("Ударная дрель").available(available).build();
        return itemService.create(ownerId, item).getId();
    }

    private NewBookingDto booking(LocalDateTime start, LocalDateTime end) {
        return new NewBookingDto(itemId, start, end);
    }

    private Long createBooking(LocalDateTime start, LocalDateTime end) {
        return bookingService.create(bookerId, booking(start, end)).getId();
    }
}
