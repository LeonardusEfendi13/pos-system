package com.pos.posApps.DTO.Dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PagedResponse<T> {
    private List<T> content;
    private long totalElements;
    private int number;
    private int size;
    private int totalPages;
    private String role;
    private String accountName;
    /** Logged-in account id; set on endpoints that need self-identification in the client. */
    private Long accountId;
}
