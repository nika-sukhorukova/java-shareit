package ru.practicum.shareit.request;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.json.JsonContent;
import ru.practicum.shareit.request.dto.ItemAnswerDto;
import ru.practicum.shareit.request.dto.ItemRequestDto;
import ru.practicum.shareit.request.dto.ItemRequestWithItemsDto;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@JsonTest
class ItemRequestDtoJsonTest {
    @Autowired
    private JacksonTester<ItemRequestWithItemsDto> withItemsJson;

    @Autowired
    private JacksonTester<ItemRequestDto> requestJson;

    @Test
    void serialize_shouldWriteCreatedInIsoFormatAndItems() throws Exception {
        ItemRequestWithItemsDto dto = ItemRequestWithItemsDto.builder()
                .id(1L)
                .description("Нужна дрель")
                .created(LocalDateTime.of(2026, 11, 5, 12, 30, 15))
                .items(List.of(new ItemAnswerDto(10L, "Дрель", 2L)))
                .build();

        JsonContent<ItemRequestWithItemsDto> result = withItemsJson.write(dto);

        assertThat(result).extractingJsonPathNumberValue("$.id").isEqualTo(1);
        assertThat(result).extractingJsonPathStringValue("$.description").isEqualTo("Нужна дрель");
        assertThat(result).extractingJsonPathStringValue("$.created").isEqualTo("2026-11-05T12:30:15");
        assertThat(result).extractingJsonPathNumberValue("$.items[0].id").isEqualTo(10);
        assertThat(result).extractingJsonPathStringValue("$.items[0].name").isEqualTo("Дрель");
        assertThat(result).extractingJsonPathNumberValue("$.items[0].ownerId").isEqualTo(2);
    }

    @Test
    void deserialize_shouldReadDescription() throws Exception {
        ItemRequestDto dto = requestJson.parseObject("{\"description\": \"Нужна дрель\"}");

        assertThat(dto.getDescription()).isEqualTo("Нужна дрель");
        assertThat(dto.getId()).isNull();
    }
}
