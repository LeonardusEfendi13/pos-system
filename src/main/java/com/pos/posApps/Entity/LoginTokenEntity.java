package com.pos.posApps.Entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "login_token")
public class LoginTokenEntity {

    @Id
    @Column(name = "token_id")
    private String tokenId;

    @Column(name = "token")
    private String token;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;


    @ManyToOne
    @JoinColumn(name = "account_id")
    private AccountEntity accountEntity;
}
