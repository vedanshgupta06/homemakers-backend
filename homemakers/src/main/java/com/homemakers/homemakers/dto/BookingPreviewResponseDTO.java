package com.homemakers.homemakers.dto;

import java.util.List;

public class BookingPreviewResponseDTO {

    private List<ServicePreviewResponseDTO> services;

    private int totalDailyMinutes;

    private double monthlyPrice;

    private boolean slotValid;

}
