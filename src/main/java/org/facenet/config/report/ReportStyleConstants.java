package org.facenet.config.report;

/**
 * Report Style Constants
 * SCALEHUB_OFFICIAL Style Profile - Official branding and styling
 */
public final class ReportStyleConstants {

    private ReportStyleConstants() {}

    /**
     * SCALEHUB_OFFICIAL Style Profile
     */
    public static final class SCALEHUB_OFFICIAL {
        
        // Font Settings
        public static final String FONT_FAMILY = "Times New Roman";
        public static final String FONT_FAMILY_FALLBACK = "Arial";
        public static final int FONT_SIZE_TITLE = 16;
        public static final int FONT_SIZE_SUBTITLE = 14;
        public static final int FONT_SIZE_BODY = 12;
        public static final int FONT_SIZE_SMALL = 11;
        public static final int FONT_SIZE_FOOTER = 10;
        
        // Number Formatting
        public static final int NUMBER_DECIMAL_PLACES = 2;
        public static final boolean NUMBER_USE_THOUSAND_SEPARATOR = true;
        public static final String NUMBER_FORMAT_PATTERN = "#,##0.00";
        public static final String NUMBER_FORMAT_INTEGER = "#,##0";
        
        // Date Formatting
        public static final String DATE_FORMAT = "dd/MM/yyyy";
        public static final String DATETIME_FORMAT = "dd/MM/yyyy HH:mm:ss";
        public static final String TIME_FORMAT = "HH:mm:ss";
        
        // Color Scheme (Professional & Subtle)
        // Colors in hex format for Excel/PDF
        public static final String COLOR_HEADER_BG = "D9E1F2";        // Light blue
        public static final String COLOR_HEADER_TEXT = "000000";      // Black
        public static final String COLOR_ZEBRA_EVEN = "F2F2F2";       // Light gray
        public static final String COLOR_ZEBRA_ODD = "FFFFFF";        // White
        public static final String COLOR_SUMMARY_BG = "E7E6E6";       // Light gray
        public static final String COLOR_SUMMARY_TEXT = "000000";     // Black
        public static final String COLOR_BORDER = "BFBFBF";           // Gray
        public static final String COLOR_ACCENT = "4472C4";           // Blue
        
        // RGB Colors for PDF (Flying Saucer)
        public static final String RGB_HEADER_BG = "rgb(217, 225, 242)";
        public static final String RGB_ZEBRA_EVEN = "rgb(242, 242, 242)";
        public static final String RGB_SUMMARY_BG = "rgb(231, 230, 230)";
        public static final String RGB_BORDER = "rgb(191, 191, 191)";
        
        // Border Settings
        public static final String BORDER_STYLE = "THIN";
        public static final String BORDER_COLOR = COLOR_BORDER;
        
        // Table Settings
        public static final int TABLE_BORDER_WIDTH = 1;
        public static final String TABLE_HEADER_ALIGNMENT = "CENTER";
        public static final boolean TABLE_HEADER_WRAP_TEXT = true;
        
        // Cell Padding (in points)
        public static final int CELL_PADDING_TOP = 4;
        public static final int CELL_PADDING_BOTTOM = 4;
        public static final int CELL_PADDING_LEFT = 8;
        public static final int CELL_PADDING_RIGHT = 8;
        
        // Row Height
        public static final float ROW_HEIGHT_HEADER = 30.0f;
        public static final float ROW_HEIGHT_DATA = 20.0f;
        public static final float ROW_HEIGHT_SUMMARY = 25.0f;
        
        // Company Branding
        public static final String COMPANY_NAME = "ScaleHub IoT System";
        public static final String COMPANY_NAME_VI = "Hệ thống Quản lý Trạm cân ScaleHub";
        public static final String WATERMARK_TEXT = "ScaleHub IoT - Confidential";
        public static final boolean SHOW_WATERMARK = false; // Can be enabled for sensitive reports
        
        // Signature Block
        public static final int SIGNATURE_FONT_SIZE = 11;
        public static final int SIGNATURE_SPACING = 50; // Spacing between signature columns
        public static final String SIGNATURE_DATE_FORMAT = "Ngày ... tháng ... năm 20...";
    }

    /**
     * Helper class for POI Excel color indices
     */
    public static final class ExcelColors {
        // Standard POI color indices
        public static final short HEADER_BG_INDEX = 44;        // Light blue
        public static final short ZEBRA_EVEN_INDEX = 22;       // Light gray
        public static final short SUMMARY_BG_INDEX = 22;       // Light gray
        public static final short ACCENT_INDEX = 12;           // Blue
    }

    /**
     * Style preset for different report types
     */
    public static final class Presets {
        
        public static final class WEIGHING_REPORT {
            public static final String TITLE = "BÁO CÁO SẢN LƯỢNG TRẠM CÂN";
            public static final String[] KEY_COLUMNS = {
                "Mã trạm", "Tên trạm", "Sản lượng", "Số lần cân"
            };
        }
        
        public static final class DAILY_SUMMARY {
            public static final String TITLE = "BÁO CÁO TỔNG HỢP NGÀY";
            public static final String[] KEY_COLUMNS = {
                "Ngày", "Tổng sản lượng", "Số lượt"
            };
        }
        
        public static final class CUSTOM_REPORT {
            public static final String TITLE = "BÁO CÁO TÙY CHỈNH";
        }
    }
}
