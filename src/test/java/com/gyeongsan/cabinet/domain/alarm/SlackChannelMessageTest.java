package com.gyeongsan.cabinet.domain.alarm;

import static org.assertj.core.api.Assertions.assertThat;

import com.gyeongsan.cabinet.domain.alarm.model.SlackChannelMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class SlackChannelMessageTest {

    private static SlackChannelMessage withSubtype(String subtype) {
        return new SlackChannelMessage("1.0", "U1", "t", subtype, 0, 0);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(
            strings = {
                "bot_message",
                "file_share",
                "thread_broadcast",
                "me_message",
                "unknown_new_type"
            })
    @DisplayName("일반 글, 봇 글, 파일 글, 모르는 하위 유형은 시스템 메시지가 아니다")
    void notSystem(String subtype) {
        assertThat(withSubtype(subtype).isSystemMessage()).isFalse();
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "channel_join",
                "channel_leave",
                "channel_topic",
                "channel_purpose",
                "channel_name",
                "channel_archive",
                "group_join",
                "group_leave",
                "pinned_item",
                "message_deleted",
                "message_changed",
                "tombstone"
            })
    @DisplayName("입퇴장, 주제·이름 변경, 보관, 고정, 삭제·수정 알림은 시스템 메시지다")
    void system(String subtype) {
        assertThat(withSubtype(subtype).isSystemMessage()).as(subtype).isTrue();
    }
}
