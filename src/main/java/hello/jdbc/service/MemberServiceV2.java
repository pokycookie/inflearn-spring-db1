package hello.jdbc.service;

import hello.jdbc.domain.Member;
import hello.jdbc.repository.MemberRepositoryV1;
import hello.jdbc.repository.MemberRepositoryV2;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

@Slf4j
@RequiredArgsConstructor
public class MemberServiceV2 {

    private final DataSource dataSource;
    private final MemberRepositoryV2 memberRepository;

    public void accountTransfer(String fromId, String toId, int money) throws SQLException {
        Connection conn = dataSource.getConnection();

        try {
            // 트랙잭션 시작
            conn.setAutoCommit(false);

            // 비즈니스 로직 실행
            Member fromMember = memberRepository.findById(conn, fromId);
            Member toMember = memberRepository.findById(conn, toId);

            memberRepository.update(conn, fromId, fromMember.getMoney() - money);
            validation(toMember);
            memberRepository.update(conn, toId, toMember.getMoney() + money);

            conn.commit(); // 성공 시 commit
        } catch (Exception e) {
            conn.rollback(); // 실패 시 rollback
            throw new IllegalStateException("이체 중 예외 발생");
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true); // 커넥션 풀로 돌아가기 전 다시 autoCommit 모드를 true 로 바꿔서 커넥션 풀에 돌려줘야 함
                    // -> 만약 커넥션 풀을 사용하지 않는 경우 close() 하면 그냥 conn 이 사라지므로, 이때는 굳이 true 로 다시 바꿀 필요는 없음
                    conn.close();
                } catch (SQLException e) {
                    log.info("error", e);
                }
            }
        }
    }

    private static void validation(Member toMember) {
        if (toMember.getMemberId().equals("ex")) {
            throw new IllegalStateException("이체 중 예외 발생");
        }
    }
}
