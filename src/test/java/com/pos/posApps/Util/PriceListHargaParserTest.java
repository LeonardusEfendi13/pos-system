package com.pos.posApps.Util;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PriceListHargaParserTest {
    @Test
    void parsesPlainDigitsAndIndonesianThousands() {
        assertEquals(new BigDecimal("1250000"), PriceListHargaParser.parse("1250000").value());
        assertEquals(new BigDecimal("1250000"), PriceListHargaParser.parse("1.250.000").value());
        assertEquals(new BigDecimal("1250000"), PriceListHargaParser.parse("Rp 1.250.000").value());
        assertEquals(new BigDecimal("1250000"), PriceListHargaParser.parse("Rp. 1.250.000").value());
        assertEquals(new BigDecimal("1250"), PriceListHargaParser.parse("1.250").value());
        assertEquals(new BigDecimal("1250000.5"), PriceListHargaParser.parse("1.250.000,50").value());
        assertEquals(new BigDecimal("1250.5"), PriceListHargaParser.parse("1250,5").value());
    }

    @Test
    void rejectsBlankNonNumericAndNonPositive() {
        assertEquals(PriceListHargaParser.EMPTY, PriceListHargaParser.parse("  ").error());
        assertEquals(PriceListHargaParser.NOT_A_NUMBER, PriceListHargaParser.parse("abc").error());
        assertEquals(PriceListHargaParser.NOT_A_NUMBER, PriceListHargaParser.parse("1,250,000.00").error());
        assertEquals(PriceListHargaParser.NOT_A_NUMBER, PriceListHargaParser.parse("1250.50").error());
        assertEquals(PriceListHargaParser.NOT_POSITIVE, PriceListHargaParser.parse("0").error());
        assertNull(PriceListHargaParser.parse("0").value());
    }

    @Test
    void normalizesPartCodes() {
        assertEquals("5D7-E1193-00", PriceListHargaParser.normalizeCode("  5d7-e1193-00 "));
        assertEquals("", PriceListHargaParser.normalizeCode("   "));
    }
}
