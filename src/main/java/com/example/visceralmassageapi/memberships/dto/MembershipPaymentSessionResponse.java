package com.example.visceralmassageapi.memberships.dto;

public record MembershipPaymentSessionResponse(
        long purchaseId,
        String mode,
        String checkoutUrl,
        boolean requiresManualConfirmation
) {
}
