package ru.practicum.shareit.user;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.exception.ConflictException;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.user.dto.UserDto;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
@RequiredArgsConstructor(onConstructor_ = @Autowired)
class UserServiceImplTest {
    private final UserService userService;

    @Test
    void create_shouldSaveUserAndAssignId() {
        UserDto created = userService.create(user("Анна", "anna@mail.ru"));

        assertThat(created.getId()).isNotNull();
        assertThat(userService.getById(created.getId()).getEmail()).isEqualTo("anna@mail.ru");
    }

    @Test
    void create_withExistingEmail_shouldThrowConflict() {
        userService.create(user("Анна", "anna@mail.ru"));

        assertThatThrownBy(() -> userService.create(user("Другая Анна", "anna@mail.ru")))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void update_onlyName_shouldKeepEmail() {
        UserDto created = userService.create(user("Анна", "anna@mail.ru"));

        UserDto updated = userService.update(created.getId(), user("Мария", null));

        assertThat(updated.getName()).isEqualTo("Мария");
        assertThat(updated.getEmail()).isEqualTo("anna@mail.ru");
    }

    @Test
    void update_toEmailOfAnotherUser_shouldThrowConflict() {
        userService.create(user("Анна", "anna@mail.ru"));
        UserDto second = userService.create(user("Мария", "maria@mail.ru"));

        assertThatThrownBy(() -> userService.update(second.getId(), user(null, "anna@mail.ru")))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void update_toOwnEmail_shouldNotThrow() {
        UserDto created = userService.create(user("Анна", "anna@mail.ru"));

        UserDto updated = userService.update(created.getId(), user(null, "anna@mail.ru"));

        assertThat(updated.getEmail()).isEqualTo("anna@mail.ru");
    }

    @Test
    void getById_unknownUser_shouldThrowNotFound() {
        assertThatThrownBy(() -> userService.getById(999_999L))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void getAll_shouldReturnAllUsers() {
        userService.create(user("Анна", "anna@mail.ru"));
        userService.create(user("Мария", "maria@mail.ru"));

        List<UserDto> all = userService.getAll();

        assertThat(all).extracting(UserDto::getEmail).contains("anna@mail.ru", "maria@mail.ru");
    }

    @Test
    void delete_shouldRemoveUser() {
        UserDto created = userService.create(user("Анна", "anna@mail.ru"));

        userService.delete(created.getId());

        assertThatThrownBy(() -> userService.getById(created.getId()))
                .isInstanceOf(NotFoundException.class);
    }

    private UserDto user(String name, String email) {
        return UserDto.builder().name(name).email(email).build();
    }
}
