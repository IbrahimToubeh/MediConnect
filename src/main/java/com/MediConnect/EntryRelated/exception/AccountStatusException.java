package com.MediConnect.EntryRelated.exception;

import com.MediConnect.EntryRelated.entities.AccountStatus;
import lombok.Getter;

@Getter
public class AccountStatusException extends RuntimeException {

    private final AccountStatus accountStatus;

    public AccountStatusException(AccountStatus accountStatus, String message) {
        super(message);
        this.accountStatus = accountStatus;
    }
}

