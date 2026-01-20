package com.gyeongsan.cabinet.lent.service;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Log4j2
public class ImageUploadService {

    private final BlobServiceClient blobServiceClient;

    @Value("${cloud.azure.storage.blob.container-name}")
    private String containerName;

    public String uploadImage(MultipartFile file) {
        if (file.isEmpty())
            return null;

        // 1. 파일명 중복 방지 (UUID)
        String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();

        try {
            // 2. 컨테이너 클라이언트 가져오기
            BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(containerName);

            // 3. Blob 클라이언트 생성 및 업로드
            BlobClient blobClient = containerClient.getBlobClient(fileName);
            blobClient.upload(file.getInputStream(), file.getSize(), true);

            // 4. 업로드된 이미지 URL 반환
            log.info("✅ 이미지 업로드 성공: {}", fileName);
            return blobClient.getBlobUrl();

        } catch (IOException e) {
            log.error("❌ 이미지 업로드 실패", e);
            throw new RuntimeException("이미지 업로드 중 오류가 발생했습니다.");
        }
    }
}
