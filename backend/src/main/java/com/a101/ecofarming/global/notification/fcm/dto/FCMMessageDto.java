package com.a101.ecofarming.global.notification.fcm.dto;

import lombok.*;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Data
public class FCMMessageDto {
    private String title;
    private String body;
}