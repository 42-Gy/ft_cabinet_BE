package com.gyeongsan.cabinet.lent.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record LentReturnRequest(
        @NotNull(message = "비밀번호는 필수입니다.")
        @Size(min = 4, max = 4, message = "공유 비밀번호는 4자리여야 합니다.")
        @Pattern(regexp = "\\d{4}", message = "공유 비밀번호는 숫자만 가능합니다.")
        String shareCode
) {}