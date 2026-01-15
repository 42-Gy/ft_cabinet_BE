package com.gyeongsan.cabinet.alarm.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AlarmEvent {
    private final String intraId;
    private final String email;
    private final String message;
}