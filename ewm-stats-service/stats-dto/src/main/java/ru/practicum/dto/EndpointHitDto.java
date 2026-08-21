package ru.practicum.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EndpointHitDto {

    private Long id;

    @NotBlank(message = "app must not be blank")
    private String app;

    @NotBlank(message = "uri must not be blank")
    private String uri;

    @NotBlank(message = "ip must not be blank")
    private String ip;

    @NotBlank(message = "timestamp must not be blank")
    @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}",
            message = "timestamp must match pattern yyyy-MM-dd HH:mm:ss")
    private String timestamp;
}
