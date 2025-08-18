package com.example.fan_cafe.notification.infrastructure;

import com.example.fan_cafe.notification.domain.Notification;
import com.example.fan_cafe.notification.domain.QNotification;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.DateTimePath;
import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class NotificationQueryRepositoryImpl {

    private final JPAQueryFactory qf;
    QNotification n = QNotification.notification;

    public List<Notification> findByReceiverCursor(Long receiverId,
                                                   LocalDateTime cursorAt, Long cursorId, int size){
        return qf.selectFrom(n)
                .where(n.receiverId.eq(receiverId),
                        beforeDesc(n.createdAt, n.id, cursorAt, cursorId))
                .orderBy(n.createdAt.desc(), n.id.desc())
                .limit(size + 1)
                .fetch();
    }

    private BooleanExpression beforeDesc(DateTimePath<LocalDateTime> at,
                                         NumberPath<Long> id,
                                         LocalDateTime cursorAt, Long cursorId) {
        if (cursorAt == null || cursorId == null) return null;
        // (createdAt, id) 복합 커서
        return at.lt(cursorAt).or(at.eq(cursorAt).and(id.lt(cursorId)));
    }
}
