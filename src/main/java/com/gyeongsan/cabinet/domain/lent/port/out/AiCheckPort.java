package com.gyeongsan.cabinet.domain.lent.port.out;

import org.springframework.web.multipart.MultipartFile;

public interface AiCheckPort {

    boolean checkItem(MultipartFile file);
}
