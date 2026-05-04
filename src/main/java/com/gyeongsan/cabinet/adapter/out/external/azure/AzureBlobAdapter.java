package com.gyeongsan.cabinet.adapter.out.external.azure;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.gyeongsan.cabinet.domain.lent.port.out.ImageUploadPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Log4j2
public class AzureBlobAdapter implements ImageUploadPort {

    private final BlobServiceClient blobServiceClient;

    @Value("${cloud.azure.storage.blob.container-name}")
    private String containerName;

    @Override
    public String uploadImage(Long userId, MultipartFile file) {
        if (file.isEmpty()) return null;

        String contentType = file.getContentType();
        if (contentType == null || (!contentType.equals("image/jpeg") && !contentType.equals("image/png"))) {
            throw new RuntimeException("이미지 파일만 업로드 가능합니다. (jpg, png)");
        }

        String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();

        try {
            BufferedImage image = ImageIO.read(file.getInputStream());
            if (image == null) {
                throw new RuntimeException("유효하지 않은 이미지 파일입니다.");
            }

            String format = contentType.equals("image/png") ? "png" : "jpg";
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            ImageIO.write(image, format, os);
            ByteArrayInputStream is = new ByteArrayInputStream(os.toByteArray());

            BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(containerName);
            BlobClient blobClient = containerClient.getBlobClient(fileName);

            blobClient.upload(is, os.size(), true);

            log.info("이미지 업로드 성공 - User: {}, File: {}", userId, fileName);
            return blobClient.getBlobUrl();

        } catch (IOException e) {
            log.error("이미지 업로드 실패 - User: {}, File: {}", userId, fileName, e);
            throw new RuntimeException("이미지 업로드 중 오류가 발생했습니다.");
        }
    }
}
