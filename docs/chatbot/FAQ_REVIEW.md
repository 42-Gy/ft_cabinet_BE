# 챗봇 FAQ 초안 검토표

자동 생성 파일입니다(`scripts/chatbot/build_seed.py`). 답변은 **초안**이며, 정책 설명이 틀리면 챗봇이 틀린 답을 합니다. 아래 '확인 필요'를 먼저 봐 주세요.

| # | 분류 | seedKey | 대표 질문 | 근거(코드/설정) | 확인 필요 |
|---|---|---|---|---|---|
| 1 | 대여 | `lent-how` | 사물함 대여 방법 | LentApplicationService.startLent, CustomOAuth2UserService.giveWelcomeGift, cabinet.policy.lent-term=31 | 화면 문구(어디서 신청하는지)는 프론트 기준으로 확인 필요. 아이템 설명 DB 문구는 '30일'이지만 설정값은 31일. |
| 2 | 대여 | `lent-fail` | 사물함 대여가 안 돼요 | startLent 검증 순서(예약, 패널티, 이미 대여, 상태, 라피신 제한, 대여권), ErrorCode | - |
| 3 | 대여 | `lent-ticket-get` | 대여권은 어떻게 얻나요 | giveWelcomeGift, UserDomainService.processLogtimeTransaction, LentTicketRewardPolicy(4800/900분), StoreDomainService(LENT 구매 불가) | 상점 구매 불가 안내 문구(코드)에는 '월 50시간'이라고 적혀 있으나 실제 기준은 80시간(4800분) — 코드 문구가 틀린 것으로 보임. 80시간/15시간은 설정값. |
| 4 | 대여 | `lent-period` | 대여 기간은 얼마나 되나요 | cabinet.policy.lent-term=31, extension-term=3, /me expiredAtIso | 설정값(31일). |
| 5 | 대여 | `lent-piscine` | 피시너도 사물함을 쓸 수 있나요 | validateLentTypePermission, PISCINER_EXTENSION_RESTRICTED, 피시너 식별(cursus 9만 있고 21 없음) | '수영장 사물함' 같은 표현은 평가 질문에서 일부러 엉뚱한 표현을 섞은 것. |
| 6 | 대여 | `lent-reserve` | 사물함 예약은 어떻게 하나요 | makeReservation, cancelReservation, RESERVATION_TTL_MINUTES=15 | - |
| 7 | 반납 | `return-how` | 사물함 반납 방법 | LentController.endLentCabinet, LentReturnRequest(\d{4}), AiServerAdapter | '공유 비밀번호'는 다음 사용자가 내 정보에서 확인하는 값(previousPassword)으로 보임 — 용어 확인. |
| 8 | 반납 | `return-ai-fail` | AI 검사가 계속 실패해요 | ErrorCode(CABINET_NOT_EMPTY, INVALID_IMAGE, AI_SERVER_ERROR), endLent forceReturn/endLentManual | 강제 반납은 '관리자 승인 요청' 상태(PENDING)로 접수됨. |
| 9 | 반납 | `return-overdue` | 대여 기간이 지나서 반납하면 어떻게 되나요 | LentApplicationService.checkAndApplyPenalty | 정책 불일치(백로그): 패널티 판정은 '만료 시각이 지나면 즉시'이고 스케줄러/알림은 '만료일 23:59:59까지 유예' — 당일 오후 반납에도 패널티가 붙을 수 있음. 답변에서 '만료일이 지난 뒤'를 어떻게 표현할지 결정 필요. |
| 10 | 연장 | `extend-ticket` | 연장권은 어떻게 쓰나요 | useExtension, cabinet.policy.extension-term=3, cabinet.items.extension-price=400 | 가격(400)은 설정값. |
| 11 | 연장 | `extend-renew` | 대여권으로 연장할 수 있나요 | manualRenew | - |
| 12 | 연장 | `extend-auto` | 자동 연장이 뭔가요 | LentScheduler.autoExtension(01:40), updateAutoExtensionStatus | '대여권이 자동으로 사라졌어요' 질문은 자동 연장으로 대여권이 소모된 경우를 가정. |
| 13 | 연장 | `extend-limit` | 연장권을 몇 개까지 살 수 있나요 | StoreDomainService MAX_EXTENSION_COUNT=5, EXTENSION_ITEM_LIMIT_EXCEEDED/PURCHASE_LIMIT | - |
| 14 | 연장 | `alarm-timing` | 반납 알림은 언제 오나요 | LentScheduler.checkExpirationImminent, checkOverdue(sendOverdueAlarm 1/3/7/14+) | 슬랙 사용자명이 인트라 ID와 달라 DM이 안 가는 사용자는 알림을 못 받음. |
| 15 | 이사 | `swap-how` | 사물함 이사는 어떻게 하나요 | useSwap, processSwapTransaction, makeReservation(swap ticket) | - |
| 16 | 이사 | `swap-conditions` | 이사가 안 돼요 | processSwapTransaction 검증(OVERDUE_USER_CANNOT_SWAP, PENALTY_USER, SAME_CABINET_SWAP, 예약 확인) | - |
| 17 | 이사 | `swap-expiry` | 이사하면 대여 기간이 어떻게 되나요 | processSwapTransaction: newLent expiredAt = oldLent.getExpiredAt() | - |
| 18 | 패널티 | `penalty-what` | 패널티가 뭔가요 | checkAndApplyPenalty, startLent/manualRenew/processSwapTransaction 의 PENALTY_USER 검증 | - |
| 19 | 패널티 | `penalty-decay` | 패널티는 언제 풀리나요 | LentScheduler.penaltyDecay(0 0 0 * * *) | - |
| 20 | 패널티 | `penalty-exempt` | 패널티 감면권은 어떻게 쓰나요 | usePenaltyExemption(decayPenalty 1일), cabinet.items.penalty-exemption-price=300 | 가격(300)은 설정값. 감면 효과는 '1일'(코드 기준) — 아이템 설명 문구('패널티를 감면합니다')와 확인. |
| 21 | 씨앗·아이템 | `coin-earn` | 씨앗은 어떻게 얻나요 | UserDomainService.doAttendance(100, 20회=2000) | 수박 이벤트에서의 씨앗 획득 방식은 확인하지 않음 — 문장은 '오간다'로만 표현. |
| 22 | 씨앗·아이템 | `item-list` | 상점에서 뭘 살 수 있나요 | cabinet.items.*, ItemPriceInitializer, StoreDomainService | 가격은 설정값이며 재시작마다 설정값으로 DB를 덮어씀 — 가격 변경 시 이 FAQ도 수정 필요(하드코딩 위험). |
| 23 | 수박 이벤트 | `watermelon-what` | 수박씨 강화 이벤트가 뭔가요 | README 10 (WatermelonConfig.MAX_LEVEL) | 확률표는 코드(WatermelonConfig) 기준이며 FAQ에는 싣지 않음. 최대 10강은 README 기준. |
| 24 | 수박 이벤트 | `watermelon-items` | 비료는 뭐가 있나요 | README 10 | - |
| 25 | 계정·로그인 | `social-link` | 카카오나 구글 계정을 연동하려면 | OauthLinkService, /v4/auth/link/{provider}, CustomOAuth2UserService.handleSocialLogin, OAUTH_ALREADY_LINKED* | 연동 메뉴 위치(화면)는 프론트 기준으로 확인 필요. |
| 26 | 계정·로그인 | `login-denied` | 로그인이 안 돼요 | CustomOAuth2UserService(allowedEmailDomain, bannedUserRepository) | - |
| 27 | 문의 | `contact` | 오류가 났을 때 어디에 문의하나요 | ErrorCode.AI_SERVER_ERROR 문구, 슬랙 오류제보 채널 | 채널 이름/연락처 구체화 필요. |

## 답변 전문

### 사물함 대여 방법 (`lent-how`, 대여)

사물함 목록에서 사용 가능한 사물함을 골라 대여를 신청하면 됩니다. 대여권이 1장 필요하고(신규 가입 시 1장 지급), 패널티 기간이 아니어야 하며, 이미 대여 중인 사물함이 없어야 합니다. 대여 기간은 31일입니다.

### 사물함 대여가 안 돼요 (`lent-fail`, 대여)

대여가 안 되는 흔한 이유는 다음과 같습니다. ① 대여권이 없음 ② 패널티 기간임 ③ 이미 대여 중인 사물함이 있음 ④ 다른 사용자가 예약(15분 선점)한 사물함임 ⑤ 사용할 수 없는 상태의 사물함임 ⑥ 피시너는 라피신 전용 사물함만, 일반 사용자는 라피신 전용이 아닌 사물함만 대여 가능. 계속 안 되면 오류제보 채널로 문의해 주세요.

### 대여권은 어떻게 얻나요 (`lent-ticket-get`, 대여)

대여권은 ① 처음 가입할 때 1장, ② 매월 1일에 지난달 로그타임이 80시간 이상일 때(트센은 15시간 이상) 1장 지급됩니다. 이미 사용하지 않은 대여권이 있으면 그 달에는 추가 지급되지 않습니다. 상점에서는 살 수 없습니다.

### 대여 기간은 얼마나 되나요 (`lent-period`, 대여)

대여 기간은 31일입니다. 만료일은 내 정보에서 확인할 수 있고, 연장권(3일), 대여권을 쓰는 수동 연장(31일), 자동 연장으로 기간을 늘릴 수 있습니다.

### 피시너도 사물함을 쓸 수 있나요 (`lent-piscine`, 대여)

피시너는 라피신 전용 사물함만 대여할 수 있고, 라피신 전용 사물함은 피시너만 사용할 수 있습니다. 또 피시너는 대여 기간을 연장할 수 없습니다.

### 사물함 예약은 어떻게 하나요 (`lent-reserve`, 대여)

원하는 사물함을 예약하면 15분 동안 다른 사람이 쓸 수 없게 선점됩니다. 예약은 한 번에 하나만 가능하고, 다른 사물함을 예약하면 이전 예약은 자동으로 취소됩니다. 직접 취소할 수도 있고, 15분이 지나면 자동으로 풀립니다. 이사를 하려는 경우 이사권이 필요합니다.

### 사물함 반납 방법 (`return-how`, 반납)

사물함을 비운 뒤 사물함 사진을 찍어 올리고, 다음 사용자를 위한 공유 비밀번호(4자리 숫자)를 입력하면 반납됩니다. AI가 사물함이 비어 있는지 검사하며, 물품이 감지되면 비우고 다시 촬영해야 합니다.

### AI 검사가 계속 실패해요 (`return-ai-fail`, 반납)

물품이 감지되었다는 안내가 나오면 사물함을 완전히 비우고 다시 촬영해 주세요. 사물함 사진이 아니라는 안내는 올바른 사진으로 다시 촬영해야 합니다. 그래도 통과되지 않으면 사유를 적어 강제(수동) 반납을 접수할 수 있고, 관리자가 확인합니다. AI 서버 오류로 반납이 보류된 경우에는 홈페이지에 공지된 연락처로 문의해 주세요.

### 대여 기간이 지나서 반납하면 어떻게 되나요 (`return-overdue`, 반납)

대여 만료일이 지난 뒤 반납하면 연체로 처리되어 패널티가 부과됩니다. 패널티는 연체일 × 3일이며(연체일이 0일로 계산돼도 최소 3일), 반납할 때 한 번에 부과됩니다.

### 연장권은 어떻게 쓰나요 (`extend-ticket`, 연장)

연장권을 1장 사용하면 대여 기간이 3일 늘어납니다. 연장권은 상점에서 씨앗 400개에 살 수 있고, 피시너는 연장할 수 없습니다.

### 대여권으로 연장할 수 있나요 (`extend-renew`, 연장)

대여권 1장을 사용해 대여 기간을 31일 연장할 수 있습니다(수동 연장). 패널티 기간이나 피시너는 사용할 수 없습니다.

### 자동 연장이 뭔가요 (`extend-auto`, 연장)

자동 연장을 켜 두면 만료 하루 전 새벽에 대여권 1장이 자동으로 사용되어 31일 연장됩니다. 대여권이 없으면 연장되지 않습니다. 자동 연장을 끄면 반납 임박 알림이 오니 직접 연장하거나 반납해 주세요.

### 연장권을 몇 개까지 살 수 있나요 (`extend-limit`, 연장)

연장권은 사용하지 않은 것을 최대 5개까지 보유할 수 있고, 한 달에 최대 5개까지 구매할 수 있습니다.

### 반납 알림은 언제 오나요 (`alarm-timing`, 연장)

대여 만료 7일 전과 1일 전에 슬랙 DM으로 반납 알림이 갑니다. 연체되면 연체 1일, 3일, 7일, 14일 이후에도 경고가 옵니다.

### 사물함 이사는 어떻게 하나요 (`swap-how`, 이사)

이사권이 1장 필요합니다. 옮기고 싶은 사물함을 먼저 예약(15분 선점)한 뒤, 지금 쓰던 사물함을 비우고 사진과 공유 비밀번호를 올려 이사를 신청하면 새 사물함으로 옮겨지고 기존 사물함은 반납 처리됩니다.

### 이사가 안 돼요 (`swap-conditions`, 이사)

이사는 이사권이 있어야 하고, 연체 중이거나 패널티 기간이면 할 수 없습니다. 지금 쓰는 사물함과 같은 사물함으로는 옮길 수 없고, 피시너와 라피신 전용 사물함 제한은 대여와 같습니다. 다른 사람이 예약한 사물함으로는 이사할 수 없습니다.

### 이사하면 대여 기간이 어떻게 되나요 (`swap-expiry`, 이사)

이사해도 만료일은 그대로 이어집니다. 새로 31일이 되지 않고, 기존 만료일이 새 사물함에도 적용됩니다.

### 패널티가 뭔가요 (`penalty-what`, 패널티)

대여 기간을 넘겨 연체한 채 반납하면 패널티가 부과됩니다. 패널티 기간에는 새로 사물함을 대여하거나 이사, 수동 연장을 할 수 없습니다.

### 패널티는 언제 풀리나요 (`penalty-decay`, 패널티)

패널티는 매일 자정에 1일씩 줄어듭니다. 남은 일수가 0이 되면 다시 대여할 수 있습니다.

### 패널티 감면권은 어떻게 쓰나요 (`penalty-exempt`, 패널티)

패널티 감면권을 1장 사용하면 패널티가 1일 줄어듭니다. 상점에서 씨앗 300개에 살 수 있고, 적용된 패널티가 없으면 사용할 수 없습니다.

### 씨앗은 어떻게 얻나요 (`coin-earn`, 씨앗·아이템)

출석 체크를 하면 하루에 씨앗 100개를 받고, 한 달에 20번째 출석을 달성하면 보너스로 씨앗 2000개(황금 수박씨)를 추가로 받습니다. 수박씨 이벤트에서도 씨앗이 오가며, 모은 씨앗은 상점에서 아이템을 사는 데 씁니다.

### 상점에서 뭘 살 수 있나요 (`item-list`, 씨앗·아이템)

상점에서는 연장권(400씨앗), 이사권(1000씨앗), 패널티 감면권(300씨앗)을 살 수 있습니다. 대여권은 상점에서 팔지 않고 가입 보상과 월간 로그타임 보상으로만 받습니다.

### 수박씨 강화 이벤트가 뭔가요 (`watermelon-what`, 수박 이벤트)

수박씨를 강화하는 이벤트입니다. 0강부터 최대 10강까지 올릴 수 있고, 강화를 시도하면 성공, 유지, 하락, 파괴 중 하나가 확률에 따라 결정됩니다. 씨앗(코인)으로 전용 상점에서 아이템을 사서 도움을 받을 수 있고, 최고 단계 기록은 리더보드에 올라갑니다.

### 비료는 뭐가 있나요 (`watermelon-items`, 수박 이벤트)

프리미엄 비료는 성공 확률을 올려 주고, 위험한 비료는 성공 확률을 크게 올리지만 7강 이상에서는 쓸 수 없습니다. 하락 방지권은 실패했을 때 단계가 내려가는 것을 막고, 파괴 방지권은 파괴를 막는 대신 현재 단계에서 2강이 내려갑니다.

### 카카오나 구글 계정을 연동하려면 (`social-link`, 계정·로그인)

먼저 42 인트라 계정으로 로그인한 뒤 카카오 또는 구글 계정을 연동해야 합니다. 연동한 뒤에는 연동한 계정으로도 로그인할 수 있습니다. 하나의 소셜 계정은 하나의 42 계정에만 연동되고, 한 42 계정에는 같은 종류의 소셜 계정을 하나만 연동할 수 있습니다.

### 로그인이 안 돼요 (`login-denied`, 계정·로그인)

이 서비스는 42경산 캠퍼스 계정(경산 캠퍼스 이메일)만 사용할 수 있습니다. 먼저 42 인트라로 로그인해 주세요. 카카오·구글로 로그인하려면 미리 연동해 두어야 합니다. 서비스 이용이 제한되었다는 안내가 나오면 관리자에게 문의해 주세요.

### 오류가 났을 때 어디에 문의하나요 (`contact`, 문의)

서비스 오류나 문의는 사물함 오류제보/문의 슬랙 채널에 남기거나 홈페이지에 공지된 연락처로 알려 주세요.

