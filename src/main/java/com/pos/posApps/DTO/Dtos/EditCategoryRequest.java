package com.pos.posApps.DTO.Dtos;

import lombok.Data;

@Data
public class EditCategoryRequest {
    private Long categoryId;
    private String name;
    private Long parentId;
}
