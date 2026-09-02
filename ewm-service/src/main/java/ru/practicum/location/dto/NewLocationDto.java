package ru.practicum.location.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
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
public class NewLocationDto {

    @NotBlank
    private String name;

    @NotNull
    private Double lat;

    @NotNull
    private Double lon;

    @NotNull
    @Positive
    private Double radius;
}
