package ru.practicum.shareit.booking;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.json.JsonContent;
import ru.practicum.shareit.booking.dto.NewBookingDto;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@JsonTest
class NewBookingDtoJsonTest {
    @Autowired
    private JacksonTester<NewBookingDto> json;

    @Test
    void serialize_shouldWriteIsoDatesWithoutValidationProperty() throws Exception {
        NewBookingDto dto = new NewBookingDto(3L,
                LocalDateTime.of(2026, 12, 1, 10, 0, 0), LocalDateTime.of(2026, 12, 2, 10, 0, 0));

        JsonContent<NewBookingDto> result = json.write(dto);

        assertThat(result).extractingJsonPathNumberValue("$.itemId").isEqualTo(3);
        assertThat(result).extractingJsonPathStringValue("$.start").isEqualTo("2026-12-01T10:00:00");
        assertThat(result).extractingJsonPathStringValue("$.end").isEqualTo("2026-12-02T10:00:00");
        assertThat(result).doesNotHaveJsonPath("$.startBeforeEnd");
    }

    @Test
    void deserialize_shouldParseIsoDates() throws Exception {
        NewBookingDto dto = json.parseObject(
                "{\"itemId\": 3, \"start\": \"2026-12-01T10:00:00\", \"end\": \"2026-12-02T10:00:00\"}");

        assertThat(dto.getItemId()).isEqualTo(3L);
        assertThat(dto.getStart()).isEqualTo(LocalDateTime.of(2026, 12, 1, 10, 0, 0));
        assertThat(dto.getEnd()).isEqualTo(LocalDateTime.of(2026, 12, 2, 10, 0, 0));
    }
}
