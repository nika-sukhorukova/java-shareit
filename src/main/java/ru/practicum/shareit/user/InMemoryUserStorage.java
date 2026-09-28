package ru.practicum.shareit.user;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Component
public class InMemoryUserStorage implements UserStorage {

    private final Map<Long, User> users = new LinkedHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);
    private final Set<String> emails = new HashSet<>();

    @Override
    public Collection<User> findAll() {
        return new ArrayList<>(users.values());
    }

    @Override
    public Optional<User> findById(Long id) {
        return Optional.ofNullable(users.get(id));
    }

    @Override
    public User create(User user) {
        user.setId(idGenerator.getAndIncrement());
        users.put(user.getId(), user);
        emails.add(user.getEmail());                              // адрес стал занят
        log.debug("В хранилище добавлен пользователь id={}", user.getId());
        return user;
    }


    @Override
    public User update(User user) {
        User old = users.get(user.getId());                       //  какой был до изменения
        emails.remove(old.getEmail());                            //  старый адрес освободился
        emails.add(user.getEmail());                              //  новый адрес занят
        users.put(user.getId(), user);
        log.debug("В хранилище обновлён пользователь id={}", user.getId());
        return user;
    }

    @Override
    public void delete(Long userId) {
        User removed = users.remove(userId);                      // remove возвращает удалённого (или null)
        if (removed != null) {
            emails.remove(removed.getEmail());                    // его адрес освободился
        }
        log.debug("В хранилище удалён пользователь id={}", userId);
    }

    @Override
    public boolean existsByEmail(String email) {
        return emails.contains(email);
    }
}
