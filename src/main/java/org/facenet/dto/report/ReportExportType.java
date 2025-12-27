package org.facenet.dto.report;

/**
 * Enum for report export types
 */
public enum ReportExportType {
    EXCEL("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", ".xlsx"),
    WORD("application/vnd.openxmlformats-officedocument.wordprocessingml.document", ".docx"),
    PDF("application/pdf", ".pdf");

    private final String contentType;
    private final String extension;

    ReportExportType(String contentType, String extension) {
        this.contentType = contentType;
        this.extension = extension;
    }

    public String getContentType() {
        return contentType;
    }

    public String getExtension() {
        return extension;
    }
}
