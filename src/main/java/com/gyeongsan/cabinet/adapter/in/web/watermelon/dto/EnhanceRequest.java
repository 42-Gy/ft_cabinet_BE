package com.gyeongsan.cabinet.adapter.in.web.watermelon.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class EnhanceRequest {
    private boolean usePremium;
    private boolean useDangerous;
    private boolean useDropProj;
    private boolean useDestroyProj;
}
