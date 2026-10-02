package ru.practicum.shareit.user;

import java.util.Collection;
import java.util.Optional;

public interface UserStorage {

    Collection<User> findAll();

    Optional<User> findById(Long id);

    User create(User user);

    User update(User user);

    void delete(Long userId);

    boolean existsByEmail(String email);
}