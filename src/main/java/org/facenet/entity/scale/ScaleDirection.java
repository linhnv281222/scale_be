package org.facenet.entity.scale;

/**
 * Enum for scale direction/purpose
 * Loại cân: Nhập hoặc Xuất
 */
public enum ScaleDirection {
    /**
     * Import/Inbound scale (Cân nhập)
     */
    IMPORT("Nhập", "Import"),
    
    /**
     * Export/Outbound scale (Cân xuất)
     */
    EXPORT("Xuất", "Export");

    private final String vietnameseName;
    private final String englishName;

    ScaleDirection(String vietnameseName, String englishName) {
        this.vietnameseName = vietnameseName;
        this.englishName = englishName;
    }

    public String getVietnameseName() {
        return vietnameseName;
    }

    public String getEnglishName() {
        return englishName;
    }
}
