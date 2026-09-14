package com.pos.posApps.DTO.Dtos;

import lombok.Data;

@Data
public class CreateCategoryRequest {
    private String name;
    private Long parentId;
}
