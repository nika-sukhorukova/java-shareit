package ru.practicum.shareit.request;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.shareit.common.RequestHeaders;
import ru.practicum.shareit.request.dto.ItemRequestDto;
import ru.practicum.shareit.request.dto.ItemRequestWithItemsDto;

import java.util.List;

@RestController
@RequestMapping(path = "/requests")
@RequiredArgsConstructor
public class ItemRequestController {
    private final ItemRequestService requestService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ItemRequestDto create(@RequestHeader(RequestHeaders.USER_ID) Long userId,
                                 @RequestBody ItemRequestDto dto) {
        return requestService.create(userId, dto);
    }

    @GetMapping
    public List<ItemRequestWithItemsDto> getOwn(@RequestHeader(RequestHeaders.USER_ID) Long userId) {
        return requestService.getOwn(userId);
    }

    @GetMapping("/all")
    public List<ItemRequestWithItemsDto> getAll(@RequestHeader(RequestHeaders.USER_ID) Long userId) {
        return requestService.getAll(userId);
    }

    @GetMapping("/{requestId}")
    public ItemRequestWithItemsDto getById(@RequestHeader(RequestHeaders.USER_ID) Long userId,
                                           @PathVariable Long requestId) {
        return requestService.getById(userId, requestId);
    }
}
