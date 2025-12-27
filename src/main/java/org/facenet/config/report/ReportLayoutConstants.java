package org.facenet.config.report;

/**
 * Report Layout Constants
 * Defines standard enterprise layout specifications
 */
public final class ReportLayoutConstants {

    private ReportLayoutConstants() {}

    /**
     * ENTERPRISE_STANDARD Layout Specification
     */
    public static final class ENTERPRISE_STANDARD {
        
        // Header Section
        public static final String HEADER_LOGO_POSITION = "LEFT";
        public static final String HEADER_TITLE_POSITION = "CENTER";
        public static final String HEADER_CODE_POSITION = "RIGHT";
        public static final int HEADER_HEIGHT_ROWS = 6;
        
        // Title Styling
        public static final int TITLE_FONT_SIZE = 16;
        public static final boolean TITLE_BOLD = true;
        public static final String TITLE_ALIGNMENT = "CENTER";
        
        // Metadata Section
        public static final String[] METADATA_FIELDS = {
            "exportTime",           // Thời gian xuất
            "scaleList",            // Danh sách trạm cân
            "preparedBy",           // Người thực hiện
            "dateRange"             // Khoảng thời gian
        };
        
        // Data Table Specifications
        public static final boolean TABLE_HEADER_BOLD = true;
        public static final boolean TABLE_ZEBRA_STRIPING = true;
        public static final String TABLE_NUMBER_ALIGNMENT = "RIGHT";
        public static final String TABLE_DATE_ALIGNMENT = "CENTER";
        public static final String TABLE_TEXT_ALIGNMENT = "LEFT";
        
        // Footer Section
        public static final boolean FOOTER_SHOW_SUMMARY = true;
        public static final boolean FOOTER_SHOW_SIGNATURES = true;
        public static final String[] SIGNATURE_ROLES = {
            "Người lập biểu",
            "Trưởng bộ phận",
            "Giám đốc"
        };
        public static final boolean FOOTER_SHOW_PAGE_NUMBER = true;
        public static final String PAGE_NUMBER_FORMAT = "Trang %d / %d";
        
        // Spacing
        public static final int SPACING_AFTER_HEADER = 1;
        public static final int SPACING_BEFORE_TABLE = 0;
        public static final int SPACING_AFTER_TABLE = 2;
        public static final int SPACING_BEFORE_SIGNATURE = 3;
    }

    /**
     * COMPACT Layout - For simple reports
     */
    public static final class COMPACT {
        public static final int HEADER_HEIGHT_ROWS = 3;
        public static final int TITLE_FONT_SIZE = 14;
        public static final boolean FOOTER_SHOW_SIGNATURES = false;
    }

    /**
     * DETAILED Layout - For comprehensive reports with charts
     */
    public static final class DETAILED {
        public static final int HEADER_HEIGHT_ROWS = 8;
        public static final boolean SHOW_CHARTS = true;
        public static final boolean SHOW_STATISTICS = true;
    }
}
