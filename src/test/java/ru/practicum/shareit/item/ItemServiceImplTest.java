package ru.practicum.shareit.item;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.booking.Booking;
import ru.practicum.shareit.booking.BookingRepository;
import ru.practicum.shareit.booking.BookingStatus;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.exception.ValidationException;
import ru.practicum.shareit.item.dto.CommentDto;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.dto.ItemWithBookingsDto;
import ru.practicum.shareit.user.UserRepository;
import ru.practicum.shareit.user.UserService;
import ru.practicum.shareit.user.dto.UserDto;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
@RequiredArgsConstructor(onConstructor_ = @Autowired)
class ItemServiceImplTest {
    private final ItemService itemService;
    private final UserService userService;
    private final BookingRepository bookingRepository;
    private final ItemRepository itemRepository;
    private final UserRepository userRepository;

    private Long ownerId;
    private Long bookerId;

    @BeforeEach
    void setUp() {
        ownerId = userService.create(UserDto.builder().name("Владелец").email("owner@mail.ru").build()).getId();
        bookerId = userService.create(UserDto.builder().name("Арендатор").email("booker@mail.ru").build()).getId();
    }

    @Test
    void create_shouldSaveItem() {
        ItemDto created = itemService.create(ownerId, item("Дрель", "Ударная дрель", true));

        assertThat(created.getId()).isNotNull();
        assertThat(created.getName()).isEqualTo("Дрель");
    }

    @Test
    void create_withUnknownOwner_shouldThrowNotFound() {
        assertThatThrownBy(() -> itemService.create(999_999L, item("Дрель", "Ударная дрель", true)))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void update_onlyAvailable_shouldKeepOtherFields() {
        ItemDto created = itemService.create(ownerId, item("Дрель", "Ударная дрель", true));

        ItemDto updated = itemService.update(ownerId, created.getId(), ItemDto.builder().available(false).build());

        assertThat(updated.getAvailable()).isFalse();
        assertThat(updated.getName()).isEqualTo("Дрель");
        assertThat(updated.getDescription()).isEqualTo("Ударная дрель");
    }

    @Test
    void update_byNotOwner_shouldThrowNotFound() {
        ItemDto created = itemService.create(ownerId, item("Дрель", "Ударная дрель", true));

        assertThatThrownBy(() -> itemService.update(bookerId, created.getId(), item("Пила", null, null)))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void getById_unknownItem_shouldThrowNotFound() {
        assertThatThrownBy(() -> itemService.getById(ownerId, 999_999L))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void search_shouldFindOnlyAvailableItemsIgnoringCase() {
        itemService.create(ownerId, item("Дрель", "Ударная ДРЕЛЬ", true));
        itemService.create(ownerId, item("Старая дрель", "Сломана", false));

        List<ItemDto> found = itemService.search("дРеЛь");

        assertThat(found).extracting(ItemDto::getName).containsExactly("Дрель");
    }

    @Test
    void search_withBlankText_shouldReturnEmptyList() {
        itemService.create(ownerId, item("Дрель", "Ударная дрель", true));

        assertThat(itemService.search("  ")).isEmpty();
    }

    @Test
    void getById_byOwner_shouldContainLastAndNextBookings() {
        Long itemId = itemService.create(ownerId, item("Дрель", "Ударная дрель", true)).getId();
        LocalDateTime now = LocalDateTime.now();
        Booking past = approvedBooking(itemId, now.minusDays(3), now.minusDays(2));
        Booking future = approvedBooking(itemId, now.plusDays(2), now.plusDays(3));

        ItemWithBookingsDto dto = itemService.getById(ownerId, itemId);

        assertThat(dto.getLastBooking().getId()).isEqualTo(past.getId());
        assertThat(dto.getNextBooking().getId()).isEqualTo(future.getId());
    }

    @Test
    void getById_byNotOwner_shouldHideBookings() {
        Long itemId = itemService.create(ownerId, item("Дрель", "Ударная дрель", true)).getId();
        LocalDateTime now = LocalDateTime.now();
        approvedBooking(itemId, now.minusDays(3), now.minusDays(2));

        ItemWithBookingsDto dto = itemService.getById(bookerId, itemId);

        assertThat(dto.getLastBooking()).isNull();
        assertThat(dto.getNextBooking()).isNull();
    }

    @Test
    void getByOwner_shouldReturnOwnerItemsWithBookings() {
        Long itemId = itemService.create(ownerId, item("Дрель", "Ударная дрель", true)).getId();
        itemService.create(ownerId, item("Пила", "Ручная пила", true));
        LocalDateTime now = LocalDateTime.now();
        approvedBooking(itemId, now.plusDays(1), now.plusDays(2));

        List<ItemWithBookingsDto> items = itemService.getByOwner(ownerId);

        assertThat(items).hasSize(2);
        assertThat(items).filteredOn(i -> i.getId().equals(itemId))
                .singleElement()
                .satisfies(i -> assertThat(i.getNextBooking()).isNotNull());
    }

    @Test
    void addComment_afterFinishedBooking_shouldBeVisibleInItem() {
        Long itemId = itemService.create(ownerId, item("Дрель", "Ударная дрель", true)).getId();
        LocalDateTime now = LocalDateTime.now();
        approvedBooking(itemId, now.minusDays(3), now.minusDays(2));

        CommentDto comment = itemService.addComment(bookerId, itemId, CommentDto.builder().text("Отлично").build());

        assertThat(comment.getAuthorName()).isEqualTo("Арендатор");
        assertThat(comment.getCreated()).isNotNull();
        assertThat(itemService.getById(bookerId, itemId).getComments())
                .extracting(CommentDto::getText)
                .containsExactly("Отлично");
    }

    @Test
    void addComment_withoutBooking_shouldThrowValidation() {
        Long itemId = itemService.create(ownerId, item("Дрель", "Ударная дрель", true)).getId();

        assertThatThrownBy(() -> itemService.addComment(bookerId, itemId, CommentDto.builder().text("Ок").build()))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void addComment_beforeBookingEnds_shouldThrowValidation() {
        Long itemId = itemService.create(ownerId, item("Дрель", "Ударная дрель", true)).getId();
        LocalDateTime now = LocalDateTime.now();
        approvedBooking(itemId, now.minusDays(1), now.plusDays(1));

        assertThatThrownBy(() -> itemService.addComment(bookerId, itemId, CommentDto.builder().text("Ок").build()))
                .isInstanceOf(ValidationException.class);
    }

    private ItemDto item(String name, String description, Boolean available) {
        return ItemDto.builder().name(name).description(description).available(available).build();
    }

    // Сохраняем напрямую через репозиторий: сервис не даст создать бронирование в прошлом
    private Booking approvedBooking(Long itemId, LocalDateTime start, LocalDateTime end) {
        Booking booking = Booking.builder()
                .start(start)
                .end(end)
                .item(itemRepository.findById(itemId).orElseThrow())
                .booker(userRepository.findById(bookerId).orElseThrow())
                .status(BookingStatus.APPROVED)
                .build();
        return bookingRepository.save(booking);
    }
}
