package com.MediConnect.EntryRelated.entities;

public enum AccountStatus {
    /**
     * Newly created accounts that still need manual review.
     */
    PENDING,

    /**
     * Accounts that successfully passed verification and can use the platform.
     */
    ACTIVE,

    /**
     * Temporarily suspended accounts awaiting further action.
     */
    ON_HOLD,

    /**
     * Permanently blocked accounts.
     */
    BANNED,

    /**
     * Accounts rejected during the manual review process.
     */
    REJECTED
}

