package com.gyeongsan.cabinet;

import jakarta.annotation.PostConstruct; // 👈 import 확인
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

@EnableAsync
@EnableScheduling
@SpringBootApplication
public class CabinetApplication {

	public static void main(String[] args) {
		SpringApplication.run(CabinetApplication.class, args);
	}

	// 👇 [추가] 서버 실행 시 서울 시간(KST)으로 기본 시간대 설정
	@PostConstruct
	public void started() {
		TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));
		System.out.println("✅ 현재 시간대: " + TimeZone.getDefault().getID() + " (KST 적용 완료)");
	}
}