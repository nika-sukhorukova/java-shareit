package ru.practicum.shareit.item;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

import ru.practicum.shareit.booking.BookingMapper;
import ru.practicum.shareit.booking.BookingRepository;
import ru.practicum.shareit.booking.BookingStatus;

import java.time.LocalDateTime;
import java.util.List;

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
        BookingShortDto last = null;
        BookingShortDto next = null;

        if (item.getOwner().getId().equals(userId)) {
            LocalDateTime now = LocalDateTime.now();
            last = findLastBooking(itemId, now);
            next = findNextBooking(itemId, now);
        }
        return ItemMapper.toItemWithBookingsDto(item, last, next, findComments(itemId));
    }


    @Override
    public List<ItemWithBookingsDto> getByOwner(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        return itemRepository.findByOwnerId(userId).stream()
                .map(item -> ItemMapper.toItemWithBookingsDto(
                        item,
                        findLastBooking(item.getId(), now),
                        findNextBooking(item.getId(), now),
                        findComments(item.getId())
                )).toList();
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

    private BookingShortDto findLastBooking(Long itemId, LocalDateTime now) {
        return bookingRepository
                .findFirstByItemIdAndStatusAndStartBeforeOrderByStartDesc(itemId, BookingStatus.APPROVED, now)
                .map(BookingMapper::toBookingShortDto)
                .orElse(null);
    }

    private BookingShortDto findNextBooking(Long itemId, LocalDateTime now) {
        return bookingRepository
                .findFirstByItemIdAndStatusAndStartAfterOrderByStartAsc(itemId, BookingStatus.APPROVED, now)
                .map(BookingMapper::toBookingShortDto)
                .orElse(null);
    }

    private List<CommentDto> findComments(Long itemId) {
        return commentRepository.findByItemIdOrderByCreatedDesc(itemId).stream()
                .map(CommentMapper::toCommentDto)
                .toList();
    }
}
