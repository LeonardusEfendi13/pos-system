package com.pos.posApps.Entity;

import jakarta.persistence.*;
import lombok.Data;

import java.sql.Timestamp;

@Entity
@Data
public class LoginTokenEntity {

    @Id
    @Column(name = "token_id")
    private String tokenId;

    @Column(name = "token")
    private String token;

    @Column(name = "created_at")
    private Timestamp createdAt;

    @ManyToOne
    @JoinColumn(name = "account_id")
    private AccountEntity accountEntity;
}
