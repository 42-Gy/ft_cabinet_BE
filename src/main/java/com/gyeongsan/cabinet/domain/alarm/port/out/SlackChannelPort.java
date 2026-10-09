package com.gyeongsan.cabinet.domain.alarm.port.out;

import com.gyeongsan.cabinet.domain.alarm.model.SlackChannelException;
import com.gyeongsan.cabinet.domain.alarm.model.SlackHistory;
import java.util.Optional;

public interface SlackChannelPort {

    /**
     * 커서(oldestTs)보다 새로운 최상위 메시지를 오래된 것부터 돌려준다(커서 자신은 제외).
     *
     * @throws SlackChannelException 호출이나 응답 해석에 실패한 경우
     */
    SlackHistory fetchNewerThan(String channelId, String oldestTs);

    /** 채널의 가장 최근 메시지 ts. 메시지가 하나도 없으면 비어 있다. */
    Optional<String> latestTs(String channelId);

    /** 메시지 원문 링크. 얻지 못하면 비어 있다(링크 없이 전달한다). */
    Optional<String> permalink(String channelId, String ts);
}
