package com.gyeongsan.cabinet.domain.admin.port.out;

import com.gyeongsan.cabinet.domain.admin.model.AdminActionLog;

public interface AdminActionLogPort {

    /** 호출한 트랜잭션에 참여해서 저장한다. 로그 저장이 실패하면 호출한 작업도 함께 롤백되어야 한다. */
    void save(AdminActionLog log);
}
