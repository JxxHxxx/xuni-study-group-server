package com.xuni.api.review.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.hibernate.validator.constraints.Range;

public record ReviewUpdateForm(
        @NotNull(message = "null 일 수 없습니다")
        @Range(min = 0, max = 5, message = "0 - 5 사이의 점수를 매겨주세요")
        Integer rating,
        @NotBlank(message = "공백일 수 없습니다")
        @Pattern(regexp = "^.{10,500}$", message = "10자 이상 작성해주세요. 500자 까지")
        String comment
) {
}
