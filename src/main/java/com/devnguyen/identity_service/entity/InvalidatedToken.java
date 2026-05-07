package com.devnguyen.identity_service.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.Date;

/*
 Bảng này dùng để blacklist JWT token đã logout.
 */
@Entity
@Table(name = "invalidated_token")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvalidatedToken {
    @Id
    private String id; // chính là jti của JWT
    private Date expiryTime;
}