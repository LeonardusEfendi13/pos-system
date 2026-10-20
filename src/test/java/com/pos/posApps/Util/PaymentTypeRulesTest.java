package com.pos.posApps.Util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PaymentTypeRulesTest {

    @Test
    void newTypesOnlyAllowTransferOrQris() {
        assertNull(PaymentTypeRules.validate("E-Wallet", "qris", null));
        assertNull(PaymentTypeRules.validate("Bank", "TRANSFER", null));
        assertEquals("Perilaku hanya bisa Transfer atau Langsung lunas",
                PaymentTypeRules.validate("Tunai 2", "cash", null));
        assertEquals("Perilaku hanya bisa Transfer atau Langsung lunas",
                PaymentTypeRules.validate("Tempo", "credit", null));
    }

    @Test
    void rejectsBlankNameAndUnknownBehavior() {
        assertEquals("Nama wajib diisi", PaymentTypeRules.validate(" ", "qris", null));
        assertEquals("Perilaku tidak valid", PaymentTypeRules.validate("X", "ewallet", null));
    }

    @Test
    void cashAndCreditTypesKeepTheirBehavior() {
        assertNull(PaymentTypeRules.validate("Tunai", "cash", "cash"));
        assertEquals("Perilaku tipe bawaan tidak bisa diubah",
                PaymentTypeRules.validate("Kredit", "qris", "credit"));
    }

    @Test
    void transferAndQrisCanSwitchButNotBecomeCashOrCredit() {
        assertNull(PaymentTypeRules.validate("E-Wallet", "transfer", "qris"));
        assertEquals("Perilaku hanya bisa Transfer atau Langsung lunas",
                PaymentTypeRules.validate("E-Wallet", "credit", "qris"));
    }

    @Test
    void deleteBlocksBuiltInAndUsedTypes() {
        assertEquals("Tipe bawaan tidak bisa dihapus", PaymentTypeRules.deleteError("cash", false));
        assertEquals("Tipe bawaan tidak bisa dihapus", PaymentTypeRules.deleteError("credit", false));
        assertEquals("Tipe masih dipakai metode pembayaran", PaymentTypeRules.deleteError("qris", true));
        assertNull(PaymentTypeRules.deleteError("transfer", false));
    }
}
