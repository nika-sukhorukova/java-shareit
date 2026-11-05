package ru.practicum.shareit.request;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.item.ItemRepository;
import ru.practicum.shareit.request.dto.ItemAnswerDto;
import ru.practicum.shareit.request.dto.ItemRequestDto;
import ru.practicum.shareit.request.dto.ItemRequestWithItemsDto;
import ru.practicum.shareit.user.User;
import ru.practicum.shareit.user.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ItemRequestServiceImpl implements ItemRequestService {
    private final ItemRequestRepository requestRepository;
    private final UserRepository userRepository;
    private final ItemRepository itemRepository;

    @Override
    @Transactional
    public ItemRequestDto create(Long userId, ItemRequestDto dto) {
        User requestor = findUserOrThrow(userId);
        ItemRequest request = new ItemRequest(null, dto.getDescription(), requestor, LocalDateTime.now());
        return ItemRequestMapper.toItemRequestDto(requestRepository.save(request));
    }

    @Override
    public List<ItemRequestWithItemsDto> getOwn(Long userId) {
        findUserOrThrow(userId);
        return withItems(requestRepository.findByRequestorIdOrderByCreatedDesc(userId));
    }

    @Override
    public List<ItemRequestWithItemsDto> getAll(Long userId) {
        findUserOrThrow(userId);
        return withItems(requestRepository.findByRequestorIdNotOrderByCreatedDesc(userId));
    }

    @Override
    public ItemRequestWithItemsDto getById(Long userId, Long requestId) {
        findUserOrThrow(userId);
        ItemRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Запрос с id=" + requestId + " не найден"));
        return withItems(List.of(request)).getFirst();
    }

    private List<ItemRequestWithItemsDto> withItems(List<ItemRequest> requests) {
        if (requests.isEmpty()) {
            return List.of();
        }
        List<Long> requestIds = requests.stream().map(ItemRequest::getId).toList();
        Map<Long, List<ItemAnswerDto>> itemsByRequest = itemRepository.findByRequestIdIn(requestIds).stream()
                .collect(Collectors.groupingBy(item -> item.getRequest().getId(),
                        Collectors.mapping(ItemRequestMapper::toItemAnswerDto, Collectors.toList())));

        return requests.stream()
                .map(request -> ItemRequestMapper.toItemRequestWithItemsDto(
                        request, itemsByRequest.getOrDefault(request.getId(), List.of())))
                .toList();
    }

    private User findUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id=" + userId + " не найден"));
    }
}
