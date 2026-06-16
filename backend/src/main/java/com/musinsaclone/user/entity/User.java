package com.musinsaclone.user.entity;

import com.musinsaclone.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String name;

    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(nullable = false)
    private int point;

    public enum Role {
        USER, ADMIN
    }

    public void usePoint(int amount) {
        if (this.point < amount) throw new IllegalArgumentException("포인트 부족");
        this.point -= amount;
    }

    public void addPoint(int amount) {
        this.point += amount;
    }
}
