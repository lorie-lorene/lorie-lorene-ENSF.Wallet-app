package com.m1_fonda.serviceUser.response;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PagedClientResponse {
    private List<ClientProfileResponse> clients;
    private int totalPages;
    private long totalElements;
    private int currentPage;
    private int size;
}