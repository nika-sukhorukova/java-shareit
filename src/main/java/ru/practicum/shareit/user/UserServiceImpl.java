package ru.practicum.shareit.user;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.practicum.shareit.exception.ConflictException;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.user.dto.UserDto;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserStorage userStorage;

    @Override
    public UserDto create(UserDto userDto) {
        if (userStorage.existsByEmail(userDto.getEmail())) {
            throw new ConflictException("Пользователь с email " + userDto.getEmail() + " уже существует");
        }
        User user = UserMapper.toUser(userDto);
        User created = userStorage.create(user);
        return UserMapper.toUserDto(created);
    }

    @Override
    public UserDto update(Long userId, UserDto userDto) {
        //Найти пользователя, иначе 404
        User existing = findUserOrThrow(userId);

        // проверка имейла или возвращаем 409
        String newEmail = userDto.getEmail();
        if (newEmail != null
                && !newEmail.equals(existing.getEmail())
                && userStorage.existsByEmail(newEmail)) {
            throw new ConflictException("Пользователь с email " + newEmail + " уже существует");
        }

        User updated = User.builder()
                .id(existing.getId())
                .name(userDto.getName() != null ? userDto.getName() : existing.getName())
                .email(newEmail != null ? newEmail : existing.getEmail())
                .build();

        return UserMapper.toUserDto(userStorage.update(updated));
    }

    @Override
    public UserDto getById(Long userId) {
        return UserMapper.toUserDto(findUserOrThrow(userId));
    }

    @Override
    public List<UserDto> getAll() {
        return userStorage.findAll().stream()
                .map(UserMapper::toUserDto)
                .toList();
    }

    @Override
    public void delete(Long userId) {
        userStorage.delete(userId);
    }

    private User findUserOrThrow(Long userId) {
        return userStorage.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id=" + userId + " не найден"));
    }
}
