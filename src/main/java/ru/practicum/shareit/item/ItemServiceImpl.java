package ru.practicum.shareit.item;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.user.User;
import ru.practicum.shareit.user.UserStorage;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ItemServiceImpl implements ItemService {
    private final ItemStorage itemStorage;
    private final UserStorage userStorage;

    @Override
    public ItemDto create(Long userId, ItemDto itemDto) {
        User owner = findUserOrThrow(userId);
        Item item = ItemMapper.toItem(itemDto);
        item.setOwner(owner);
        return ItemMapper.toItemDto(itemStorage.create(item));
    }

    @Override
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

        return ItemMapper.toItemDto(itemStorage.update(updated));
    }

    @Override
    public ItemDto getById(Long itemId) {
        return ItemMapper.toItemDto(findItemOrThrow(itemId));
    }

    @Override
    public List<ItemDto> getByOwner(Long userId) {
        return itemStorage.findByOwnerId(userId).stream()
                .map(ItemMapper::toItemDto)
                .toList();
    }

    @Override
    public List<ItemDto> search(String text) {
        return itemStorage.search(text).stream()
                .map(ItemMapper::toItemDto)
                .toList();
    }

    private Item findItemOrThrow(Long itemId) {
        return itemStorage.findById(itemId)
                .orElseThrow(() -> new NotFoundException("Вещь с id=" + itemId + " не найдена"));
    }

    private User findUserOrThrow(Long userId) {
        return userStorage.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id=" + userId + " не найден"));
    }
}
