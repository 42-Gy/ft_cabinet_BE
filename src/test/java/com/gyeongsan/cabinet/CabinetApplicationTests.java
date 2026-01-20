package com.gyeongsan.cabinet;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class CabinetApplicationTests {

	@org.springframework.boot.test.mock.mockito.MockBean
	private com.azure.storage.blob.BlobServiceClient blobServiceClient;

	@Test
	void contextLoads() {
	}

}
