package com.pos.posApps.DTO.Dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ResponseForWhatsapp {
    private boolean status;
    private String message;
    private boolean openWa;
    private String phoneNumber;
    private String waMessage;
}
