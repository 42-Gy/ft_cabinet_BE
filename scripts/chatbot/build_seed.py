#!/usr/bin/env python3
"""초기 FAQ(초안)와 검토표를 만든다. 내용을 고칠 때는 이 파일을 고치고 다시 실행한다.

  python3 scripts/chatbot/build_seed.py

만들어지는 파일:
  src/main/resources/chatbot/faq-seed.json   초기 FAQ (테이블이 비어 있을 때만 DB 에 들어간다)
  docs/chatbot/FAQ_REVIEW.md                 검토용 표(각 답변의 근거 코드와 확인이 필요한 점)

평가 질문(src/test/resources/chatbot/eval-set.json, eval-holdout.json)은 이 스크립트가 만들지 않는다. 결과를 보고 평가 질문을 고치면
평가의 의미가 사라지므로 파일을 고정해 두었고, FAQ 를 병합·개명하면 faq-key-aliases.json 으로 옛 정답 키를 새 키로 읽는다.
FAQ 변형을 새로 쓸 때는 평가 질문을 보고 베끼지 않는다(테스트가 같은 FAQ 를 가리키는 평가 질문과 글자 겹침 0.85 이상인 변형을 막는다).

답변의 사실은 코드/설정에서 확인한 값이다. 값이 바뀔 수 있는 것(가격, 기간)은 설정 기준이며 FAQ_REVIEW.md 에 표시했다.

변형 수: 기본 6개, 서로 헷갈리는 군집(반납·이사·소셜 로그인·문의·연장 등)은 8개까지. 문체를 섞어 쓴다(격식체, 구어체, 짧은 키워드형,
상황 설명형, 동의어·외래어, 띄어쓰기).
"""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]

FAQS = [
  {
    "key": "lent-how",
    "category": "대여",
    "questions": [
      "사물함 대여 방법",
      "사물함은 어떻게 빌리나요?",
      "사물함을 대여하고 싶어요",
      "대여 신청은 어디서 하나요",
      "사물함 빌리는 순서 알려줘",
      "사물함 이용 절차",
      "락커 신청 방법",
      "사물함대여 어떻게 해"
    ],
    "answer": "사물함 목록에서 사용 가능한 사물함을 골라 대여를 신청하면 됩니다. 대여권이 1장 필요하고(신규 가입 시 1장 지급), 패널티 기간이 아니어야 하며, 이미 대여 중인 사물함이 없어야 합니다. 대여 기간은 31일입니다.",
    "source": "LentApplicationService.startLent, CustomOAuth2UserService.giveWelcomeGift, cabinet.policy.lent-term=31",
    "note": "화면 문구(어디서 신청하는지)는 프론트 기준으로 확인 필요. 아이템 설명 DB 문구는 '30일'이지만 설정값은 31일."
  },
  {
    "key": "lent-fail",
    "category": "대여",
    "questions": [
      "사물함 대여가 안 돼요",
      "대여 신청이 실패해요",
      "왜 사물함을 못 빌리나요",
      "빌리기 버튼이 먹통이에요",
      "사물함 못 빌리는 원인",
      "신청이 계속 튕겨요"
    ],
    "answer": "대여가 안 되는 흔한 이유는 다음과 같습니다. ① 대여권이 없음 ② 패널티 기간임 ③ 이미 대여 중인 사물함이 있음 ④ 다른 사용자가 예약(15분 선점)한 사물함임 ⑤ 사용할 수 없는 상태의 사물함임 ⑥ 피시너는 라피신 전용 사물함만, 일반 사용자는 라피신 전용이 아닌 사물함만 대여 가능. 계속 안 되면 오류제보 채널로 문의해 주세요.",
    "source": "startLent 검증 순서(예약, 패널티, 이미 대여, 상태, 라피신 제한, 대여권), ErrorCode",
    "note": ""
  },
  {
    "key": "lent-ticket-get",
    "category": "대여",
    "questions": [
      "대여권은 어떻게 얻나요",
      "대여권이 없어요",
      "대여권 구하는 방법",
      "대여권 지급 기준",
      "이용권이 떨어졌어요 어떻게 충전해요",
      "대여 티켓 받는 법",
      "월간 대여권 보상",
      "대여권 다 썼어요"
    ],
    "answer": "대여권은 ① 처음 가입할 때 1장, ② 매월 1일에 지난달 로그타임이 80시간 이상일 때(트센은 15시간 이상) 1장 지급됩니다. 이미 사용하지 않은 대여권이 있으면 그 달에는 추가 지급되지 않습니다. 상점에서는 살 수 없습니다.",
    "source": "giveWelcomeGift, UserDomainService.processLogtimeTransaction, LentTicketRewardPolicy(4800/900분), StoreDomainService(LENT 구매 불가)",
    "note": "상점 구매 불가 안내 문구(코드)에는 '월 50시간'이라고 적혀 있으나 실제 기준은 80시간(4800분) — 코드 문구가 틀린 것으로 보임. 80시간/15시간은 설정값."
  },
  {
    "key": "lent-period",
    "category": "대여",
    "questions": [
      "대여 기간은 얼마나 되나요",
      "사물함은 며칠 쓸 수 있어요",
      "대여 만료일이 언제인가요",
      "대여 가능 일수",
      "사물함 이용 기간 알려줘",
      "빌리면 언제까지 반납해야 해요"
    ],
    "answer": "대여 기간은 31일입니다. 만료일은 내 정보에서 확인할 수 있고, 연장권(3일), 대여권을 쓰는 수동 연장(31일), 자동 연장으로 기간을 늘릴 수 있습니다.",
    "source": "cabinet.policy.lent-term=31, extension-term=3, /me expiredAtIso",
    "note": "설정값(31일)."
  },
  {
    "key": "lent-piscine",
    "category": "대여",
    "questions": [
      "피시너도 사물함을 쓸 수 있나요",
      "라피신 전용 사물함이 뭔가요",
      "피시너 사물함 제한",
      "라피신 참가자 사물함 이용 규칙",
      "피시너가 본과정 사물함을 빌릴 수 있나요",
      "본과정 사람이 라피신 사물함 써도 되나요"
    ],
    "answer": "피시너는 라피신 전용 사물함만 대여할 수 있고, 라피신 전용 사물함은 피시너만 사용할 수 있습니다. 또 피시너는 대여 기간을 연장할 수 없습니다.",
    "source": "validateLentTypePermission, PISCINER_EXTENSION_RESTRICTED, 피시너 식별(cursus 9만 있고 21 없음)",
    "note": "'수영장 사물함' 같은 표현은 평가 질문에서 일부러 엉뚱한 표현을 섞은 것."
  },
  {
    "key": "lent-reserve",
    "category": "대여",
    "questions": [
      "사물함 예약은 어떻게 하나요",
      "예약하면 얼마나 유지되나요",
      "예약 취소할 수 있나요",
      "사물함 선점하는 방법",
      "예약 15분 지나면 풀려요?",
      "예약한 사물함 취소 방법"
    ],
    "answer": "원하는 사물함을 예약하면 15분 동안 다른 사람이 쓸 수 없게 선점됩니다. 예약은 한 번에 하나만 가능하고, 다른 사물함을 예약하면 이전 예약은 자동으로 취소됩니다. 직접 취소할 수도 있고, 15분이 지나면 자동으로 풀립니다. 이사를 하려는 경우 이사권이 필요합니다.",
    "source": "makeReservation, cancelReservation, RESERVATION_TTL_MINUTES=15",
    "note": ""
  },
  {
    "key": "return-how",
    "category": "반납",
    "questions": [
      "사물함 반납 방법",
      "반납은 어떻게 하나요",
      "사물함을 반납하고 싶어요",
      "반납 절차",
      "사물함 다 썼는데 반납 어떻게 해?",
      "반납할 때 필요한 것",
      "반납 시 사진을 찍는 이유",
      "공유 비밀번호는 어디에 입력하나요"
    ],
    "answer": "사물함을 비운 뒤 사물함 사진을 찍어 올리고, 다음 사용자를 위한 공유 비밀번호(4자리 숫자)를 입력하면 반납됩니다. 사진은 AI가 사물함이 비어 있는지 검사하는 데 쓰이며, 물품이 감지되면 비우고 다시 촬영해야 합니다. AI 검사가 계속 통과되지 않으면 강제(수동) 반납을 접수할 수 있습니다(반납 사진이 통과가 안 될 때의 안내 참고).",
    "source": "LentController.endLentCabinet, LentReturnRequest(\\d{4}), AiServerAdapter",
    "note": "'공유 비밀번호'는 다음 사용자가 내 정보에서 확인하는 값(previousPassword)으로 보임 — 용어 확인. / 2026-10-06 '사진은 AI 검사용' 명시 + return-ai-fail 상호 안내 추가."
  },
  {
    "key": "return-ai-fail",
    "category": "반납",
    "questions": [
      "AI 검사가 계속 실패해요",
      "물품이 감지됐다고 나와요",
      "반납 사진이 통과가 안 돼요",
      "반납할 때 AI가 계속 거절해요",
      "물건이 남아있다고 나오는데 비어 있어요",
      "수동 반납 신청 방법",
      "사물함 사진 인식 오류",
      "반납이 승인 대기 중이라고 나와요"
    ],
    "answer": "물품이 감지되었다는 안내가 나오면 사물함을 완전히 비우고 다시 촬영해 주세요. 사물함 사진이 아니라는 안내는 올바른 사진으로 다시 촬영해야 합니다. 그래도 통과되지 않으면 사유를 적어 강제(수동) 반납을 접수할 수 있고, 관리자가 확인합니다. AI 서버 오류로 반납이 보류된 경우에는 홈페이지에 공지된 연락처로 문의해 주세요. 정상적인 반납 절차는 '사물함 반납 방법' 안내를 참고하세요.",
    "source": "ErrorCode(CABINET_NOT_EMPTY, INVALID_IMAGE, AI_SERVER_ERROR), endLent forceReturn/endLentManual",
    "note": "강제 반납은 '관리자 승인 요청' 상태(PENDING)로 접수됨. / 2026-10-06 return-how 상호 안내 추가."
  },
  {
    "key": "return-overdue",
    "category": "반납",
    "questions": [
      "대여 기간이 지나서 반납하면 어떻게 되나요",
      "연체하면 어떻게 되나요",
      "반납을 늦게 하면 패널티가 있나요",
      "연체 반납 시 패널티 기준",
      "연체일수에 따른 패널티 계산",
      "기간 안에 반납 못 하면 어떻게 돼요"
    ],
    "answer": "대여 만료일이 지난 뒤 반납하면 연체로 처리되어 패널티가 부과됩니다. 패널티는 연체일 × 3일이며(연체일이 0일로 계산돼도 최소 3일), 반납할 때 한 번에 부과됩니다.",
    "source": "LentApplicationService.checkAndApplyPenalty",
    "note": "정책 불일치(백로그): 패널티 판정은 '만료 시각이 지나면 즉시'이고 스케줄러/알림은 '만료일 23:59:59까지 유예' — 당일 오후 반납에도 패널티가 붙을 수 있음. 답변에서 '만료일이 지난 뒤'를 어떻게 표현할지 결정 필요."
  },
  {
    "key": "extend-how",
    "category": "연장",
    "questions": [
      "연장권은 어떻게 쓰나요",
      "대여 기간을 연장하고 싶어요",
      "연장권을 사용하면 며칠 늘어나나요",
      "대여권으로 연장할 수 있나요",
      "수동 연장이 뭔가요",
      "한 달 더 연장하고 싶어요",
      "사물함 기간 연장 방법",
      "연장하려면 연장권이랑 대여권 중에 뭘 써야 해요"
    ],
    "answer": "대여 기간은 두 가지 방법으로 늘릴 수 있습니다. ① 연장권 1장: 기간이 3일 늘어납니다. 연장권은 상점에서 씨앗 400개에 살 수 있습니다. ② 대여권 1장을 쓰는 수동 연장: 기간이 31일 늘어납니다. 패널티 기간에는 쓸 수 없습니다. 두 방법 모두 피시너는 사용할 수 없습니다. 만료 하루 전에 대여권이 자동으로 쓰이게 하는 자동 연장도 있습니다.",
    "source": "useExtension(extension-term=3, extension-price=400), manualRenew(31일, 패널티 검증), 피시너 연장 제한(PISCINER_EXTENSION_RESTRICTED)",
    "note": "2026-10-06 extend-ticket + extend-renew 병합. 가격(400)·기간(3일/31일)은 설정값. 수동 연장의 패널티 제한 문구는 원 답변(패널티 기간·피시너 불가)을 합친 것이며, 연장권이 패널티 기간에 쓸 수 있는지는 코드 확인 필요."
  },
  {
    "key": "extend-auto",
    "category": "연장",
    "questions": [
      "자동 연장이 뭔가요",
      "자동 연장을 끄고 싶어요",
      "만료되기 전에 알아서 연장되나요",
      "자동 연장 켜는 법",
      "자동 연장하면 대여권이 쓰이나요",
      "만료 전날 자동으로 연장되는 건가요"
    ],
    "answer": "자동 연장을 켜 두면 만료 하루 전 새벽에 대여권 1장이 자동으로 사용되어 31일 연장됩니다. 대여권이 없으면 연장되지 않습니다. 자동 연장을 끄면 반납 임박 알림이 오니 직접 연장하거나 반납해 주세요.",
    "source": "LentScheduler.autoExtension(01:40), updateAutoExtensionStatus",
    "note": "'대여권이 자동으로 사라졌어요' 질문은 자동 연장으로 대여권이 소모된 경우를 가정."
  },
  {
    "key": "extend-limit",
    "category": "연장",
    "questions": [
      "연장권을 몇 개까지 살 수 있나요",
      "연장권 구매 한도",
      "연장권 보유 개수 제한",
      "연장권 최대 보유 수",
      "연장권 한 달 구매 가능 횟수",
      "연장권을 너무 많이 사면 안 되나요"
    ],
    "answer": "연장권은 사용하지 않은 것을 최대 5개까지 보유할 수 있고, 한 달에 최대 5개까지 구매할 수 있습니다.",
    "source": "StoreDomainService MAX_EXTENSION_COUNT=5, EXTENSION_ITEM_LIMIT_EXCEEDED/PURCHASE_LIMIT",
    "note": ""
  },
  {
    "key": "alarm-timing",
    "category": "연장",
    "questions": [
      "반납 알림은 언제 오나요",
      "슬랙으로 알림이 오나요",
      "만료 전에 알려주나요",
      "반납 알림 시점",
      "만료 며칠 전에 DM이 와요",
      "연체하면 경고 알림도 오나요"
    ],
    "answer": "대여 만료 7일 전과 1일 전에 슬랙 DM으로 반납 알림이 갑니다. 연체되면 연체 1일, 3일, 7일, 14일 이후에도 경고가 옵니다.",
    "source": "LentScheduler.checkExpirationImminent, checkOverdue(sendOverdueAlarm 1/3/7/14+)",
    "note": "슬랙 사용자명이 인트라 ID와 달라 DM이 안 가는 사용자는 알림을 못 받음."
  },
  {
    "key": "swap-how",
    "category": "이사",
    "questions": [
      "사물함 이사는 어떻게 하나요",
      "다른 사물함으로 옮기고 싶어요",
      "이사권은 어떻게 쓰나요",
      "사물함 이사 절차",
      "이사 신청 순서",
      "이사권 사용 방법",
      "다른 번호 사물함으로 옮기는 법",
      "이사하려면 예약부터 해야 하나요"
    ],
    "answer": "이사권이 1장 필요합니다. 옮기고 싶은 사물함을 먼저 예약(15분 선점)한 뒤, 지금 쓰던 사물함을 비우고 사진과 공유 비밀번호를 올려 이사를 신청하면 새 사물함으로 옮겨지고 기존 사물함은 반납 처리됩니다. 이사가 안 될 때는 이사 조건 안내를 확인해 주세요.",
    "source": "useSwap, processSwapTransaction, makeReservation(swap ticket)",
    "note": ""
  },
  {
    "key": "swap-conditions",
    "category": "이사",
    "questions": [
      "이사가 안 돼요",
      "이사 조건이 뭔가요",
      "연체 중에도 이사할 수 있나요",
      "이사 가능한 조건",
      "이사 못 하는 경우",
      "이사 신청이 거절되는 이유",
      "패널티 기간에 이사 가능한가요",
      "같은 번호로 이사 되나요"
    ],
    "answer": "이사는 이사권이 있어야 하고, 연체 중이거나 패널티 기간이면 할 수 없습니다. 지금 쓰는 사물함과 같은 사물함으로는 옮길 수 없고, 피시너와 라피신 전용 사물함 제한은 대여와 같습니다. 다른 사람이 예약한 사물함으로는 이사할 수 없습니다. 이사하는 방법 자체는 '사물함 이사는 어떻게 하나요' 안내를 참고하세요.",
    "source": "processSwapTransaction 검증(OVERDUE_USER_CANNOT_SWAP, PENALTY_USER, SAME_CABINET_SWAP, 예약 확인)",
    "note": ""
  },
  {
    "key": "swap-expiry",
    "category": "이사",
    "questions": [
      "이사하면 대여 기간이 어떻게 되나요",
      "이사하면 31일로 다시 시작하나요",
      "이사 후 만료일",
      "이사하면 만료일이 달라지나요",
      "이사 후 남은 대여 기간",
      "이사하면 기간이 리셋돼요?"
    ],
    "answer": "이사해도 만료일은 그대로 이어집니다. 새로 31일이 되지 않고, 기존 만료일이 새 사물함에도 적용됩니다.",
    "source": "processSwapTransaction: newLent expiredAt = oldLent.getExpiredAt()",
    "note": ""
  },
  {
    "key": "penalty-what",
    "category": "패널티",
    "questions": [
      "패널티가 뭔가요",
      "패널티 기간에는 뭘 못 하나요",
      "패널티는 왜 생기나요",
      "패널티 걸리면 제한되는 것",
      "패널티 부과 조건",
      "패널티 상태에서 할 수 있는 것과 없는 것"
    ],
    "answer": "대여 기간을 넘겨 연체한 채 반납하면 패널티가 부과됩니다. 패널티 기간에는 새로 사물함을 대여하거나 이사, 수동 연장을 할 수 없습니다.",
    "source": "checkAndApplyPenalty, startLent/manualRenew/processSwapTransaction 의 PENALTY_USER 검증",
    "note": ""
  },
  {
    "key": "penalty-decay",
    "category": "패널티",
    "questions": [
      "패널티는 언제 풀리나요",
      "패널티가 줄어드는 시간",
      "패널티 남은 일수는 어떻게 줄어요",
      "패널티 감소 방식",
      "패널티 해제 시점",
      "패널티가 끝나는 때"
    ],
    "answer": "패널티는 매일 자정에 1일씩 줄어듭니다. 남은 일수가 0이 되면 다시 대여할 수 있습니다.",
    "source": "LentScheduler.penaltyDecay(0 0 0 * * *)",
    "note": ""
  },
  {
    "key": "penalty-exempt",
    "category": "패널티",
    "questions": [
      "패널티 감면권은 어떻게 쓰나요",
      "패널티를 줄이고 싶어요",
      "감면권 가격이 얼마예요",
      "감면권 효과",
      "감면권 사용 방법",
      "패널티 줄이는 법"
    ],
    "answer": "패널티 감면권을 1장 사용하면 패널티가 1일 줄어듭니다. 상점에서 씨앗 300개에 살 수 있고, 적용된 패널티가 없으면 사용할 수 없습니다.",
    "source": "usePenaltyExemption(decayPenalty 1일), cabinet.items.penalty-exemption-price=300",
    "note": "가격(300)은 설정값. 감면 효과는 '1일'(코드 기준) — 아이템 설명 문구('패널티를 감면합니다')와 확인."
  },
  {
    "key": "coin-earn",
    "category": "씨앗·아이템",
    "questions": [
      "씨앗은 어떻게 얻나요",
      "코인을 모으는 방법",
      "씨앗을 얻는 방법이 뭐예요",
      "출석 체크 보상",
      "출석 보너스가 뭐예요",
      "황금 수박씨는 어떻게 얻어요",
      "씨앗 어떻게 모아요",
      "한 달 20회 출석 보상"
    ],
    "answer": "출석 체크를 하면 하루에 씨앗 100개를 받고, 한 달에 20번째 출석을 달성하면 보너스로 씨앗 2000개(황금 수박씨)를 추가로 받습니다. 수박씨 이벤트에서도 씨앗이 오가며, 모은 씨앗은 상점에서 아이템을 사는 데 씁니다.",
    "source": "UserDomainService.doAttendance(100, 20회=2000)",
    "note": "수박 이벤트에서의 씨앗 획득 방식은 확인하지 않음 — 문장은 '오간다'로만 표현."
  },
  {
    "key": "item-list",
    "category": "씨앗·아이템",
    "questions": [
      "상점에서 뭘 살 수 있나요",
      "아이템 가격이 얼마예요",
      "이사권 가격이 궁금해요",
      "상점 판매 품목",
      "상점에서 파는 것들",
      "대여권 판매 여부"
    ],
    "answer": "상점에서는 연장권(400씨앗), 이사권(1000씨앗), 패널티 감면권(300씨앗)을 살 수 있습니다. 대여권은 상점에서 팔지 않고 가입 보상과 월간 로그타임 보상으로만 받습니다.",
    "source": "cabinet.items.*, ItemPriceInitializer, StoreDomainService",
    "note": "가격은 설정값이며 재시작마다 설정값으로 DB를 덮어씀 — 가격 변경 시 이 FAQ도 수정 필요(하드코딩 위험)."
  },
  {
    "key": "watermelon-what",
    "category": "수박 이벤트",
    "questions": [
      "수박씨 강화 이벤트가 뭔가요",
      "수박 이벤트는 어떻게 하나요",
      "강화는 몇 강까지 있나요",
      "수박 이벤트 규칙",
      "강화 실패하면 어떻게 되나요",
      "리더보드 랭킹 기준",
      "수박 강화 게임 방법",
      "랭킹은 어떻게 집계돼요"
    ],
    "answer": "수박씨를 강화하는 이벤트입니다. 0강부터 최대 10강까지 올릴 수 있고, 강화를 시도하면 성공, 유지, 하락, 파괴 중 하나가 확률에 따라 결정됩니다. 씨앗(코인)으로 전용 상점에서 아이템을 사서 도움을 받을 수 있고, 최고 단계 기록은 리더보드에 올라갑니다.",
    "source": "README 10 (WatermelonConfig.MAX_LEVEL)",
    "note": "확률표는 코드(WatermelonConfig) 기준이며 FAQ에는 싣지 않음. 최대 10강은 README 기준."
  },
  {
    "key": "watermelon-items",
    "category": "수박 이벤트",
    "questions": [
      "비료는 뭐가 있나요",
      "하락 방지권이 뭔가요",
      "파괴 방지권 효과",
      "비료 종류와 효과",
      "방지권 종류",
      "비료는 몇 강까지 쓸 수 있어요"
    ],
    "answer": "프리미엄 비료는 성공 확률을 올려 주고, 위험한 비료는 성공 확률을 크게 올리지만 7강 이상에서는 쓸 수 없습니다. 하락 방지권은 실패했을 때 단계가 내려가는 것을 막고, 파괴 방지권은 파괴를 막는 대신 현재 단계에서 2강이 내려갑니다.",
    "source": "README 10",
    "note": ""
  },
  {
    "key": "social-link",
    "category": "계정·로그인",
    "questions": [
      "카카오나 구글 계정을 연동하려면",
      "소셜 로그인 연동 방법",
      "구글로 로그인하고 싶어요",
      "카카오 계정 연동 방법",
      "구글 연동 방법",
      "소셜 계정은 몇 개까지 연동돼요",
      "소셜 계정 로그인은 어떻게 하나요",
      "소셜 계정 연동 개수 제한"
    ],
    "answer": "소셜 로그인은 먼저 42 인트라 계정으로 로그인한 뒤 카카오 또는 구글 계정을 연동해 두면 쓸 수 있습니다. 연동한 뒤에는 연동한 계정으로도 로그인할 수 있습니다. 하나의 소셜 계정은 하나의 42 계정에만 연동되고, 한 42 계정에는 같은 종류의 소셜 계정을 하나만 연동할 수 있습니다. 로그인이 안 될 때는 '로그인이 안 돼요' 안내를 참고하세요.",
    "source": "OauthLinkService, /v4/auth/link/{provider}, CustomOAuth2UserService.handleSocialLogin, OAUTH_ALREADY_LINKED*",
    "note": "연동 메뉴 위치(화면)는 프론트 기준으로 확인 필요. 2026-10-06 역할 분리: 이 FAQ는 '연동 방법과 소셜 로그인', 로그인 실패 원인은 login-denied."
  },
  {
    "key": "login-denied",
    "category": "계정·로그인",
    "questions": [
      "로그인이 안 돼요",
      "경산 캠퍼스 유저만 사용할 수 있다고 나와요",
      "서비스 이용이 제한됐다고 나와요",
      "카카오로 로그인했는데 실패해요",
      "소셜 로그인이 안 될 때",
      "서울 캠퍼스 계정도 쓸 수 있나요",
      "로그인했더니 이용 제한 메시지가 떠요",
      "연동 안 한 계정으로 로그인하면 어떻게 돼요"
    ],
    "answer": "로그인이 안 되는 흔한 이유는 다음과 같습니다. ① 42경산 캠퍼스 계정(경산 캠퍼스 이메일)이 아님: 이 서비스는 경산 캠퍼스 계정만 사용할 수 있습니다. ② 카카오·구글로 로그인하려는데 미리 연동하지 않음: 먼저 42 인트라로 로그인해 소셜 계정을 연동한 뒤 사용해 주세요(연동 방법은 소셜 로그인 연동 안내 참고). ③ 서비스 이용이 제한되었다는 안내가 나옴: 관리자에게 문의해 주세요.",
    "source": "CustomOAuth2UserService(allowedEmailDomain, bannedUserRepository)",
    "note": "2026-10-06 역할 분리: 이 FAQ는 '로그인 실패 원인'(캠퍼스 제한·미연동·이용 제한). 개발 평가 세트의 '카카오로 로그인이 안 돼요'(expected=social-link)는 이 FAQ 쪽이 더 맞는 질문이라 라벨과 어긋남 — 평가 라벨은 바꾸지 않음."
  },
  {
    "key": "contact",
    "category": "문의",
    "questions": [
      "오류가 났을 때 어디에 문의하나요",
      "문의는 어디로 하나요",
      "버그를 제보하고 싶어요",
      "사물함 고장·파손 신고는 어디에 하나요",
      "운영진 연락처",
      "서비스 문제 신고 방법",
      "도움이 필요할 때 어디에 말해요",
      "오류 제보하는 곳"
    ],
    "answer": "서비스 오류나 문의, 사물함 고장·파손 신고는 사물함 오류제보/문의 슬랙 채널에 남겨 주세요. 채널에 남긴 글은 관리자가 확인합니다. 슬랙을 쓰기 어렵다면 홈페이지에 공지된 연락처로 알려 주세요.",
    "source": "ErrorCode.AI_SERVER_ERROR 문구, 슬랙 오류제보 채널(오류제보 → 관리자 DM 전달 기능)",
    "note": "2026-10-06 사물함 고장·파손 신고 포함(운영 방식 확인: 그 채널에 올리면 관리자 DM 으로 전달). 채널 이름/연락처 구체화 필요."
  }
]

CHANGES = [
    "extend-ticket 과 extend-renew 를 extend-how(대여 기간 연장 방법)로 병합했다. 평가 질문 파일은 그대로 두고 faq-key-aliases.json 으로 옛 키를 읽는다.",
    "return-how 에 '사진은 AI 가 비어 있는지 검사하는 용도'를 명시하고, return-how 와 return-ai-fail 이 서로를 안내하게 했다.",
    "social-link(연동 방법과 소셜 로그인)와 login-denied(로그인 실패 원인)의 역할을 나누고 서로 안내하게 했다.",
    "contact 답변에 사물함 고장·파손 신고를 포함했다(운영 방식: 오류제보 채널에 올리면 관리자에게 전달).",
    "FAQ 변형을 기본 6개, 혼동 군집은 8개로 늘렸다(82개 → 180개).",
    "주의: 변형 일부는 Phase 0 혼동 목록(어떤 질문이 틀렸는지)을 본 뒤 그 약점을 겨냥해 추가했다. 평가 질문 문장은 베끼지 않았지만(테스트로 확인) 개발·보류 세트 수치가 같은 저자·같은 의도 때문에 낙관적일 수 있다.",
    "swap-how 와 swap-conditions 가 서로를 안내하게 했다.",
]


def main() -> None:
    seed = []
    review = ["# 챗봇 FAQ 초안 검토표", "",
              "자동 생성 파일입니다(`scripts/chatbot/build_seed.py`). 답변은 **초안**이며, 정책 설명이 틀리면 챗봇이 틀린 답을 합니다. "
              "아래 '확인 필요'를 먼저 봐 주세요.", "",
              "## 2026-10-06 변경", ""]
    review += [f"- {c}" for c in CHANGES]
    review += ["", "## 검토표", "",
               "| # | 분류 | seedKey | 대표 질문 | 변형 수 | 근거(코드/설정) | 확인 필요 |", "|---|---|---|---|---|---|---|"]
    for i, f in enumerate(FAQS, 1):
        assert 6 <= len(f["questions"]) <= 8, f["key"]
        assert len(set(f["questions"])) == len(f["questions"]), f["key"]
        seed.append({"seedKey": f["key"], "category": f["category"], "answer": f["answer"], "enabled": True,
                     "questions": f["questions"]})
        review.append(f"| {i} | {f['category']} | `{f['key']}` | {f['questions'][0]} | {len(f['questions'])} | {f['source']} | {f['note'] or '-'} |")
    review += ["", "## 답변 전문", ""]
    for f in FAQS:
        review += [f"### {f['questions'][0]} (`{f['key']}`, {f['category']})", "", f["answer"], ""]
    (ROOT / "src/main/resources/chatbot").mkdir(parents=True, exist_ok=True)
    (ROOT / "docs/chatbot").mkdir(parents=True, exist_ok=True)
    (ROOT / "src/main/resources/chatbot/faq-seed.json").write_text(
        json.dumps(seed, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    (ROOT / "docs/chatbot/FAQ_REVIEW.md").write_text("\n".join(review) + "\n", encoding="utf-8")
    total = sum(len(f["questions"]) for f in FAQS)
    print(f"FAQ {len(seed)}건, 질문 표현 {total}개")


if __name__ == "__main__":
    main()
