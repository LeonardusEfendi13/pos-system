package com.pos.posApps.Util;

import java.util.List;

public final class MenuCatalog {
    public static final String USER = "user";
    public static final String KASIR = "kasir";
    public static final String PENJUALAN = "penjualan";

    public record Entry(String id, String href) {
    }

    private static final List<Entry> ENTRIES = List.of(
            new Entry("dashboard", "/home"),
            new Entry("kasir", "/kasir"),
            new Entry("products", "/products"),
            new Entry("products.kartu-stok", "/products/kartu-stok"),
            new Entry("products.vehicle", "/products/vehicle"),
            new Entry("products.kategori", "/products/kategori"),
            new Entry("products.barang-set", "/products/barang-set"),
            new Entry("preorder.tambah", "/preorder/tambah"),
            new Entry("preorder.under-stock", "/preorder/under-stock"),
            new Entry("preorder.riwayat", "/preorder"),
            new Entry("inden.tambah", "/inden/tambah"),
            new Entry("inden.riwayat", "/inden"),
            new Entry("penjualan", "/penjualan"),
            new Entry("pembelian.tambah", "/pembelian/tambah"),
            new Entry("pembelian.riwayat", "/pembelian"),
            new Entry("laporan.nilai-persediaan", "/laporan/nilai-persediaan"),
            new Entry("laporan.penjualan", "/laporan/penjualan"),
            new Entry("laporan.penjualan.periode", "/laporan/penjualan/periode"),
            new Entry("laporan.penjualan.pelanggan", "/laporan/penjualan/pelanggan"),
            new Entry("laporan.penjualan.barang", "/laporan/penjualan/barang"),
            new Entry("laporan.pembelian", "/laporan/pembelian"),
            new Entry("laporan.pembelian.periode", "/laporan/pembelian/periode"),
            new Entry("laporan.pembelian.supplier", "/laporan/pembelian/supplier"),
            new Entry("supplier", "/supplier"),
            new Entry("customer", "/customer"),
            new Entry("user", "/user"),
            new Entry("branch.transfer.kasir", "/branch/transfer/kasir"),
            new Entry("branch.transfer.riwayat", "/branch/transfer/riwayat"),
            new Entry("branch.daftar", "/branch"),
            new Entry("data-center", "/data-center"),
            new Entry("settings", "/settings")
    );

    public static final List<String> PRODUCT_LOOKUP_KEYS = List.of(
            "kasir",
            "penjualan",
            "products",
            "products.kartu-stok",
            "preorder.tambah",
            "preorder.under-stock",
            "inden.tambah",
            "pembelian.tambah",
            "branch.transfer.kasir"
    );

    public static final List<String> CUSTOMER_LOOKUP_KEYS = List.of(
            "kasir",
            "penjualan",
            "customer",
            "branch.daftar",
            "branch.transfer.kasir"
    );

    public static final List<String> PRICE_LOOKUP_KEYS = List.of(
            "kasir",
            "penjualan",
            "products"
    );

    public static final List<String> STARTER_KEYS = List.of(KASIR, PENJUALAN);

    private MenuCatalog() {
    }

    public static List<Entry> entries() {
        return ENTRIES;
    }

    public static List<String> allKeys() {
        return ENTRIES.stream().map(Entry::id).toList();
    }

    public static boolean isKnownKey(String key) {
        return key != null && allKeys().contains(key);
    }

    public static String matchId(String pathname) {
        if (pathname == null || pathname.isBlank()) {
            return null;
        }

        Entry best = null;
        for (Entry entry : ENTRIES) {
            if (!covers(pathname, entry.href())) {
                continue;
            }
            if (best == null || entry.href().length() > best.href().length()) {
                best = entry;
            }
        }

        return best == null ? null : best.id();
    }

    public static String landingPath(boolean godAdmin, boolean superAdmin, List<String> menuKeys) {
        if (godAdmin) {
            return "/user";
        }
        if (superAdmin) {
            return "/home";
        }
        if (menuKeys != null && menuKeys.contains(KASIR)) {
            return "/kasir";
        }
        if (menuKeys != null) {
            for (Entry entry : ENTRIES) {
                if (menuKeys.contains(entry.id()) && isPage(entry.href())) {
                    return entry.href();
                }
            }
        }
        return "/kasir";
    }

    private static boolean isPage(String href) {
        return !"/laporan/penjualan".equals(href) && !"/laporan/pembelian".equals(href);
    }

    private static boolean covers(String pathname, String href) {
        return pathname.equals(href) || pathname.startsWith(href + "/");
    }
}
