package ru.practicum.shareit.user;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.exception.ConflictException;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.user.dto.UserDto;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;

    @Override
    @Transactional
    public UserDto create(UserDto userDto) {
        if (userRepository.existsByEmail(userDto.getEmail())) {
            throw new ConflictException("Пользователь с email " + userDto.getEmail() + " уже существует");
        }
        User user = UserMapper.toUser(userDto);
        User created = userRepository.save(user);
        return UserMapper.toUserDto(created);
    }

    @Override
    @Transactional
    public UserDto update(Long userId, UserDto userDto) {
        //Найти пользователя, иначе 404
        User existing = findUserOrThrow(userId);

        // проверка имейла или возвращаем 409
        String newEmail = userDto.getEmail();
        if (newEmail != null
                && !newEmail.equals(existing.getEmail())
                && userRepository.existsByEmail(newEmail)) {
            throw new ConflictException("Пользователь с email " + newEmail + " уже существует");
        }

        User updated = User.builder()
                .id(existing.getId())
                .name(userDto.getName() != null ? userDto.getName() : existing.getName())
                .email(newEmail != null ? newEmail : existing.getEmail())
                .build();

        return UserMapper.toUserDto(userRepository.save(updated));
    }

    @Override
    public UserDto getById(Long userId) {
        return UserMapper.toUserDto(findUserOrThrow(userId));
    }

    @Override
    public List<UserDto> getAll() {
        return userRepository.findAll().stream()
                .map(UserMapper::toUserDto)
                .toList();
    }

    @Override
    @Transactional
    public void delete(Long userId) {
        userRepository.deleteById(userId);
    }


    private User findUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id=" + userId + " не найден"));
    }
}
