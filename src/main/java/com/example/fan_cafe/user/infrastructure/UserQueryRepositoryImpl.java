package com.example.fan_cafe.user.infrastructure;


import com.example.fan_cafe.user.domain.QUser;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class UserQueryRepositoryImpl implements UserQueryRepository{

    private final JPAQueryFactory queryFactory;
    QUser user = QUser.user;

    @Override
    public boolean existsNickname(String nickname, Long id) {
        return queryFactory
                .selectOne()
                .from(user)
                .where(
                        user.nickname.equalsIgnoreCase(nickname),
                        user.id.ne(id), //id 가 주어진 id 와 다름
                        user.deletedAt.isNull()
                )
                .fetchFirst() != null;
    }
}
