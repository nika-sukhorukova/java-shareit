package ru.practicum.shareit.item;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.practicum.shareit.item.model.Item;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Component
public class InMemoryItemStorage implements ItemStorage {

    private final Map<Long, Item> items = new LinkedHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    @Override
    public Item create(Item item) {
        item.setId(idGenerator.getAndIncrement());
        items.put(item.getId(), item);
        log.debug("В хранилище добавлена вещь id={}", item.getId());
        return item;
    }

    @Override
    public Item update(Item item) {
        items.put(item.getId(), item);
        log.debug("В хранилище обновлена вещь id={}", item.getId());
        return item;
    }

    @Override
    public Optional<Item> findById(Long itemId) {
        return Optional.ofNullable(items.get(itemId));
    }

    @Override
    public List<Item> findByOwnerId(Long ownerId) {
        return items.values().stream()
                .filter(item -> item.getOwner().getId().equals(ownerId))
                .toList();
    }

    @Override
    public List<Item> search(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        String query = text.toLowerCase();
        return items.values().stream()
                .filter(item -> Boolean.TRUE.equals(item.getAvailable()))
                .filter(item -> containsIgnoreCase(item.getName(), query)
                        || containsIgnoreCase(item.getDescription(), query))
                .toList();
    }

    private boolean containsIgnoreCase(String source, String lowerCaseQuery) {
        return source != null && source.toLowerCase().contains(lowerCaseQuery);
    }
}
