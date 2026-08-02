package com.gyeongsan.cabinet.adapter.in.web.admin.dto;

import java.time.LocalDate;

public record AttendanceStatResponse(LocalDate date, long count) {}
