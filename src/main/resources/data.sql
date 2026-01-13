INSERT IGNORE INTO user (
        id,
        blackholed_at,
        coin,
        deleted_at,
        email,
        email_alarm,
        name,
        push_alarm,
        role,
        slack_alarm,
        penalty_days,
        monthly_logtime
    )
VALUES (
        1,
        '2026-06-25 09:42:00.000000',
        97204,
        null,
        'juyoukim@student.42gyeongsan.kr',
        true,
        'juyoukim',
        false,
        'ADMIN',
        true,
        0,
        5000
    );
INSERT IGNORE INTO user (
        id,
        blackholed_at,
        coin,
        deleted_at,
        email,
        email_alarm,
        name,
        push_alarm,
        role,
        slack_alarm,
        penalty_days,
        monthly_logtime
    )
VALUES (
        2,
        '2026-01-18 09:42:00.000000',
        63050394784233398,
        null,
        'seonghan@student.42gyeongsan.kr',
        true,
        'seonghan',
        false,
        'ADMIN',
        true,
        0,
        12341234
    );
INSERT IGNORE INTO item (id, description, name, price, type)
VALUES (
        1,
        '?щЪ?⑥쓣 30?쇨컙 ??ы븷 ???덉뒿?덈떎.',
        '??ш텒',
        0,
        'LENT'
    );
INSERT IGNORE INTO item (id, description, name, price, type)
VALUES (
        2,
        '???湲곌컙??15???곗옣?⑸땲??',
        '?곗옣沅?',
        1000,
        'EXTENSION'
    );
INSERT IGNORE INTO item (id, description, name, price, type)
VALUES (
        3,
        ' ? 꾩옱 諛섎궔 ? ? 洹몃 ? 濡 ? ? ㅻⅨ ? щЪ ? ⑥ 쑝濡 ? ? 대룞 ? ⑸ 땲 ? ? ',
        ' ? 댁궗沅 ? ',
        1000,
        'SWAP'
    );
INSERT IGNORE INTO item (id, description, name, price, type)
VALUES (
        4,
        ' ? ⑤ 꼸 ? ? 湲곌컙 ? ?2 ? ? 以꾩뿬以띾땲 ? ? ',
        ' ? ⑤ 꼸 ? ? 媛먮 ㈃ 沅 ? ',
        100,
        'PENALTY_EXEMPTION'
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        1,
        1,
        2,
        ' PRIVATE ',
        1,
        1,
        ' Section 1 ',
        ' BROKEN ',
        '',
        2045
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        2,
        1,
        2,
        ' PRIVATE ',
        1,
        1,
        ' Section 1 ',
        ' AVAILABLE ',
        null,
        2046
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        3,
        1,
        2,
        ' PRIVATE ',
        1,
        1,
        ' Section 1 ',
        ' AVAILABLE ',
        null,
        2047
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        4,
        1,
        2,
        ' PRIVATE ',
        1,
        1,
        ' Section 1 ',
        ' AVAILABLE ',
        null,
        2048
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        5,
        1,
        2,
        ' PRIVATE ',
        1,
        1,
        ' Section 1 ',
        ' AVAILABLE ',
        null,
        2049
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        6,
        1,
        2,
        ' PRIVATE ',
        1,
        1,
        ' Section 1 ',
        ' AVAILABLE ',
        null,
        2050
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        7,
        1,
        2,
        ' PRIVATE ',
        1,
        1,
        ' Section 1 ',
        ' AVAILABLE ',
        null,
        2051
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        8,
        1,
        2,
        ' PRIVATE ',
        1,
        1,
        ' Section 1 ',
        ' AVAILABLE ',
        null,
        2052
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        9,
        1,
        2,
        ' PRIVATE ',
        1,
        1,
        ' Section 1 ',
        ' AVAILABLE ',
        null,
        2053
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        10,
        1,
        2,
        ' PRIVATE ',
        1,
        1,
        ' Section 1 ',
        ' AVAILABLE ',
        null,
        2054
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        11,
        1,
        2,
        ' PRIVATE ',
        1,
        1,
        ' Section 1 ',
        ' AVAILABLE ',
        null,
        2055
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        12,
        1,
        2,
        ' PRIVATE ',
        1,
        1,
        ' Section 1 ',
        ' AVAILABLE ',
        null,
        2056
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        13,
        1,
        2,
        ' PRIVATE ',
        1,
        1,
        ' Section 1 ',
        ' AVAILABLE ',
        null,
        2057
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        14,
        1,
        2,
        ' PRIVATE ',
        1,
        1,
        ' Section 1 ',
        ' AVAILABLE ',
        null,
        2058
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        15,
        1,
        2,
        ' PRIVATE ',
        1,
        1,
        ' Section 1 ',
        ' AVAILABLE ',
        null,
        2059
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        16,
        1,
        2,
        ' PRIVATE ',
        1,
        1,
        ' Section 1 ',
        ' AVAILABLE ',
        null,
        2060
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        17,
        1,
        2,
        ' PRIVATE ',
        1,
        1,
        ' Section 1 ',
        ' AVAILABLE ',
        null,
        2061
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        18,
        1,
        2,
        ' PRIVATE ',
        1,
        1,
        ' Section 1 ',
        ' AVAILABLE ',
        null,
        2062
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        19,
        1,
        2,
        ' PRIVATE ',
        1,
        1,
        ' Section 1 ',
        ' AVAILABLE ',
        null,
        2063
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        20,
        1,
        2,
        ' PRIVATE ',
        1,
        1,
        ' Section 1 ',
        ' AVAILABLE ',
        null,
        2064
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        21,
        1,
        2,
        ' PRIVATE ',
        1,
        1,
        ' Section 1 ',
        ' AVAILABLE ',
        null,
        2065
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        22,
        1,
        2,
        ' PRIVATE ',
        1,
        1,
        ' Section 1 ',
        ' AVAILABLE ',
        null,
        2066
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        23,
        1,
        2,
        ' PRIVATE ',
        1,
        1,
        ' Section 1 ',
        ' AVAILABLE ',
        null,
        2067
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        24,
        1,
        2,
        ' PRIVATE ',
        1,
        1,
        ' Section 1 ',
        ' AVAILABLE ',
        null,
        2068
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        25,
        1,
        2,
        ' PRIVATE ',
        1,
        1,
        ' Section 1 ',
        ' AVAILABLE ',
        null,
        2069
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        26,
        1,
        2,
        ' PRIVATE ',
        1,
        1,
        ' Section 1 ',
        ' AVAILABLE ',
        null,
        2070
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        27,
        1,
        2,
        ' PRIVATE ',
        1,
        1,
        ' Section 1 ',
        ' AVAILABLE ',
        null,
        2071
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        28,
        1,
        2,
        ' PRIVATE ',
        1,
        1,
        ' Section 1 ',
        ' AVAILABLE ',
        null,
        2072
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        29,
        1,
        2,
        ' PRIVATE ',
        1,
        1,
        ' Section 1 ',
        ' AVAILABLE ',
        null,
        2073
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        30,
        1,
        2,
        ' PRIVATE ',
        1,
        1,
        ' Section 1 ',
        ' AVAILABLE ',
        null,
        2074
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        31,
        1,
        2,
        ' PRIVATE ',
        1,
        1,
        ' Section 1 ',
        ' AVAILABLE ',
        null,
        2075
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        32,
        1,
        2,
        ' PRIVATE ',
        1,
        1,
        ' Section 1 ',
        ' AVAILABLE ',
        null,
        2076
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        33,
        1,
        2,
        ' PRIVATE ',
        1,
        1,
        ' Section 1 ',
        ' AVAILABLE ',
        null,
        2077
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        34,
        1,
        2,
        ' PRIVATE ',
        1,
        1,
        ' Section 1 ',
        ' AVAILABLE ',
        null,
        2078
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        35,
        1,
        2,
        ' PRIVATE ',
        1,
        1,
        ' Section 1 ',
        ' AVAILABLE ',
        null,
        2079
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        36,
        1,
        2,
        ' PRIVATE ',
        1,
        1,
        ' Section 1 ',
        ' AVAILABLE ',
        null,
        2080
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        37,
        1,
        2,
        ' PRIVATE ',
        1,
        1,
        ' Section 1 ',
        ' AVAILABLE ',
        null,
        2081
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        38,
        1,
        2,
        ' PRIVATE ',
        1,
        1,
        ' Section 1 ',
        ' AVAILABLE ',
        null,
        2082
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        39,
        1,
        2,
        ' PRIVATE ',
        1,
        1,
        ' Section 1 ',
        ' AVAILABLE ',
        null,
        2083
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        40,
        1,
        2,
        ' PRIVATE ',
        1,
        1,
        ' Section 1 ',
        ' AVAILABLE ',
        null,
        2084
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        41,
        1,
        2,
        ' PRIVATE ',
        1,
        1,
        ' Section 1 ',
        ' AVAILABLE ',
        null,
        2085
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        42,
        1,
        2,
        ' PRIVATE ',
        1,
        1,
        ' Section 1 ',
        ' AVAILABLE ',
        null,
        2086
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        43,
        1,
        2,
        ' PRIVATE ',
        1,
        1,
        ' Section 1 ',
        ' AVAILABLE ',
        null,
        2087
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        44,
        1,
        2,
        ' PRIVATE ',
        1,
        1,
        ' Section 1 ',
        ' AVAILABLE ',
        null,
        2088
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        45,
        1,
        2,
        ' PRIVATE ',
        1,
        1,
        ' Section 1 ',
        ' AVAILABLE ',
        null,
        2089
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        46,
        1,
        2,
        ' PRIVATE ',
        1,
        1,
        ' Section 1 ',
        ' AVAILABLE ',
        null,
        2090
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        47,
        1,
        2,
        ' PRIVATE ',
        1,
        1,
        ' Section 1 ',
        ' AVAILABLE ',
        null,
        2091
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        48,
        1,
        2,
        ' PRIVATE ',
        1,
        1,
        ' Section 1 ',
        ' AVAILABLE ',
        null,
        2092
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        49,
        1,
        2,
        ' PRIVATE ',
        1,
        1,
        ' Section 1 ',
        ' AVAILABLE ',
        null,
        2093
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        50,
        1,
        2,
        ' PRIVATE ',
        1,
        1,
        ' Section 1 ',
        ' AVAILABLE ',
        null,
        2094
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        51,
        1,
        2,
        ' PRIVATE ',
        1,
        1,
        ' Section 1 ',
        ' AVAILABLE ',
        null,
        2095
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        52,
        1,
        2,
        ' PRIVATE ',
        1,
        1,
        ' Section 1 ',
        ' AVAILABLE ',
        null,
        2096
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        53,
        1,
        2,
        ' PRIVATE ',
        1,
        1,
        ' Section 1 ',
        ' AVAILABLE ',
        null,
        2097
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        54,
        1,
        2,
        ' PRIVATE ',
        1,
        1,
        ' Section 1 ',
        ' AVAILABLE ',
        null,
        2098
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        55,
        1,
        2,
        ' PRIVATE ',
        1,
        1,
        ' Section 1 ',
        ' AVAILABLE ',
        null,
        2099
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        56,
        1,
        2,
        ' PRIVATE ',
        1,
        1,
        ' Section 1 ',
        ' AVAILABLE ',
        null,
        2100
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        57,
        1,
        2,
        ' PRIVATE ',
        1,
        1,
        ' Section 1 ',
        ' AVAILABLE ',
        null,
        2101
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        58,
        1,
        2,
        ' PRIVATE ',
        1,
        1,
        ' Section 1 ',
        ' AVAILABLE ',
        null,
        2102
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        59,
        1,
        2,
        ' PRIVATE ',
        1,
        1,
        ' Section 1 ',
        ' AVAILABLE ',
        null,
        2103
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        60,
        1,
        2,
        ' PRIVATE ',
        1,
        1,
        ' Section 1 ',
        ' AVAILABLE ',
        null,
        2104
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        61,
        1,
        2,
        ' PRIVATE ',
        1,
        1,
        ' Section 1 ',
        ' AVAILABLE ',
        null,
        2105
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        62,
        1,
        2,
        ' PRIVATE ',
        1,
        1,
        ' Section 1 ',
        ' AVAILABLE ',
        null,
        2106
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        63,
        1,
        2,
        ' PRIVATE ',
        1,
        1,
        ' Section 1 ',
        ' AVAILABLE ',
        null,
        2107
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        64,
        1,
        2,
        ' PRIVATE ',
        1,
        1,
        ' Section 1 ',
        ' AVAILABLE ',
        null,
        2108
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        128,
        1,
        2,
        ' PRIVATE ',
        1,
        1,
        ' Section 2 ',
        ' AVAILABLE ',
        null,
        2001
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        129,
        1,
        2,
        ' PRIVATE ',
        1,
        1,
        ' Section 2 ',
        ' AVAILABLE ',
        null,
        2002
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        130,
        1,
        2,
        ' PRIVATE ',
        1,
        1,
        ' Section 2 ',
        ' AVAILABLE ',
        null,
        2003
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        131,
        1,
        2,
        ' PRIVATE ',
        1,
        1,
        ' Section 2 ',
        ' AVAILABLE ',
        null,
        2004
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        132,
        1,
        2,
        ' PRIVATE ',
        1,
        1,
        ' Section 2 ',
        ' AVAILABLE ',
        null,
        2005
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        133,
        1,
        2,
        ' PRIVATE ',
        1,
        1,
        ' Section 2 ',
        ' AVAILABLE ',
        null,
        2006
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        134,
        1,
        2,
        ' PRIVATE ',
        1,
        1,
        ' Section 2 ',
        ' AVAILABLE ',
        null,
        2007
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        135,
        1,
        2,
        ' PRIVATE ',
        1,
        1,
        ' Section 2 ',
        ' AVAILABLE ',
        null,
        2008
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        136,
        1,
        2,
        ' PRIVATE ',
        1,
        1,
        ' Section 2 ',
        ' AVAILABLE ',
        null,
        2009
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        137,
        1,
        2,
        ' PRIVATE ',
        1,
        1,
        ' Section 2 ',
        ' AVAILABLE ',
        null,
        2010
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        138,
        1,
        2,
        ' PRIVATE ',
        1,
        1,
        ' Section 2 ',
        ' AVAILABLE ',
        null,
        2011
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        139,
        1,
        2,
        ' PRIVATE ',
        1,
        1,
        ' Section 2 ',
        ' AVAILABLE ',
        null,
        2012
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        140,
        1,
        2,
        ' PRIVATE ',
        1,
        1,
        ' Section 2 ',
        ' AVAILABLE ',
        null,
        2013
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        141,
        1,
        2,
        ' PRIVATE ',
        1,
        1,
        ' Section 2 ',
        ' AVAILABLE ',
        null,
        2014
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        142,
        1,
        2,
        ' PRIVATE ',
        1,
        1,
        ' Section 2 ',
        ' AVAILABLE ',
        null,
        2015
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        143,
        1,
        2,
        ' PRIVATE ',
        1,
        1,
        ' Section 2 ',
        ' AVAILABLE ',
        null,
        2016
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        144,
        1,
        2,
        ' PRIVATE ',
        1,
        1,
        ' Section 2 ',
        ' AVAILABLE ',
        null,
        2017
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        145,
        1,
        2,
        ' PRIVATE ',
        1,
        1,
        ' Section 2 ',
        ' AVAILABLE ',
        null,
        2018
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        146,
        1,
        2,
        ' PRIVATE ',
        1,
        1,
        ' Section 2 ',
        ' AVAILABLE ',
        null,
        2019
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        147,
        1,
        2,
        ' PRIVATE ',
        1,
        1,
        ' Section 2 ',
        ' AVAILABLE ',
        null,
        2020
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        148,
        1,
        2,
        ' PRIVATE ',
        1,
        1,
        ' Section 2 ',
        ' AVAILABLE ',
        null,
        2021
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        149,
        1,
        2,
        ' PRIVATE ',
        1,
        1,
        ' Section 2 ',
        ' AVAILABLE ',
        null,
        2022
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        150,
        1,
        2,
        ' PRIVATE ',
        1,
        1,
        ' Section 2 ',
        ' AVAILABLE ',
        null,
        2023
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        151,
        1,
        2,
        ' PRIVATE ',
        1,
        1,
        ' Section 2 ',
        ' AVAILABLE ',
        null,
        2024
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        152,
        1,
        2,
        ' PRIVATE ',
        1,
        1,
        ' Section 2 ',
        ' AVAILABLE ',
        null,
        2025
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        153,
        1,
        2,
        ' PRIVATE ',
        1,
        1,
        ' Section 2 ',
        ' AVAILABLE ',
        null,
        2026
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        154,
        1,
        2,
        ' PRIVATE ',
        1,
        1,
        ' Section 2 ',
        ' AVAILABLE ',
        null,
        2027
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        155,
        1,
        2,
        ' PRIVATE ',
        1,
        1,
        ' Section 2 ',
        ' AVAILABLE ',
        null,
        2028
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        156,
        1,
        2,
        ' PRIVATE ',
        1,
        1,
        ' Section 2 ',
        ' AVAILABLE ',
        null,
        2029
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        157,
        1,
        2,
        ' PRIVATE ',
        1,
        1,
        ' Section 2 ',
        ' AVAILABLE ',
        null,
        2030
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        158,
        1,
        2,
        ' PRIVATE ',
        1,
        1,
        ' Section 2 ',
        ' AVAILABLE ',
        null,
        2031
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        159,
        1,
        2,
        ' PRIVATE ',
        1,
        1,
        ' Section 2 ',
        ' AVAILABLE ',
        null,
        2032
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        160,
        1,
        2,
        ' PRIVATE ',
        1,
        1,
        ' Section 2 ',
        ' AVAILABLE ',
        null,
        2033
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        161,
        1,
        2,
        ' PRIVATE ',
        1,
        1,
        ' Section 2 ',
        ' AVAILABLE ',
        null,
        2034
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        162,
        1,
        2,
        ' PRIVATE ',
        1,
        1,
        ' Section 2 ',
        ' AVAILABLE ',
        null,
        2035
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        163,
        1,
        2,
        ' PRIVATE ',
        1,
        1,
        ' Section 2 ',
        ' AVAILABLE ',
        null,
        2036
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        164,
        1,
        2,
        ' PRIVATE ',
        1,
        1,
        ' Section 2 ',
        ' AVAILABLE ',
        null,
        2037
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        165,
        1,
        2,
        ' PRIVATE ',
        1,
        1,
        ' Section 2 ',
        ' AVAILABLE ',
        null,
        2038
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        166,
        1,
        2,
        ' PRIVATE ',
        1,
        1,
        ' Section 2 ',
        ' AVAILABLE ',
        null,
        2039
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        167,
        1,
        2,
        ' PRIVATE ',
        1,
        1,
        ' Section 2 ',
        ' AVAILABLE ',
        null,
        2040
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        168,
        1,
        2,
        ' PRIVATE ',
        1,
        1,
        ' Section 2 ',
        ' AVAILABLE ',
        null,
        2041
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        169,
        1,
        2,
        ' PRIVATE ',
        1,
        1,
        ' Section 2 ',
        ' AVAILABLE ',
        null,
        2042
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        170,
        1,
        2,
        ' PRIVATE ',
        1,
        1,
        ' Section 2 ',
        ' AVAILABLE ',
        null,
        2043
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        171,
        1,
        2,
        ' PRIVATE ',
        1,
        1,
        ' Section 2 ',
        ' AVAILABLE ',
        null,
        2044
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        191,
        1,
        2,
        ' PRIVATE ',
        1,
        1,
        ' Section 3 ',
        ' AVAILABLE ',
        null,
        2109
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        192,
        1,
        2,
        ' PRIVATE ',
        1,
        1,
        ' Section 3 ',
        ' AVAILABLE ',
        null,
        2110
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        193,
        1,
        2,
        ' PRIVATE ',
        1,
        1,
        ' Section 3 ',
        ' AVAILABLE ',
        null,
        2111
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        194,
        1,
        2,
        ' PRIVATE ',
        1,
        1,
        ' Section 3 ',
        ' AVAILABLE ',
        null,
        2112
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        195,
        1,
        2,
        ' PRIVATE ',
        1,
        1,
        ' Section 3 ',
        ' AVAILABLE ',
        null,
        2113
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        196,
        1,
        2,
        ' PRIVATE ',
        1,
        1,
        ' Section 3 ',
        ' AVAILABLE ',
        null,
        2114
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        197,
        1,
        2,
        ' PRIVATE ',
        1,
        1,
        ' Section 3 ',
        ' AVAILABLE ',
        null,
        2115
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        198,
        1,
        2,
        ' PRIVATE ',
        1,
        1,
        ' Section 3 ',
        ' AVAILABLE ',
        null,
        2116
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        199,
        1,
        2,
        ' PRIVATE ',
        1,
        1,
        ' Section 3 ',
        ' AVAILABLE ',
        null,
        2117
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        200,
        1,
        2,
        ' PRIVATE ',
        1,
        1,
        ' Section 3 ',
        ' AVAILABLE ',
        null,
        2118
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        201,
        1,
        2,
        ' PRIVATE ',
        1,
        1,
        ' Section 3 ',
        ' AVAILABLE ',
        null,
        2119
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        202,
        1,
        2,
        ' PRIVATE ',
        1,
        1,
        ' Section 3 ',
        ' AVAILABLE ',
        null,
        2120
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        203,
        1,
        2,
        ' PRIVATE ',
        1,
        1,
        ' Section 3 ',
        ' AVAILABLE ',
        null,
        2121
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        204,
        1,
        2,
        ' PRIVATE ',
        1,
        1,
        ' Section 3 ',
        ' AVAILABLE ',
        null,
        2122
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        205,
        1,
        2,
        ' PRIVATE ',
        1,
        1,
        ' Section 3 ',
        ' AVAILABLE ',
        null,
        2123
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        206,
        1,
        2,
        ' PRIVATE ',
        1,
        1,
        ' Section 3 ',
        ' AVAILABLE ',
        null,
        2124
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        207,
        1,
        2,
        ' PRIVATE ',
        1,
        1,
        ' Section 3 ',
        ' AVAILABLE ',
        null,
        2125
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        208,
        1,
        2,
        ' PRIVATE ',
        1,
        1,
        ' Section 3 ',
        ' AVAILABLE ',
        null,
        2126
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        209,
        1,
        2,
        ' PRIVATE ',
        1,
        1,
        ' Section 3 ',
        ' AVAILABLE ',
        null,
        2127
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        210,
        1,
        2,
        ' PRIVATE ',
        1,
        1,
        ' Section 3 ',
        ' AVAILABLE ',
        null,
        2128
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        211,
        1,
        2,
        ' PRIVATE ',
        1,
        1,
        ' Section 3 ',
        ' AVAILABLE ',
        null,
        2129
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        212,
        1,
        2,
        ' PRIVATE ',
        1,
        1,
        ' Section 3 ',
        ' AVAILABLE ',
        null,
        2130
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        213,
        1,
        2,
        ' PRIVATE ',
        1,
        1,
        ' Section 3 ',
        ' AVAILABLE ',
        null,
        2131
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        214,
        1,
        2,
        ' PRIVATE ',
        1,
        1,
        ' Section 3 ',
        ' AVAILABLE ',
        null,
        2132
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        222,
        1,
        2,
        ' PRIVATE ',
        1,
        1,
        ' Section 4 ',
        ' FULL ',
        null,
        2133
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        223,
        1,
        2,
        ' PRIVATE ',
        1,
        1,
        ' Section 4 ',
        ' AVAILABLE ',
        null,
        2134
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        224,
        1,
        2,
        ' PRIVATE ',
        1,
        1,
        ' Section 4 ',
        ' AVAILABLE ',
        null,
        2135
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        225,
        1,
        2,
        ' PRIVATE ',
        1,
        1,
        ' Section 4 ',
        ' AVAILABLE ',
        null,
        2136
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        226,
        1,
        2,
        ' PRIVATE ',
        1,
        1,
        ' Section 4 ',
        ' AVAILABLE ',
        null,
        2137
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        227,
        1,
        2,
        ' PRIVATE ',
        1,
        1,
        ' Section 4 ',
        ' AVAILABLE ',
        null,
        2138
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        228,
        1,
        2,
        ' PRIVATE ',
        1,
        1,
        ' Section 4 ',
        ' AVAILABLE ',
        null,
        2139
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        229,
        1,
        2,
        ' PRIVATE ',
        1,
        1,
        ' Section 4 ',
        ' AVAILABLE ',
        null,
        2140
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        230,
        1,
        2,
        ' PRIVATE ',
        1,
        1,
        ' Section 4 ',
        ' AVAILABLE ',
        null,
        2141
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        231,
        1,
        2,
        ' PRIVATE ',
        1,
        1,
        ' Section 4 ',
        ' AVAILABLE ',
        null,
        2142
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        232,
        1,
        2,
        ' PRIVATE ',
        1,
        1,
        ' Section 4 ',
        ' AVAILABLE ',
        null,
        2143
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        233,
        1,
        2,
        ' PRIVATE ',
        1,
        1,
        ' Section 4 ',
        ' AVAILABLE ',
        null,
        2144
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        234,
        1,
        2,
        ' PRIVATE ',
        1,
        1,
        ' Section 4 ',
        ' AVAILABLE ',
        null,
        2145
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        235,
        1,
        2,
        ' PRIVATE ',
        1,
        1,
        ' Section 4 ',
        ' AVAILABLE ',
        null,
        2146
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        236,
        1,
        2,
        ' PRIVATE ',
        1,
        1,
        ' Section 4 ',
        ' AVAILABLE ',
        null,
        2147
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        237,
        1,
        2,
        ' PRIVATE ',
        1,
        1,
        ' Section 4 ',
        ' AVAILABLE ',
        null,
        2148
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        238,
        1,
        2,
        ' PRIVATE ',
        1,
        1,
        ' Section 4 ',
        ' AVAILABLE ',
        null,
        2149
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        239,
        1,
        2,
        ' PRIVATE ',
        1,
        1,
        ' Section 4 ',
        ' AVAILABLE ',
        null,
        2150
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        240,
        1,
        2,
        ' PRIVATE ',
        1,
        1,
        ' Section 4 ',
        ' AVAILABLE ',
        null,
        2151
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        241,
        1,
        2,
        ' PRIVATE ',
        1,
        1,
        ' Section 4 ',
        ' AVAILABLE ',
        null,
        2152
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        242,
        1,
        2,
        ' PRIVATE ',
        1,
        1,
        ' Section 4 ',
        ' AVAILABLE ',
        null,
        2153
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        243,
        1,
        2,
        ' PRIVATE ',
        1,
        1,
        ' Section 4 ',
        ' AVAILABLE ',
        null,
        2154
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        244,
        1,
        2,
        ' PRIVATE ',
        1,
        1,
        ' Section 4 ',
        ' AVAILABLE ',
        null,
        2155
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        245,
        1,
        2,
        ' PRIVATE ',
        1,
        1,
        ' Section 4 ',
        ' AVAILABLE ',
        null,
        2156
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        246,
        1,
        2,
        ' PRIVATE ',
        1,
        1,
        ' Section 4 ',
        ' AVAILABLE ',
        null,
        2157
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        247,
        1,
        2,
        ' PRIVATE ',
        1,
        1,
        ' Section 4 ',
        ' AVAILABLE ',
        null,
        2158
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        248,
        1,
        2,
        ' PRIVATE ',
        1,
        1,
        ' Section 4 ',
        ' AVAILABLE ',
        null,
        2159
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        249,
        1,
        2,
        ' PRIVATE ',
        1,
        1,
        ' Section 4 ',
        ' AVAILABLE ',
        null,
        2160
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        250,
        1,
        2,
        ' PRIVATE ',
        1,
        1,
        ' Section 4 ',
        ' AVAILABLE ',
        null,
        2161
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        251,
        1,
        2,
        ' PRIVATE ',
        1,
        1,
        ' Section 4 ',
        ' AVAILABLE ',
        null,
        2162
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        252,
        1,
        2,
        ' PRIVATE ',
        1,
        1,
        ' Section 4 ',
        ' AVAILABLE ',
        null,
        2163
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        253,
        1,
        2,
        ' PRIVATE ',
        1,
        1,
        ' Section 4 ',
        ' AVAILABLE ',
        null,
        2164
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        254,
        1,
        2,
        ' PRIVATE ',
        1,
        1,
        ' Section 4 ',
        ' AVAILABLE ',
        null,
        2165
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        255,
        1,
        2,
        ' PRIVATE ',
        1,
        1,
        ' Section 4 ',
        ' AVAILABLE ',
        null,
        2166
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        256,
        1,
        2,
        ' PRIVATE ',
        1,
        1,
        ' Section 4 ',
        ' AVAILABLE ',
        null,
        2167
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        257,
        1,
        2,
        ' PRIVATE ',
        1,
        1,
        ' Section 4 ',
        ' AVAILABLE ',
        null,
        2168
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        285,
        1,
        3,
        ' PRIVATE ',
        1,
        1,
        ' Section 5 ',
        ' AVAILABLE ',
        null,
        3045
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        286,
        1,
        3,
        ' PRIVATE ',
        1,
        1,
        ' Section 5 ',
        ' AVAILABLE ',
        null,
        3046
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        287,
        1,
        3,
        ' PRIVATE ',
        1,
        1,
        ' Section 5 ',
        ' AVAILABLE ',
        null,
        3047
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        288,
        1,
        3,
        ' PRIVATE ',
        1,
        1,
        ' Section 5 ',
        ' AVAILABLE ',
        null,
        3048
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        289,
        1,
        3,
        ' PRIVATE ',
        1,
        1,
        ' Section 5 ',
        ' AVAILABLE ',
        null,
        3049
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        290,
        1,
        3,
        ' PRIVATE ',
        1,
        1,
        ' Section 5 ',
        ' AVAILABLE ',
        null,
        3050
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        291,
        1,
        3,
        ' PRIVATE ',
        1,
        1,
        ' Section 5 ',
        ' AVAILABLE ',
        null,
        3051
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        292,
        1,
        3,
        ' PRIVATE ',
        1,
        1,
        ' Section 5 ',
        ' AVAILABLE ',
        null,
        3052
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        293,
        1,
        3,
        ' PRIVATE ',
        1,
        1,
        ' Section 5 ',
        ' AVAILABLE ',
        null,
        3053
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        294,
        1,
        3,
        ' PRIVATE ',
        1,
        1,
        ' Section 5 ',
        ' AVAILABLE ',
        null,
        3054
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        295,
        1,
        3,
        ' PRIVATE ',
        1,
        1,
        ' Section 5 ',
        ' AVAILABLE ',
        null,
        3055
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        296,
        1,
        3,
        ' PRIVATE ',
        1,
        1,
        ' Section 5 ',
        ' AVAILABLE ',
        null,
        3056
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        297,
        1,
        3,
        ' PRIVATE ',
        1,
        1,
        ' Section 5 ',
        ' AVAILABLE ',
        null,
        3057
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        298,
        1,
        3,
        ' PRIVATE ',
        1,
        1,
        ' Section 5 ',
        ' AVAILABLE ',
        null,
        3058
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        299,
        1,
        3,
        ' PRIVATE ',
        1,
        1,
        ' Section 5 ',
        ' AVAILABLE ',
        null,
        3059
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        300,
        1,
        3,
        ' PRIVATE ',
        1,
        1,
        ' Section 5 ',
        ' AVAILABLE ',
        null,
        3060
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        301,
        1,
        3,
        ' PRIVATE ',
        1,
        1,
        ' Section 5 ',
        ' AVAILABLE ',
        null,
        3061
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        302,
        1,
        3,
        ' PRIVATE ',
        1,
        1,
        ' Section 5 ',
        ' AVAILABLE ',
        null,
        3062
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        303,
        1,
        3,
        ' PRIVATE ',
        1,
        1,
        ' Section 5 ',
        ' AVAILABLE ',
        null,
        3063
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        304,
        1,
        3,
        ' PRIVATE ',
        1,
        1,
        ' Section 5 ',
        ' AVAILABLE ',
        null,
        3064
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        305,
        1,
        3,
        ' PRIVATE ',
        1,
        1,
        ' Section 5 ',
        ' AVAILABLE ',
        null,
        3065
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        306,
        1,
        3,
        ' PRIVATE ',
        1,
        1,
        ' Section 5 ',
        ' AVAILABLE ',
        null,
        3066
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        307,
        1,
        3,
        ' PRIVATE ',
        1,
        1,
        ' Section 5 ',
        ' AVAILABLE ',
        null,
        3067
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        308,
        1,
        3,
        ' PRIVATE ',
        1,
        1,
        ' Section 5 ',
        ' AVAILABLE ',
        null,
        3068
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        309,
        1,
        3,
        ' PRIVATE ',
        1,
        1,
        ' Section 5 ',
        ' AVAILABLE ',
        null,
        3069
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        310,
        1,
        3,
        ' PRIVATE ',
        1,
        1,
        ' Section 5 ',
        ' AVAILABLE ',
        null,
        3070
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        311,
        1,
        3,
        ' PRIVATE ',
        1,
        1,
        ' Section 5 ',
        ' AVAILABLE ',
        null,
        3071
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        312,
        1,
        3,
        ' PRIVATE ',
        1,
        1,
        ' Section 5 ',
        ' AVAILABLE ',
        null,
        3072
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        313,
        1,
        3,
        ' PRIVATE ',
        1,
        1,
        ' Section 5 ',
        ' AVAILABLE ',
        null,
        3073
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        314,
        1,
        3,
        ' PRIVATE ',
        1,
        1,
        ' Section 5 ',
        ' AVAILABLE ',
        null,
        3074
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        315,
        1,
        3,
        ' PRIVATE ',
        1,
        1,
        ' Section 5 ',
        ' AVAILABLE ',
        null,
        3075
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        316,
        1,
        3,
        ' PRIVATE ',
        1,
        1,
        ' Section 5 ',
        ' AVAILABLE ',
        null,
        3076
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        317,
        1,
        3,
        ' PRIVATE ',
        1,
        1,
        ' Section 5 ',
        ' AVAILABLE ',
        null,
        3077
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        318,
        1,
        3,
        ' PRIVATE ',
        1,
        1,
        ' Section 5 ',
        ' AVAILABLE ',
        null,
        3078
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        319,
        1,
        3,
        ' PRIVATE ',
        1,
        1,
        ' Section 5 ',
        ' AVAILABLE ',
        null,
        3079
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        320,
        1,
        3,
        ' PRIVATE ',
        1,
        1,
        ' Section 5 ',
        ' AVAILABLE ',
        null,
        3080
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        321,
        1,
        3,
        ' PRIVATE ',
        1,
        1,
        ' Section 5 ',
        ' AVAILABLE ',
        null,
        3081
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        322,
        1,
        3,
        ' PRIVATE ',
        1,
        1,
        ' Section 5 ',
        ' AVAILABLE ',
        null,
        3082
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        323,
        1,
        3,
        ' PRIVATE ',
        1,
        1,
        ' Section 5 ',
        ' AVAILABLE ',
        null,
        3083
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        324,
        1,
        3,
        ' PRIVATE ',
        1,
        1,
        ' Section 5 ',
        ' AVAILABLE ',
        null,
        3084
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        325,
        1,
        3,
        ' PRIVATE ',
        1,
        1,
        ' Section 5 ',
        ' AVAILABLE ',
        null,
        3085
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        326,
        1,
        3,
        ' PRIVATE ',
        1,
        1,
        ' Section 5 ',
        ' AVAILABLE ',
        null,
        3086
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        327,
        1,
        3,
        ' PRIVATE ',
        1,
        1,
        ' Section 5 ',
        ' AVAILABLE ',
        null,
        3087
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        328,
        1,
        3,
        ' PRIVATE ',
        1,
        1,
        ' Section 5 ',
        ' AVAILABLE ',
        null,
        3088
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        329,
        1,
        3,
        ' PRIVATE ',
        1,
        1,
        ' Section 5 ',
        ' AVAILABLE ',
        null,
        3089
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        330,
        1,
        3,
        ' PRIVATE ',
        1,
        1,
        ' Section 5 ',
        ' AVAILABLE ',
        null,
        3090
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        331,
        1,
        3,
        ' PRIVATE ',
        1,
        1,
        ' Section 5 ',
        ' AVAILABLE ',
        null,
        3091
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        332,
        1,
        3,
        ' PRIVATE ',
        1,
        1,
        ' Section 5 ',
        ' AVAILABLE ',
        null,
        3092
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        333,
        1,
        3,
        ' PRIVATE ',
        1,
        1,
        ' Section 5 ',
        ' AVAILABLE ',
        null,
        3093
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        334,
        1,
        3,
        ' PRIVATE ',
        1,
        1,
        ' Section 5 ',
        ' AVAILABLE ',
        null,
        3094
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        335,
        1,
        3,
        ' PRIVATE ',
        1,
        1,
        ' Section 5 ',
        ' AVAILABLE ',
        null,
        3095
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        336,
        1,
        3,
        ' PRIVATE ',
        1,
        1,
        ' Section 5 ',
        ' AVAILABLE ',
        null,
        3096
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        337,
        1,
        3,
        ' PRIVATE ',
        1,
        1,
        ' Section 5 ',
        ' AVAILABLE ',
        null,
        3097
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        338,
        1,
        3,
        ' PRIVATE ',
        1,
        1,
        ' Section 5 ',
        ' AVAILABLE ',
        null,
        3098
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        339,
        1,
        3,
        ' PRIVATE ',
        1,
        1,
        ' Section 5 ',
        ' AVAILABLE ',
        null,
        3099
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        340,
        1,
        3,
        ' PRIVATE ',
        1,
        1,
        ' Section 5 ',
        ' AVAILABLE ',
        null,
        3100
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        341,
        1,
        3,
        ' PRIVATE ',
        1,
        1,
        ' Section 5 ',
        ' AVAILABLE ',
        null,
        3101
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        342,
        1,
        3,
        ' PRIVATE ',
        1,
        1,
        ' Section 5 ',
        ' AVAILABLE ',
        null,
        3102
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        343,
        1,
        3,
        ' PRIVATE ',
        1,
        1,
        ' Section 5 ',
        ' AVAILABLE ',
        null,
        3103
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        344,
        1,
        3,
        ' PRIVATE ',
        1,
        1,
        ' Section 5 ',
        ' AVAILABLE ',
        null,
        3104
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        348,
        1,
        3,
        ' PRIVATE ',
        1,
        1,
        ' Section 6 ',
        ' AVAILABLE ',
        null,
        3001
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        349,
        1,
        3,
        ' PRIVATE ',
        1,
        1,
        ' Section 6 ',
        ' AVAILABLE ',
        null,
        3002
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        350,
        1,
        3,
        ' PRIVATE ',
        1,
        1,
        ' Section 6 ',
        ' AVAILABLE ',
        null,
        3003
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        351,
        1,
        3,
        ' PRIVATE ',
        1,
        1,
        ' Section 6 ',
        ' AVAILABLE ',
        null,
        3004
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        352,
        1,
        3,
        ' PRIVATE ',
        1,
        1,
        ' Section 6 ',
        ' AVAILABLE ',
        null,
        3005
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        353,
        1,
        3,
        ' PRIVATE ',
        1,
        1,
        ' Section 6 ',
        ' AVAILABLE ',
        null,
        3006
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        354,
        1,
        3,
        ' PRIVATE ',
        1,
        1,
        ' Section 6 ',
        ' AVAILABLE ',
        null,
        3007
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        355,
        1,
        3,
        ' PRIVATE ',
        1,
        1,
        ' Section 6 ',
        ' AVAILABLE ',
        null,
        3008
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        356,
        1,
        3,
        ' PRIVATE ',
        1,
        1,
        ' Section 6 ',
        ' AVAILABLE ',
        null,
        3009
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        357,
        1,
        3,
        ' PRIVATE ',
        1,
        1,
        ' Section 6 ',
        ' AVAILABLE ',
        null,
        3010
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        358,
        1,
        3,
        ' PRIVATE ',
        1,
        1,
        ' Section 6 ',
        ' AVAILABLE ',
        null,
        3011
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        359,
        1,
        3,
        ' PRIVATE ',
        1,
        1,
        ' Section 6 ',
        ' AVAILABLE ',
        null,
        3012
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        360,
        1,
        3,
        ' PRIVATE ',
        1,
        1,
        ' Section 6 ',
        ' AVAILABLE ',
        null,
        3013
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        361,
        1,
        3,
        ' PRIVATE ',
        1,
        1,
        ' Section 6 ',
        ' AVAILABLE ',
        null,
        3014
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        362,
        1,
        3,
        ' PRIVATE ',
        1,
        1,
        ' Section 6 ',
        ' AVAILABLE ',
        null,
        3015
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        363,
        1,
        3,
        ' PRIVATE ',
        1,
        1,
        ' Section 6 ',
        ' AVAILABLE ',
        null,
        3016
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        364,
        1,
        3,
        ' PRIVATE ',
        1,
        1,
        ' Section 6 ',
        ' AVAILABLE ',
        null,
        3017
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        365,
        1,
        3,
        ' PRIVATE ',
        1,
        1,
        ' Section 6 ',
        ' AVAILABLE ',
        null,
        3018
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        366,
        1,
        3,
        ' PRIVATE ',
        1,
        1,
        ' Section 6 ',
        ' AVAILABLE ',
        null,
        3019
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        367,
        1,
        3,
        ' PRIVATE ',
        1,
        1,
        ' Section 6 ',
        ' AVAILABLE ',
        null,
        3020
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        368,
        1,
        3,
        ' PRIVATE ',
        1,
        1,
        ' Section 6 ',
        ' AVAILABLE ',
        null,
        3021
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        369,
        1,
        3,
        ' PRIVATE ',
        1,
        1,
        ' Section 6 ',
        ' AVAILABLE ',
        null,
        3022
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        370,
        1,
        3,
        ' PRIVATE ',
        1,
        1,
        ' Section 6 ',
        ' AVAILABLE ',
        null,
        3023
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        371,
        1,
        3,
        ' PRIVATE ',
        1,
        1,
        ' Section 6 ',
        ' AVAILABLE ',
        null,
        3024
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        372,
        1,
        3,
        ' PRIVATE ',
        1,
        1,
        ' Section 6 ',
        ' AVAILABLE ',
        null,
        3025
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        373,
        1,
        3,
        ' PRIVATE ',
        1,
        1,
        ' Section 6 ',
        ' AVAILABLE ',
        null,
        3026
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        374,
        1,
        3,
        ' PRIVATE ',
        1,
        1,
        ' Section 6 ',
        ' AVAILABLE ',
        null,
        3027
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        375,
        1,
        3,
        ' PRIVATE ',
        1,
        1,
        ' Section 6 ',
        ' AVAILABLE ',
        null,
        3028
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        376,
        1,
        3,
        ' PRIVATE ',
        1,
        1,
        ' Section 6 ',
        ' AVAILABLE ',
        null,
        3029
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        377,
        1,
        3,
        ' PRIVATE ',
        1,
        1,
        ' Section 6 ',
        ' AVAILABLE ',
        null,
        3030
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        378,
        1,
        3,
        ' PRIVATE ',
        1,
        1,
        ' Section 6 ',
        ' AVAILABLE ',
        null,
        3031
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        379,
        1,
        3,
        ' PRIVATE ',
        1,
        1,
        ' Section 6 ',
        ' AVAILABLE ',
        null,
        3032
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        380,
        1,
        3,
        ' PRIVATE ',
        1,
        1,
        ' Section 6 ',
        ' AVAILABLE ',
        null,
        3033
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        381,
        1,
        3,
        ' PRIVATE ',
        1,
        1,
        ' Section 6 ',
        ' AVAILABLE ',
        null,
        3034
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        382,
        1,
        3,
        ' PRIVATE ',
        1,
        1,
        ' Section 6 ',
        ' AVAILABLE ',
        null,
        3035
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        383,
        1,
        3,
        ' PRIVATE ',
        1,
        1,
        ' Section 6 ',
        ' AVAILABLE ',
        null,
        3036
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        384,
        1,
        3,
        ' PRIVATE ',
        1,
        1,
        ' Section 6 ',
        ' AVAILABLE ',
        null,
        3037
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        385,
        1,
        3,
        ' PRIVATE ',
        1,
        1,
        ' Section 6 ',
        ' AVAILABLE ',
        null,
        3038
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        386,
        1,
        3,
        ' PRIVATE ',
        1,
        1,
        ' Section 6 ',
        ' AVAILABLE ',
        null,
        3039
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        387,
        1,
        3,
        ' PRIVATE ',
        1,
        1,
        ' Section 6 ',
        ' AVAILABLE ',
        null,
        3040
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        388,
        1,
        3,
        ' PRIVATE ',
        1,
        1,
        ' Section 6 ',
        ' AVAILABLE ',
        null,
        3041
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        389,
        1,
        3,
        ' PRIVATE ',
        1,
        1,
        ' Section 6 ',
        ' AVAILABLE ',
        null,
        3042
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        390,
        1,
        3,
        ' PRIVATE ',
        1,
        1,
        ' Section 6 ',
        ' AVAILABLE ',
        null,
        3043
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        391,
        1,
        3,
        ' PRIVATE ',
        1,
        1,
        ' Section 6 ',
        ' AVAILABLE ',
        null,
        3044
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        411,
        1,
        3,
        ' PRIVATE ',
        1,
        1,
        ' Section 7 ',
        ' AVAILABLE ',
        null,
        3105
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        412,
        1,
        3,
        ' PRIVATE ',
        1,
        1,
        ' Section 7 ',
        ' AVAILABLE ',
        null,
        3106
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        413,
        1,
        3,
        ' PRIVATE ',
        1,
        1,
        ' Section 7 ',
        ' AVAILABLE ',
        null,
        3107
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        414,
        1,
        3,
        ' PRIVATE ',
        1,
        1,
        ' Section 7 ',
        ' AVAILABLE ',
        null,
        3108
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        415,
        1,
        3,
        ' PRIVATE ',
        1,
        1,
        ' Section 7 ',
        ' AVAILABLE ',
        null,
        3109
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        416,
        1,
        3,
        ' PRIVATE ',
        1,
        1,
        ' Section 7 ',
        ' AVAILABLE ',
        null,
        3110
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        417,
        1,
        3,
        ' PRIVATE ',
        1,
        1,
        ' Section 7 ',
        ' AVAILABLE ',
        null,
        3111
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        418,
        1,
        3,
        ' PRIVATE ',
        1,
        1,
        ' Section 7 ',
        ' AVAILABLE ',
        null,
        3112
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        419,
        1,
        3,
        ' PRIVATE ',
        1,
        1,
        ' Section 7 ',
        ' AVAILABLE ',
        null,
        3113
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        420,
        1,
        3,
        ' PRIVATE ',
        1,
        1,
        ' Section 7 ',
        ' AVAILABLE ',
        null,
        3114
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        421,
        1,
        3,
        ' PRIVATE ',
        1,
        1,
        ' Section 7 ',
        ' AVAILABLE ',
        null,
        3115
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        422,
        1,
        3,
        ' PRIVATE ',
        1,
        1,
        ' Section 7 ',
        ' AVAILABLE ',
        null,
        3116
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        423,
        1,
        3,
        ' PRIVATE ',
        1,
        1,
        ' Section 7 ',
        ' AVAILABLE ',
        null,
        3117
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        424,
        1,
        3,
        ' PRIVATE ',
        1,
        1,
        ' Section 7 ',
        ' AVAILABLE ',
        null,
        3118
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        425,
        1,
        3,
        ' PRIVATE ',
        1,
        1,
        ' Section 7 ',
        ' AVAILABLE ',
        null,
        3119
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        426,
        1,
        3,
        ' PRIVATE ',
        1,
        1,
        ' Section 7 ',
        ' AVAILABLE ',
        null,
        3120
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        427,
        1,
        3,
        ' PRIVATE ',
        1,
        1,
        ' Section 7 ',
        ' AVAILABLE ',
        null,
        3121
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        428,
        1,
        3,
        ' PRIVATE ',
        1,
        1,
        ' Section 7 ',
        ' AVAILABLE ',
        null,
        3122
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        429,
        1,
        3,
        ' PRIVATE ',
        1,
        1,
        ' Section 7 ',
        ' AVAILABLE ',
        null,
        3123
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        430,
        1,
        3,
        ' PRIVATE ',
        1,
        1,
        ' Section 7 ',
        ' AVAILABLE ',
        null,
        3124
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        431,
        1,
        3,
        ' PRIVATE ',
        1,
        1,
        ' Section 7 ',
        ' AVAILABLE ',
        null,
        3125
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        432,
        1,
        3,
        ' PRIVATE ',
        1,
        1,
        ' Section 7 ',
        ' AVAILABLE ',
        null,
        3126
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        433,
        1,
        3,
        ' PRIVATE ',
        1,
        1,
        ' Section 7 ',
        ' AVAILABLE ',
        null,
        3127
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        434,
        1,
        3,
        ' PRIVATE ',
        1,
        1,
        ' Section 7 ',
        ' AVAILABLE ',
        null,
        3128
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        435,
        1,
        3,
        ' PRIVATE ',
        1,
        1,
        ' Section 7 ',
        ' AVAILABLE ',
        null,
        3129
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        436,
        1,
        3,
        ' PRIVATE ',
        1,
        1,
        ' Section 7 ',
        ' AVAILABLE ',
        null,
        3130
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        437,
        1,
        3,
        ' PRIVATE ',
        1,
        1,
        ' Section 7 ',
        ' AVAILABLE ',
        null,
        3131
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        438,
        1,
        3,
        ' PRIVATE ',
        1,
        1,
        ' Section 7 ',
        ' AVAILABLE ',
        null,
        3132
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        439,
        1,
        3,
        ' PRIVATE ',
        1,
        1,
        ' Section 7 ',
        ' AVAILABLE ',
        null,
        3133
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        440,
        1,
        3,
        ' PRIVATE ',
        1,
        1,
        ' Section 7 ',
        ' AVAILABLE ',
        null,
        3134
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        441,
        1,
        3,
        ' PRIVATE ',
        1,
        1,
        ' Section 7 ',
        ' AVAILABLE ',
        null,
        3135
    );
INSERT IGNORE INTO cabinet (
        id,
        grid_col,
        floor,
        lent_type,
        max_user,
        grid_row,
        section,
        status,
        status_note,
        visible_num
    )
VALUES (
        442,
        1,
        3,
        ' PRIVATE ',
        1,
        1,
        ' Section 7 ',
        ' AVAILABLE ',
        null,
        3136
    );