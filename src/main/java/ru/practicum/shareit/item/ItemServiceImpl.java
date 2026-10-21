package ru.practicum.shareit.item;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.booking.Booking;
import ru.practicum.shareit.booking.BookingMapper;
import ru.practicum.shareit.booking.BookingRepository;
import ru.practicum.shareit.booking.BookingStatus;
import ru.practicum.shareit.booking.dto.BookingShortDto;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.exception.ValidationException;
import ru.practicum.shareit.item.dto.CommentDto;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.dto.ItemWithBookingsDto;
import ru.practicum.shareit.item.model.Comment;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.user.User;
import ru.practicum.shareit.user.UserRepository;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ItemServiceImpl implements ItemService {
    private final ItemRepository itemRepository;
    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final CommentRepository commentRepository;

    @Override
    @Transactional
    public ItemDto create(Long userId, ItemDto itemDto) {
        User owner = findUserOrThrow(userId);
        Item item = ItemMapper.toItem(itemDto);
        item.setOwner(owner);
        return ItemMapper.toItemDto(itemRepository.save(item));
    }

    @Override
    @Transactional
    public ItemDto update(Long userId, Long itemId, ItemDto itemDto) {
        Item existing = findItemOrThrow(itemId);

        // Редактировать может только владелец. Отвечаем 404, чтобы не раскрывать чужие вещи
        if (!existing.getOwner().getId().equals(userId)) {
            throw new NotFoundException("Вещь с id=" + itemId + " не найдена у пользователя с id=" + userId);
        }

        Item updated = Item.builder()
                .id(existing.getId())
                .name(itemDto.getName() != null ? itemDto.getName() : existing.getName())
                .description(itemDto.getDescription() != null ? itemDto.getDescription() : existing.getDescription())
                .available(itemDto.getAvailable() != null ? itemDto.getAvailable() : existing.getAvailable())
                .owner(existing.getOwner())
                .request(existing.getRequest())
                .build();

        return ItemMapper.toItemDto(itemRepository.save(updated));
    }

    @Override
    public ItemWithBookingsDto getById(Long userId, Long itemId) {
        Item item = findItemOrThrow(itemId);
        List<Long> itemIds = List.of(itemId);
        List<CommentDto> comments = findCommentsByItem(itemIds).getOrDefault(itemId, List.of());

        if (!item.getOwner().getId().equals(userId)) {
            return ItemMapper.toItemWithBookingsDto(item, null, null, comments);
        }
        List<Booking> bookings = findApprovedBookingsByItem(itemIds).getOrDefault(itemId, List.of());
        LocalDateTime now = LocalDateTime.now();
        return ItemMapper.toItemWithBookingsDto(item, findLast(bookings, now), findNext(bookings, now), comments);
    }

    @Override
    public List<ItemWithBookingsDto> getByOwner(Long userId) {
        List<Item> items = itemRepository.findByOwnerId(userId);
        if (items.isEmpty()) {
            return List.of();
        }
        List<Long> itemIds = items.stream().map(Item::getId).toList();

        Map<Long, List<Booking>> bookingsByItem = findApprovedBookingsByItem(itemIds);
        Map<Long, List<CommentDto>> commentsByItem = findCommentsByItem(itemIds);
        LocalDateTime now = LocalDateTime.now();

        return items.stream()
                .map(item -> {
                    List<Booking> bookings = bookingsByItem.getOrDefault(item.getId(), List.of());
                    return ItemMapper.toItemWithBookingsDto(
                            item,
                            findLast(bookings, now),
                            findNext(bookings, now),
                            commentsByItem.getOrDefault(item.getId(), List.of()));
                })
                .toList();
    }

    @Override
    public List<ItemDto> search(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        return itemRepository.search(text).stream()
                .map(ItemMapper::toItemDto)
                .toList();
    }

    @Override
    @Transactional
    public CommentDto addComment(Long userId, Long itemId, CommentDto dto) {
        User author = findUserOrThrow(userId);
        Item item = findItemOrThrow(itemId);
        LocalDateTime now = LocalDateTime.now();

        boolean hasRented = bookingRepository
                .existsByBookerIdAndItemIdAndStatusAndEndBefore(userId, itemId, BookingStatus.APPROVED, now);
        if (!hasRented) {
            throw new ValidationException("Пользователь с id=" + userId
                    + " не брал вещь с id=" + itemId + " в аренду или аренда ещё не закончилась");
        }

        Comment comment = Comment.builder()
                .text(dto.getText())
                .item(item)
                .author(author)
                .created(now)
                .build();
        return CommentMapper.toCommentDto(commentRepository.save(comment));
    }

    private Item findItemOrThrow(Long itemId) {
        return itemRepository.findById(itemId)
                .orElseThrow(() -> new NotFoundException("Вещь с id=" + itemId + " не найдена"));
    }

    private User findUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id=" + userId + " не найден"));
    }

    private Map<Long, List<Booking>> findApprovedBookingsByItem(List<Long> itemIds) {
        return bookingRepository.findByItemIdInAndStatus(itemIds, BookingStatus.APPROVED).stream()
                .collect(Collectors.groupingBy(booking -> booking.getItem().getId()));
    }

    private Map<Long, List<CommentDto>> findCommentsByItem(List<Long> itemIds) {
        return commentRepository.findByItemIdInOrderByCreatedDesc(itemIds).stream()
                .collect(Collectors.groupingBy(comment -> comment.getItem().getId(),
                        Collectors.mapping(CommentMapper::toCommentDto, Collectors.toList())));
    }

    // Последнее — самое позднее из уже начавшихся
    private BookingShortDto findLast(List<Booking> bookings, LocalDateTime now) {
        return bookings.stream()
                .filter(booking -> booking.getStart().isBefore(now))
                .max(Comparator.comparing(Booking::getStart))
                .map(BookingMapper::toBookingShortDto)
                .orElse(null);
    }

    private BookingShortDto findNext(List<Booking> bookings, LocalDateTime now) {
        return bookings.stream()
                .filter(booking -> booking.getStart().isAfter(now))
                .min(Comparator.comparing(Booking::getStart))
                .map(BookingMapper::toBookingShortDto)
                .orElse(null);
    }
}
