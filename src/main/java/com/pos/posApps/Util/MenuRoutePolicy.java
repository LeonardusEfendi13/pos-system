package com.pos.posApps.Util;

import java.util.List;

public final class MenuRoutePolicy {
    public record Rule(String method, String prefix, List<String> keys) {
    }

    private static final List<Rule> RULES = buildRules();

    private MenuRoutePolicy() {
    }

    public static List<String> requiredKeys(String method, String path) {
        String normalized = normalize(path);
        if (normalized == null) {
            return null;
        }

        Rule match = null;
        for (Rule rule : RULES) {
            if (!ruleMatches(rule, method, normalized)) {
                continue;
            }
            if (match == null || rule.prefix().length() > match.prefix().length()) {
                match = rule;
            }
        }

        return match == null ? null : match.keys();
    }

    public static boolean isPublic(String path) {
        String normalized = normalize(path);
        if (normalized == null) {
            return true;
        }

        return normalized.equals("/login")
                || normalized.equals("/doLogin")
                || normalized.equals("/logout")
                || normalized.equals("/error")
                || normalized.startsWith("/uploads/")
                || normalized.startsWith("/css/")
                || normalized.startsWith("/js/")
                || normalized.startsWith("/webjars/")
                || normalized.equals("/favicon.ico");
    }

    public static boolean isSession(String path) {
        String normalized = normalize(path);
        return "/api/session".equals(normalized);
    }

    public static boolean isShellRead(String method, String path) {
        return "GET".equalsIgnoreCase(method) && "/api/client/settings".equals(normalize(path));
    }

    private static boolean ruleMatches(Rule rule, String method, String path) {
        if (rule.method() != null && (method == null || !rule.method().equalsIgnoreCase(method))) {
            return false;
        }
        return path.equals(rule.prefix()) || path.startsWith(rule.prefix() + "/");
    }

    static String normalize(String path) {
        if (path == null || path.isBlank()) {
            return null;
        }
        String value = path;
        int query = value.indexOf('?');
        if (query >= 0) {
            value = value.substring(0, query);
        }
        if (value.length() > 1 && value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }

    private static List<Rule> buildRules() {
        return List.of(
                rule(null, "/api/user", MenuCatalog.USER),
                rule(null, "/api/role", MenuCatalog.USER),
                rule(null, "/api/kasir", MenuCatalog.KASIR),
                rule(null, "/api/product/find", MenuCatalog.PRODUCT_LOOKUP_KEYS),
                rule(null, "/api/product/search", MenuCatalog.PRODUCT_LOOKUP_KEYS),
                rule(null, "/api/price-list", MenuCatalog.PRICE_LOOKUP_KEYS),
                rule(null, "/api/customer/list", MenuCatalog.CUSTOMER_LOOKUP_KEYS),
                rule(null, "/api/product-set", "products.barang-set"),
                rule(null, "/api/master/product", "products"),
                rule(null, "/api/product", "products"),
                rule(null, "/api/category", "products.kategori"),
                rule(null, "/api/vehicle", "products.vehicle"),
                rule(null, "/api/preorder/bootstrap", "preorder.tambah"),
                rule(null, "/api/preorder/add", "preorder.tambah"),
                rule(null, "/api/preorder/edit", "preorder.tambah"),
                rule(null, "/api/preorder/understock", "preorder.under-stock"),
                rule(null, "/api/preorder/list", "preorder.riwayat"),
                rule(null, "/api/preorder/delete", "preorder.riwayat"),
                rule(null, "/api/preorder", "preorder.riwayat"),
                rule(null, "/api/inden/bootstrap", "inden.tambah"),
                rule(null, "/api/inden/add", "inden.tambah"),
                rule(null, "/api/inden/edit", "inden.tambah"),
                rule(null, "/api/inden/list", "inden.riwayat"),
                rule(null, "/api/inden/delete", "inden.riwayat"),
                rule(null, "/api/inden/update_status", "inden.riwayat"),
                rule(null, "/api/inden/lunaskan", "inden.riwayat"),
                rule(null, "/api/inden", "inden.riwayat"),
                rule(null, "/api/pembelian/bootstrap", "pembelian.tambah"),
                rule(null, "/api/pembelian/add", "pembelian.tambah"),
                rule(null, "/api/pembelian/edit", "pembelian.tambah"),
                rule(null, "/api/pembelian/cek", "pembelian.tambah"),
                rule(null, "/api/pembelian/list", "pembelian.riwayat"),
                rule(null, "/api/pembelian/delete", "pembelian.riwayat"),
                rule(null, "/api/pembelian/lunaskan", "pembelian.riwayat"),
                rule(null, "/api/pembelian/batalkan-lunas", "pembelian.riwayat"),
                rule(null, "/api/pembelian/bukti", "pembelian.riwayat"),
                rule(null, "/api/pembelian", "pembelian.riwayat"),
                rule(null, "/api/penjualan", MenuCatalog.PENJUALAN),
                rule(null, "/api/laporan/nilai-persediaan", "laporan.nilai-persediaan"),
                rule(null, "/api/laporan/pendapatan/periode", "laporan.penjualan.periode"),
                rule(null, "/api/laporan/pendapatan/pelanggan", "laporan.penjualan.pelanggan"),
                rule(null, "/api/laporan/pendapatan/barang", "laporan.penjualan.barang"),
                rule(null, "/api/laporan/pengeluaran/periode", "laporan.pembelian.periode"),
                rule(null, "/api/laporan/pengeluaran/pelanggan", "laporan.pembelian.supplier"),
                rule(null, "/laporan/nilai_persediaan", "laporan.nilai-persediaan"),
                rule(null, "/laporan/pendapatan/periode", "laporan.penjualan.periode"),
                rule(null, "/laporan/pendapatan/pelanggan", "laporan.penjualan.pelanggan"),
                rule(null, "/laporan/pengeluaran/periode", "laporan.pembelian.periode"),
                rule(null, "/laporan/pengeluaran/pelanggan", "laporan.pembelian.supplier"),
                rule(null, "/api/supplier", "supplier"),
                rule(null, "/api/customer", "customer"),
                rule(null, "/api/branch/kasir", "branch.transfer.kasir"),
                rule(null, "/api/branch/transfer", "branch.transfer.riwayat"),
                rule(null, "/api/branch", "branch.daftar"),
                rule(null, "/api/backup", "data-center"),
                rule(null, "/api/restore", "data-center"),
                rule(null, "/api/data-center", "data-center"),
                rule(null, "/api/client", "settings"),
                rule(null, "/v3/dashboard", "dashboard"),
                rule(null, "/kasir", MenuCatalog.KASIR),
                rule(null, "/user", MenuCatalog.USER),
                rule(null, "/home", "dashboard")
        );
    }

    private static Rule rule(String method, String prefix, String key) {
        return new Rule(method, prefix, List.of(key));
    }

    private static Rule rule(String method, String prefix, List<String> keys) {
        return new Rule(method, prefix, keys);
    }
}
