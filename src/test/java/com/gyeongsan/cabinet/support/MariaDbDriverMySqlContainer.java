package com.gyeongsan.cabinet.support;

import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * 운영/테스트 DB 와 같은 방식으로, MySQL 서버에 MariaDB 드라이버(org.mariadb.jdbc)와 jdbc:mariadb URL 로 붙는 컨테이너.
 * Testcontainers 기본 MySQLContainer 는 MySQL 드라이버 클래스를 찾기 때문에 드라이버와 URL 을 재정의한다.
 */
public class MariaDbDriverMySqlContainer extends MySQLContainer<MariaDbDriverMySqlContainer> {

    public MariaDbDriverMySqlContainer(String image) {
        super(DockerImageName.parse(image));
    }

    @Override
    public String getDriverClassName() {
        return "org.mariadb.jdbc.Driver";
    }

    @Override
    public String getJdbcUrl() {
        return "jdbc:mariadb://"
                + getHost()
                + ":"
                + getMappedPort(MYSQL_PORT)
                + "/"
                + getDatabaseName();
    }
}
