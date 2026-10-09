package com.gyeongsan.cabinet.domain.alarm.model;

import java.util.List;

/**
 * 커서 이후의 채널 메시지 조회 결과.
 *
 * @param messagesOldestFirst 오래된 것부터 정렬된 최상위 메시지
 * @param truncated 안전 한도를 넘어 일부(가장 오래된 쪽)를 가져오지 못했는지
 */
public record SlackHistory(List<SlackChannelMessage> messagesOldestFirst, boolean truncated) {}
