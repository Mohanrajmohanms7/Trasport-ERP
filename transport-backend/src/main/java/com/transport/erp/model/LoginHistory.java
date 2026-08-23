package com.transport.erp.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "login_history")
public class LoginHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private AppUser user;

    @Column(nullable = false, length = 50)
    private String username;

    @Column(name = "login_time", nullable = false)
    private LocalDateTime loginTime = LocalDateTime.now();

    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    @Column(nullable = false, length = 20)
    private String status; // SUCCESS, FAILED

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @Column(name = "logout_time")
    private java.time.LocalDateTime logoutTime;
}
