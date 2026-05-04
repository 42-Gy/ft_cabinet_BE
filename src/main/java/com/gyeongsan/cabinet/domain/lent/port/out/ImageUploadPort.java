package com.gyeongsan.cabinet.domain.lent.port.out;

import org.springframework.web.multipart.MultipartFile;

public interface ImageUploadPort {

    String uploadImage(Long userId, MultipartFile file);
}
