package com.m1_fonda.serviceUser.response;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ClientStatisticsResponse {
    private Long totalClients;
    private Long activeClients;
    private Long pendingClients;
    private Long blockedClients;
    private Long newClientsToday;
    private LocalDateTime generatedAt;
}