package com.example.fan_cafe.auth.infrastructure;

import lombok.*;
import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PasswordResetPayLoad implements Serializable {
    private Long userId;
    private long issuedAtEpochSec;
    private long passwordUpdatedAtEpochSecAtIssue;
    private String purpose;

}
