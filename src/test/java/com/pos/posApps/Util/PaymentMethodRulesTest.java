package com.pos.posApps.Util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PaymentMethodRulesTest {
    @Test
    void requiresNameTypeAndTransferAccount() {
        assertEquals("Nama wajib diisi", PaymentMethodRules.validate(" ", "cash", "", false, null));
        assertEquals(
                "Tipe metode tidak valid",
                PaymentMethodRules.validate("Debit", "debit", "", false, null)
        );
        assertEquals(
                "Rekening wajib diisi",
                PaymentMethodRules.validate("BCA", "transfer", " ", false, null)
        );
        assertNull(PaymentMethodRules.validate("BCA", "Transfer", "123", false, null));
    }

    @Test
    void clearsRekeningUnlessTransfer() {
        assertEquals("", PaymentMethodRules.rekeningFor("cash", "123"));
        assertEquals("123", PaymentMethodRules.rekeningFor("transfer", " 123 "));
    }

    @Test
    void protectsTheDefaultCashRow() {
        assertEquals(
                "Tipe metode bawaan tidak bisa diubah",
                PaymentMethodRules.validate("Tunai", "qris", "", true, "cash")
        );
        assertNull(PaymentMethodRules.validate("Tunai", "cash", "", true, "cash"));
        assertEquals("Metode bawaan tidak bisa dihapus", PaymentMethodRules.deleteError(true));
        assertNull(PaymentMethodRules.deleteError(false));
    }
}
