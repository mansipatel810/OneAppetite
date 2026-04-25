package com.cts.mfrp.oa.model;

public enum VendorType {
    VEG,
    NON_VEG;

    public static VendorType fromString(String raw) {
        if (raw == null) return null;
        String s = raw.trim();
        if (s.isEmpty()) return null;
        String normalized = s.replace("-", "_").replace(" ", "_").toUpperCase();
        if (normalized.equals("VEG") || normalized.equals("VEGETARIAN")) return VEG;
        if (normalized.equals("NON_VEG") || normalized.equals("NONVEG") || normalized.equals("NONVEGETARIAN") || normalized.equals("NON_VEGETARIAN")) return NON_VEG;
        try {
            return VendorType.valueOf(normalized);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
