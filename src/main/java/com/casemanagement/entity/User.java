package com.casemanagement.entity;

import com.casemanagement.enums.UserRole;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;
import java.util.UUID;

@Entity
@Table(name = "users", uniqueConstraints = { @UniqueConstraint(name = "uk_users_email", columnNames = "email")})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id
    @UuidGenerator
    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "id", nullable = false, updatable = false, columnDefinition = "BINARY(16)")
    private UUID id;

    @NotBlank
    @Email
    @Setter
    @Column(name = "email", nullable = false, length = 255)
    private String email;

    @NotBlank
    @Setter
    @Column(name = "password", nullable = false, length = 255)
    private String password;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Setter
    @Column(name = "role", nullable = false, length = 30)
    private UserRole role;

    public User(String email, String password, UserRole role) {
        this.email = email;
        this.password = password;
        this.role = role;
    }
}

