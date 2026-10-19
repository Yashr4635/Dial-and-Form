package dev.atelierwatch.watches.dto;

import java.util.List;

public record WatchComparisonResponse(
    List<WatchDetailResponse> watches,
    int count
) {
    public static WatchComparisonResponse from(List<WatchDetailResponse> watches) {
        return new WatchComparisonResponse(watches, watches.size());
    }
}
