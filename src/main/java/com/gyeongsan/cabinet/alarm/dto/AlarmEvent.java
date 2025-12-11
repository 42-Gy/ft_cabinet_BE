package com.gyeongsan.cabinet.alarm.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AlarmEvent {
    private final String email;   // 받을 사람
    private final String message; // 내용
}