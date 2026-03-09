package com.omnisolve.service.dto;

import java.util.List;

public record DocumentAttentionResponse(
        List<PendingApprovalItem> pendingApproval,
        List<OverdueReviewItem> overdueReviews
) {
}

