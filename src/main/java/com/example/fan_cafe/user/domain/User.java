package com.example.fan_cafe.user.domain;

import com.example.fan_cafe.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.SoftDelete;

import java.util.Objects;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Entity
@Table(name= "users")
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String nickname;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column
    private String avatarUrl;

    @Column(nullable = false)
    @Builder.Default
    private String introduction = "";

    @Column(nullable=false) //마지막 변경시각
    private Long passwordUpdatedAtEpochSec;

    @Column(nullable=false) //소셜 로그인 계정 여부
    @Builder.Default
    private boolean passwordSet = true;

    void createPassword(String encodedPassword) {
        this.password = encodedPassword;
        this.passwordUpdatedAtEpochSec = System.currentTimeMillis() / 1000;
    }

    public void changePassword(String encodedPassword) {
        this.password = encodedPassword;
        this.passwordUpdatedAtEpochSec = System.currentTimeMillis() / 1000;
    }

    @Override
    public boolean equals(Object o){
        if(this == o) return true;
        if(!(o instanceof User)) return false; //비교대상이 user 가 아닐 시
        User user = (User) o;
        return id != null && id.equals(user.getId());
    }

    @Override
    public int hashCode(){
        return Objects.hash(id);
    }

    public static User of(String email, String encodedPassword, String nickname, Role role) {
        User user = User.builder()
                .email(email)
                .nickname(nickname)
                .role(role)
                .build();

        user.createPassword(encodedPassword);
        return user;
    }

    public void updateProfile(String nickname, String introduction) {
        this.nickname = nickname;
        this.introduction = introduction;
    }

}
