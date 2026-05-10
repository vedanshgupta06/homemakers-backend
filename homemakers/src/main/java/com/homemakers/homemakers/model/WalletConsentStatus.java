package com.homemakers.homemakers.model;

public enum WalletConsentStatus {
    PENDING,   // user hasn't responded yet
    ACCEPTED,  // user said yes, amount reserved
    DECLINED   // user said no, pay full via gateway
}
