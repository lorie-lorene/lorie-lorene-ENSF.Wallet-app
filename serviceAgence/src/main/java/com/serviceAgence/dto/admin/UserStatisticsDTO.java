package com.serviceAgence.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserStatisticsDTO {
    private long totalUsers;
    private long activeUsers;
    private long suspendedUsers;
    private long inactiveUsers;
    private long recentlyActiveUsers;
    private long adminUsers;
    private long supervisorUsers;
    private long agenceUsers;
    private LocalDateTime generatedAt;
}