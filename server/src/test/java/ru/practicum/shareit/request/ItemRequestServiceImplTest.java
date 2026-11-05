package ru.practicum.shareit.request;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.item.ItemService;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.request.dto.ItemRequestDto;
import ru.practicum.shareit.request.dto.ItemRequestWithItemsDto;
import ru.practicum.shareit.user.UserService;
import ru.practicum.shareit.user.dto.UserDto;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
@RequiredArgsConstructor(onConstructor_ = @Autowired)
class ItemRequestServiceImplTest {
    private final ItemRequestService requestService;
    private final ItemService itemService;
    private final UserService userService;

    private Long requestorId;
    private Long ownerId;

    @BeforeEach
    void setUp() {
        requestorId = userService.create(UserDto.builder().name("Автор").email("requestor@mail.ru").build()).getId();
        ownerId = userService.create(UserDto.builder().name("Владелец").email("owner@mail.ru").build()).getId();
    }

    @Test
    void create_shouldSaveRequestWithCreatedDate() {
        ItemRequestDto created = requestService.create(requestorId, request("Нужна дрель"));

        assertThat(created.getId()).isNotNull();
        assertThat(created.getDescription()).isEqualTo("Нужна дрель");
        assertThat(created.getCreated()).isNotNull();
    }

    @Test
    void create_withUnknownUser_shouldThrowNotFound() {
        assertThatThrownBy(() -> requestService.create(999_999L, request("Нужна дрель")))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void getOwn_shouldReturnOwnRequestsNewestFirstWithItems() {
        Long first = requestService.create(requestorId, request("Нужна дрель")).getId();
        Long second = requestService.create(requestorId, request("Нужна пила")).getId();
        requestService.create(ownerId, request("Чужой запрос"));
        Long itemId = itemService.create(ownerId, ItemDto.builder()
                .name("Дрель").description("Ударная").available(true).requestId(first).build()).getId();

        List<ItemRequestWithItemsDto> result = requestService.getOwn(requestorId);

        assertThat(result).extracting(ItemRequestWithItemsDto::getId).containsExactly(second, first);
        assertThat(result.get(0).getItems()).isEmpty();
        assertThat(result.get(1).getItems()).hasSize(1);
        assertThat(result.get(1).getItems().get(0).getId()).isEqualTo(itemId);
        assertThat(result.get(1).getItems().get(0).getName()).isEqualTo("Дрель");
        assertThat(result.get(1).getItems().get(0).getOwnerId()).isEqualTo(ownerId);
    }

    @Test
    void getOwn_withoutRequests_shouldReturnEmptyList() {
        assertThat(requestService.getOwn(requestorId)).isEmpty();
    }

    @Test
    void getOwn_withUnknownUser_shouldThrowNotFound() {
        assertThatThrownBy(() -> requestService.getOwn(999_999L))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void getAll_shouldReturnOnlyOtherUsersRequests() {
        requestService.create(requestorId, request("Свой запрос"));
        Long foreign = requestService.create(ownerId, request("Чужой запрос")).getId();

        List<ItemRequestWithItemsDto> result = requestService.getAll(requestorId);

        assertThat(result).extracting(ItemRequestWithItemsDto::getId).containsExactly(foreign);
    }

    @Test
    void getById_shouldBeAvailableForAnyUser() {
        Long requestId = requestService.create(requestorId, request("Нужна дрель")).getId();
        itemService.create(ownerId, ItemDto.builder()
                .name("Дрель").description("Ударная").available(true).requestId(requestId).build());

        ItemRequestWithItemsDto result = requestService.getById(ownerId, requestId);

        assertThat(result.getDescription()).isEqualTo("Нужна дрель");
        assertThat(result.getItems()).hasSize(1);
    }

    @Test
    void getById_whenNotFound_shouldThrowNotFound() {
        assertThatThrownBy(() -> requestService.getById(requestorId, 999_999L))
                .isInstanceOf(NotFoundException.class);
    }

    private ItemRequestDto request(String description) {
        return ItemRequestDto.builder().description(description).build();
    }
}
