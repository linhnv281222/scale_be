
# `db_diagram.md`

## ScaleHub IoT – Database Design Specification (v1.1 – Audit & Time-aware)

---

## 0. Quy ước chung (RẤT QUAN TRỌNG)

### 0.1. Audit fields (áp dụng cho **tất cả các bảng**)

Mỗi bảng đều có:
```
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by varchar(256),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by varchar(256),
```

> `created_by / updated_by`:

* NULL nếu thao tác bởi system / engine
* Có giá trị nếu thao tác từ UI / API

---

### 0.2. Thời gian đọc dữ liệu thực tế

* Các bảng **realtime / history** có thêm:

```sql
last_time TIMESTAMPTZ NOT NULL
```

`last_time` = **thời điểm thiết bị trả dữ liệu**,
KHÔNG phải thời điểm insert DB.

---

## 1. ERD (cập nhật)

```mermaid
erDiagram
    USERS ||--o{ USER_ROLES : has
    ROLES ||--o{ USER_ROLES : assigned
    ROLES ||--o{ ROLE_PERMISSIONS : grants
    PERMISSIONS ||--o{ ROLE_PERMISSIONS : included

    LOCATIONS ||--o{ LOCATIONS : parent
    LOCATIONS ||--o{ SCALES : contains

    SCALES ||--|| SCALE_CONFIGS : configured_by
    SCALES ||--|| SCALE_CURRENT_STATES : has
    SCALES ||--o{ WEIGHING_LOGS : produces
    SCALES ||--o{ SCALE_DAILY_REPORTS : aggregates

    SHIFTS ||--o{ SCALE_CURRENT_STATES : tagged_by
    SHIFTS ||--o{ WEIGHING_LOGS : tagged_by
```

---

## 2. Table Definitions (FULL)

---

## A. Users & RBAC

### `users`

```sql
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by varchar(256),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by varchar(256),
    username VARCHAR(50) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(100),
    status SMALLINT DEFAULT 1,
);
```

---

### `roles`

```sql
CREATE TABLE roles (
    id SERIAL PRIMARY KEY,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by varchar(256),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by varchar(256),
    name VARCHAR(50) NOT NULL,
    code VARCHAR(50) UNIQUE NOT NULL,
);
```

---

### `permissions`

```sql
CREATE TABLE permissions (
    id SERIAL PRIMARY KEY,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by varchar(256),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by varchar(256),
    code VARCHAR(100) UNIQUE NOT NULL,
    description TEXT,
);
```

---

### `user_roles`

```sql
CREATE TABLE user_roles (
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by varchar(256),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by varchar(256),
    user_id BIGINT REFERENCES users(id) ON DELETE CASCADE,
    role_id INT REFERENCES roles(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);
```

---

### `role_permissions`

```sql
CREATE TABLE role_permissions (
    role_id INT REFERENCES roles(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by varchar(256),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by varchar(256),
    permission_id INT REFERENCES permissions(id) ON DELETE CASCADE,

    PRIMARY KEY (role_id, permission_id)
);
```

---

## B. Location

### `locations`

```sql
CREATE TABLE locations (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by varchar(256),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by varchar(256),
    code VARCHAR(20) UNIQUE NOT NULL,
    name VARCHAR(100),
    parent_id BIGINT REFERENCES locations(id),
);

CREATE INDEX idx_locations_parent ON locations(parent_id);
```

---

## C. Scales & Config

### `scales`

```sql
CREATE TABLE scales (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by varchar(256),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by varchar(256),
    name VARCHAR(100) NOT NULL,
    location_id BIGINT REFERENCES locations(id),
    model VARCHAR(50),
    is_active BOOLEAN DEFAULT true,
);

CREATE INDEX idx_scales_location ON scales(location_id);
```

---

### `scale_configs`

```sql
CREATE TABLE scale_configs (
    scale_id BIGINT PRIMARY KEY REFERENCES scales(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by varchar(256),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by varchar(256),
    protocol VARCHAR(20) NOT NULL,
    poll_interval INT DEFAULT 1000,
    conn_params JSONB NOT NULL,

    data_1 JSONB,
    data_2 JSONB,
    data_3 JSONB,
    data_4 JSONB,
    data_5 JSONB,
);
```

---

## D. Realtime State

### `scale_current_states`

```sql
CREATE TABLE scale_current_states (
    scale_id BIGINT PRIMARY KEY REFERENCES scales(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by varchar(256),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by varchar(256),
    data_1 varchar(256),
    data_2 varchar(256),
    data_3 varchar(256),
    data_4 varchar(256),
    data_5 varchar(256),
    status VARCHAR(20),
    shift_id BIGINT REFERENCES shifts(id),
    last_time TIMESTAMPTZ NOT NULL,
);
```

---

## E. Historical Logs

### `weighing_logs`

```sql
CREATE TABLE weighing_logs (
    scale_id BIGINT REFERENCES scales(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by varchar(256),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by varchar(256),
    last_time TIMESTAMPTZ NOT NULL,

    shift_id BIGINT REFERENCES shifts(id),

    data_1 varchar(256),
    data_2 varchar(256),
    data_3 varchar(256),
    data_4 varchar(256),
    data_5 varchar(256),
    PRIMARY KEY (scale_id, created_at)
) PARTITION BY RANGE (created_at);

---

## F. Shift Management

### `shifts`

```sql
CREATE TABLE shifts (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by varchar(256),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by varchar(256),
    code VARCHAR(50) UNIQUE NOT NULL,
    name VARCHAR(100),
    start_time TIME,
    end_time TIME,
    is_active BOOLEAN DEFAULT true
);
```
```

Index:

```sql
CREATE INDEX idx_logs_scale_time
ON weighing_logs (scale_id, created_at DESC);
```

---

## F. Daily Aggregation

### `scale_daily_reports`

```sql
CREATE TABLE scale_daily_reports (
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by varchar(256),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by varchar(256),
    date DATE NOT NULL,
    scale_id BIGINT REFERENCES scales(id),

    data_1 varchar(256),
    data_2 varchar(256),
    data_3 varchar(256),
    data_4 varchar(256),
    data_5 varchar(256),
    last_time TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (date, scale_id)
);
```

---

## G. Report Configuration (Dynamic)

### `organization_settings`
Cấu hình thông tin công ty và branding

```sql
CREATE TABLE organization_settings (
    id SERIAL PRIMARY KEY,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by varchar(256),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by varchar(256),
    
    -- Company info
    company_name VARCHAR(255) NOT NULL,
    company_name_en VARCHAR(255),
    address TEXT,
    phone VARCHAR(50),
    email VARCHAR(100),
    website VARCHAR(255),
    tax_code VARCHAR(50),
    
    -- Branding
    logo_url TEXT,
    logo_data BYTEA,  -- Binary data cho logo
    watermark_text VARCHAR(100),
    
    -- Default settings
    is_active BOOLEAN DEFAULT true,
    is_default BOOLEAN DEFAULT false
);
```

---

### `report_templates`
Template động cho các loại báo cáo

```sql
CREATE TABLE report_templates (
    id SERIAL PRIMARY KEY,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by varchar(256),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by varchar(256),
    
    -- Template info
    code VARCHAR(50) UNIQUE NOT NULL,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    report_type VARCHAR(20) NOT NULL,  -- EXCEL, WORD, PDF
    
    -- Configuration
    title_template VARCHAR(500),  -- e.g. "BÁO CÁO SẢN LƯỢNG - {{dateRange}}"
    
    -- Styling (JSONB for flexibility)
    header_config JSONB,  -- {showLogo, showCompanyInfo, headerColor, etc.}
    footer_config JSONB,  -- {showPageNumber, footerText, signatureFields, etc.}
    table_config JSONB,   -- {headerColor, borderStyle, fontSize, etc.}
    
    -- Status
    is_active BOOLEAN DEFAULT true,
    is_default BOOLEAN DEFAULT false,
    
    CONSTRAINT chk_report_type CHECK (report_type IN ('EXCEL', 'WORD', 'PDF'))
);

CREATE INDEX idx_report_templates_type ON report_templates(report_type, is_active);
```

---

### `report_columns`
Cấu hình động các cột trong báo cáo

```sql
CREATE TABLE report_columns (
    id SERIAL PRIMARY KEY,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by varchar(256),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by varchar(256),
    
    template_id INT REFERENCES report_templates(id) ON DELETE CASCADE,
    
    -- Column definition
    column_key VARCHAR(50) NOT NULL,     -- e.g. "data_1", "scale_name"
    column_label VARCHAR(200) NOT NULL,   -- e.g. "Khối lượng (kg)"
    column_order INT NOT NULL,
    
    -- Data mapping
    data_source VARCHAR(50) NOT NULL,     -- SCALE_INFO, WEIGHING_DATA, CALCULATED
    data_field VARCHAR(50),               -- Field name in source
    data_type VARCHAR(20),                -- STRING, NUMBER, DATE, BOOLEAN
    
    -- Formatting
    format_pattern VARCHAR(100),          -- e.g. "#,##0.00" for numbers
    aggregation_type VARCHAR(20),         -- SUM, AVG, MAX, MIN, COUNT, NONE
    
    -- Display
    is_visible BOOLEAN DEFAULT true,
    width INT,                            -- Column width (for Excel/Word)
    alignment VARCHAR(10),                -- LEFT, CENTER, RIGHT
    
    CONSTRAINT chk_data_source CHECK (data_source IN ('SCALE_INFO', 'WEIGHING_DATA', 'CALCULATED')),
    CONSTRAINT chk_aggregation CHECK (aggregation_type IN ('SUM', 'AVG', 'MAX', 'MIN', 'COUNT', 'NONE'))
);

CREATE INDEX idx_report_columns_template ON report_columns(template_id, column_order);
```

---

### `report_export_logs`
Audit trail cho việc xuất báo cáo

```sql
CREATE TABLE report_export_logs (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by varchar(256),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by varchar(256),
    
    template_id INT REFERENCES report_templates(id),
    
    -- Request info
    export_type VARCHAR(20) NOT NULL,
    start_time TIMESTAMPTZ NOT NULL,
    end_time TIMESTAMPTZ NOT NULL,
    scale_ids JSONB,                      -- Array of scale IDs
    
    -- Result
    file_name VARCHAR(500),
    file_size BIGINT,
    record_count INT,
    status VARCHAR(20) NOT NULL,          -- SUCCESS, FAILED, TIMEOUT
    error_message TEXT,
    
    -- Performance
    execution_time_ms BIGINT,
    
    CONSTRAINT chk_export_status CHECK (status IN ('SUCCESS', 'FAILED', 'TIMEOUT'))
);

CREATE INDEX idx_export_logs_created ON report_export_logs(created_at DESC);
CREATE INDEX idx_export_logs_user ON report_export_logs(created_by, created_at DESC);
```

---

## 3. Design Notes (Enterprise-grade)

* `last_time` ≠ `created_at`
* **Dynamic Configuration**: Report templates được cấu hình hoàn toàn động qua JSONB
* **Multi-tenant Ready**: `organization_settings` cho phép multiple organizations
* **Flexible Columns**: `report_columns` mapping động, không hardcode
* **Audit Trail**: `report_export_logs` theo dõi toàn bộ lịch sử xuất báo cáo

``` json
{
    "success": true,
    "data": {
        "data": [
            {
                "interval": "SHIFT",
                "fromDate": "2026-01-14T07:55:15.176Z",
                "toDate": "2026-01-21T16:59:59.999Z",
                "dataFieldNames": {
                    "data_5": {
                        "name": "",
                        "unit": ""
                    },
                    "data_2": {
                        "name": "Lưu lượng",
                        "unit": ""
                    },
                    "data_1": {
                        "name": "Khối lượng tích lũy",
                        "unit": "kg"
                    },
                    "data_3": {
                        "name": "Tốc độ",
                        "unit": ""
                    }
                },
                "aggregationByField": {
                    "data_5": "AVG",
                    "data_2": "ABS",
                    "data_1": "SUM",
                    "data_4": "ABS",
                    "data_3": "ABS"
                },
                "ratioFormula": "data_1/data_3",
                "overview": {
                    "1": {
                        "data_2": {
                            "value": "841524571.75",
                            "aggregation": "AVG",
                            "name": "Lưu lượng",
                            "unit": "",
                            "used": true
                        },
                        "data_1": {
                            "value": "13223531566.00",
                            "aggregation": "SUM",
                            "name": "Khối lượng",
                            "unit": "kg",
                            "used": true
                        },
                        "data_3": {
                            "value": "841524571.75",
                            "aggregation": "AVG",
                            "name": "Tốc độ",
                            "unit": "",
                            "used": true
                        }
                    }
                },
                "rows": [
                    {
                        "scale": {
                            "id": 1,
                            "name": "Cân 01",
                            "model": "MODEL 1",
                            "direction": "IMPORT",
                            "location": {
                                "id": 1,
                                "code": "VT01",
                                "name": "Kho "
                            },
                            "is_active": true,
                            "created_at": "2025-12-30T01:50:06.746173Z",
                            "created_by": "admin",
                            "updated_at": "2026-01-14T01:33:14.935524Z",
                            "updated_by": "admin"
                        },
                        "period": "2026-01-15 CA2",
                        "recordCount": 2674,
                        "direction": 1,
                        "ratio": {
                            "value": "1.0000",
                            "formula": "data_1/data_3"
                        },
                        "start_time": "2026-01-15 13:30:00",
                        "end_time": "2026-01-15 21:30:28",
                        "start_values": {
                            "data_5": {
                                "value": "0",
                                "name": "Data 5",
                                "unit": "",
                                "used": false
                            },
                            "data_2": {
                                "value": "1583242846",
                                "name": "Lưu lượng",
                                "unit": "",
                                "used": true
                            },
                            "data_1": {
                                "value": "1583242846",
                                "name": "Khối lượng",
                                "unit": "kg",
                                "used": true
                            },
                            "data_3": {
                                "value": "1583242846",
                                "name": "Tốc độ",
                                "unit": "",
                                "used": true
                            }
                        },
                        "end_values": {
                            "data_5": {
                                "value": "0",
                                "name": "Data 5",
                                "unit": "",
                                "used": false
                            },
                            "data_2": {
                                "value": "1934521166",
                                "name": "Lưu lượng",
                                "unit": "",
                                "used": true
                            },
                            "data_1": {
                                "value": "1934521166",
                                "name": "Khối lượng",
                                "unit": "kg",
                                "used": true
                            },
                            "data_3": {
                                "value": "1934521166",
                                "name": "Tốc độ",
                                "unit": "",
                                "used": true
                            }
                        },
                        "data_values": {
                            "data_5": {
                                "value": "0",
                                "name": "Data 5",
                                "unit": "",
                                "used": false
                            },
                            "data_2": {
                                "value": "351278320",
                                "name": "Lưu lượng",
                                "unit": "",
                                "used": true
                            },
                            "data_1": {
                                "value": "351278320.00",
                                "name": "Khối lượng",
                                "unit": "kg",
                                "used": true
                            },
                            "data_3": {
                                "value": "351278320",
                                "name": "Tốc độ",
                                "unit": "",
                                "used": true
                            }
                        }
                    },
                    {
                        "scale": {
                            "id": 1,
                            "name": "Cân 01",
                            "model": "MODEL 1",
                            "direction": "IMPORT",
                            "location": {
                                "id": 1,
                                "code": "VT01",
                                "name": "Kho "
                            },
                            "is_active": true,
                            "created_at": "2025-12-30T01:50:06.746173Z",
                            "created_by": "admin",
                            "updated_at": "2026-01-14T01:33:14.935524Z",
                            "updated_by": "admin"
                        },
                        "period": "2026-01-15 CA3",
                        "recordCount": 898,
                        "direction": 1,
                        "ratio": {
                            "value": "33.2296",
                            "formula": "data_1/data_3"
                        },
                        "start_time": "2026-01-15 21:30:00",
                        "end_time": "2026-01-16 05:30:00",
                        "start_values": {
                            "data_5": {
                                "value": "0",
                                "name": "Data 5",
                                "unit": "",
                                "used": false
                            },
                            "data_2": {
                                "value": "1934127944",
                                "name": "Lưu lượng",
                                "unit": "",
                                "used": true
                            },
                            "data_1": {
                                "value": "1934127944",
                                "name": "Khối lượng",
                                "unit": "kg",
                                "used": true
                            },
                            "data_3": {
                                "value": "1934127944",
                                "name": "Tốc độ",
                                "unit": "",
                                "used": true
                            }
                        },
                        "end_values": {
                            "data_5": {
                                "value": "0",
                                "name": "Data 5",
                                "unit": "",
                                "used": false
                            },
                            "data_2": {
                                "value": "-1983673917",
                                "name": "Lưu lượng",
                                "unit": "",
                                "used": true
                            },
                            "data_1": {
                                "value": "-1983673917",
                                "name": "Khối lượng",
                                "unit": "kg",
                                "used": true
                            },
                            "data_3": {
                                "value": "-1983673917",
                                "name": "Tốc độ",
                                "unit": "",
                                "used": true
                            }
                        },
                        "data_values": {
                            "data_5": {
                                "value": "0",
                                "name": "Data 5",
                                "unit": "",
                                "used": false
                            },
                            "data_2": {
                                "value": "117901063",
                                "name": "Lưu lượng",
                                "unit": "",
                                "used": true
                            },
                            "data_1": {
                                "value": "3917801861.00",
                                "name": "Khối lượng",
                                "unit": "kg",
                                "used": true
                            },
                            "data_3": {
                                "value": "117901063",
                                "name": "Tốc độ",
                                "unit": "",
                                "used": true
                            }
                        }
                    },
                    {
                        "scale": {
                            "id": 1,
                            "name": "Cân 01",
                            "model": "MODEL 1",
                            "direction": "IMPORT",
                            "location": {
                                "id": 1,
                                "code": "VT01",
                                "name": "Kho "
                            },
                            "is_active": true,
                            "created_at": "2025-12-30T01:50:06.746173Z",
                            "created_by": "admin",
                            "updated_at": "2026-01-14T01:33:14.935524Z",
                            "updated_by": "admin"
                        },
                        "period": "2026-01-16 CA1",
                        "recordCount": 2882,
                        "direction": 1,
                        "ratio": {
                            "value": "1.0000",
                            "formula": "data_1/data_3"
                        },
                        "start_time": "2026-01-16 05:30:00",
                        "end_time": "2026-01-16 13:30:00",
                        "start_values": {
                            "data_5": {
                                "value": "0",
                                "name": "Data 5",
                                "unit": "",
                                "used": false
                            },
                            "data_2": {
                                "value": "-1983673917",
                                "name": "Lưu lượng",
                                "unit": "",
                                "used": true
                            },
                            "data_1": {
                                "value": "-1983673917",
                                "name": "Khối lượng",
                                "unit": "kg",
                                "used": true
                            },
                            "data_3": {
                                "value": "-1983673917",
                                "name": "Tốc độ",
                                "unit": "",
                                "used": true
                            }
                        },
                        "end_values": {
                            "data_5": {
                                "value": "0",
                                "name": "Data 5",
                                "unit": "",
                                "used": false
                            },
                            "data_2": {
                                "value": "6422626",
                                "name": "Lưu lượng",
                                "unit": "",
                                "used": true
                            },
                            "data_1": {
                                "value": "6422626",
                                "name": "Khối lượng",
                                "unit": "kg",
                                "used": true
                            },
                            "data_3": {
                                "value": "6422626",
                                "name": "Tốc độ",
                                "unit": "",
                                "used": true
                            }
                        },
                        "data_values": {
                            "data_5": {
                                "value": "0",
                                "name": "Data 5",
                                "unit": "",
                                "used": false
                            },
                            "data_2": {
                                "value": "1990096543",
                                "name": "Lưu lượng",
                                "unit": "",
                                "used": true
                            },
                            "data_1": {
                                "value": "1990096543.00",
                                "name": "Khối lượng",
                                "unit": "kg",
                                "used": true
                            },
                            "data_3": {
                                "value": "1990096543",
                                "name": "Tốc độ",
                                "unit": "",
                                "used": true
                            }
                        }
                    },
                    {
                        "scale": {
                            "id": 1,
                            "name": "Cân 01",
                            "model": "MODEL 1",
                            "direction": "IMPORT",
                            "location": {
                                "id": 1,
                                "code": "VT01",
                                "name": "Kho "
                            },
                            "is_active": true,
                            "created_at": "2025-12-30T01:50:06.746173Z",
                            "created_by": "admin",
                            "updated_at": "2026-01-14T01:33:14.935524Z",
                            "updated_by": "admin"
                        },
                        "period": "2026-01-16 CA2",
                        "recordCount": 2884,
                        "direction": 1,
                        "ratio": {
                            "value": "1.0000",
                            "formula": "data_1/data_3"
                        },
                        "start_time": "2026-01-16 13:30:00",
                        "end_time": "2026-01-16 21:30:28",
                        "start_values": {
                            "data_5": {
                                "value": "0",
                                "name": "Data 5",
                                "unit": "",
                                "used": false
                            },
                            "data_2": {
                                "value": "6422626",
                                "name": "Lưu lượng",
                                "unit": "",
                                "used": true
                            },
                            "data_1": {
                                "value": "6422626",
                                "name": "Khối lượng",
                                "unit": "kg",
                                "used": true
                            },
                            "data_3": {
                                "value": "6422626",
                                "name": "Tốc độ",
                                "unit": "",
                                "used": true
                            }
                        },
                        "end_values": {
                            "data_5": {
                                "value": "0",
                                "name": "Data 5",
                                "unit": "",
                                "used": false
                            },
                            "data_2": {
                                "value": "384046820",
                                "name": "Lưu lượng",
                                "unit": "",
                                "used": true
                            },
                            "data_1": {
                                "value": "384046820",
                                "name": "Khối lượng",
                                "unit": "kg",
                                "used": true
                            },
                            "data_3": {
                                "value": "384046820",
                                "name": "Tốc độ",
                                "unit": "",
                                "used": true
                            }
                        },
                        "data_values": {
                            "data_5": {
                                "value": "0",
                                "name": "Data 5",
                                "unit": "",
                                "used": false
                            },
                            "data_2": {
                                "value": "377624194",
                                "name": "Lưu lượng",
                                "unit": "",
                                "used": true
                            },
                            "data_1": {
                                "value": "377624194.00",
                                "name": "Khối lượng",
                                "unit": "kg",
                                "used": true
                            },
                            "data_3": {
                                "value": "377624194",
                                "name": "Tốc độ",
                                "unit": "",
                                "used": true
                            }
                        }
                    },
                    {
                        "scale": {
                            "id": 1,
                            "name": "Cân 01",
                            "model": "MODEL 1",
                            "direction": "IMPORT",
                            "location": {
                                "id": 1,
                                "code": "VT01",
                                "name": "Kho "
                            },
                            "is_active": true,
                            "created_at": "2025-12-30T01:50:06.746173Z",
                            "created_by": "admin",
                            "updated_at": "2026-01-14T01:33:14.935524Z",
                            "updated_by": "admin"
                        },
                        "period": "2026-01-16 CA3",
                        "recordCount": 2878,
                        "direction": 1,
                        "ratio": {
                            "value": "0.0242",
                            "formula": "data_1/data_3"
                        },
                        "start_time": "2026-01-16 21:30:00",
                        "end_time": "2026-01-17 05:30:00",
                        "start_values": {
                            "data_5": {
                                "value": "0",
                                "name": "Data 5",
                                "unit": "",
                                "used": false
                            },
                            "data_2": {
                                "value": "383653598",
                                "name": "Lưu lượng",
                                "unit": "",
                                "used": true
                            },
                            "data_1": {
                                "value": "383653598",
                                "name": "Khối lượng",
                                "unit": "kg",
                                "used": true
                            },
                            "data_3": {
                                "value": "383653598",
                                "name": "Tốc độ",
                                "unit": "",
                                "used": true
                            }
                        },
                        "end_values": {
                            "data_5": {
                                "value": "0",
                                "name": "Data 5",
                                "unit": "",
                                "used": false
                            },
                            "data_2": {
                                "value": "279711916",
                                "name": "Lưu lượng",
                                "unit": "",
                                "used": true
                            },
                            "data_1": {
                                "value": "279711916",
                                "name": "Khối lượng",
                                "unit": "kg",
                                "used": true
                            },
                            "data_3": {
                                "value": "279711916",
                                "name": "Tốc độ",
                                "unit": "",
                                "used": true
                            }
                        },
                        "data_values": {
                            "data_5": {
                                "value": "0",
                                "name": "Data 5",
                                "unit": "",
                                "used": false
                            },
                            "data_2": {
                                "value": "4294836222",
                                "name": "Lưu lượng",
                                "unit": "",
                                "used": true
                            },
                            "data_1": {
                                "value": "103941682.00",
                                "name": "Khối lượng",
                                "unit": "kg",
                                "used": true
                            },
                            "data_3": {
                                "value": "4294836222",
                                "name": "Tốc độ",
                                "unit": "",
                                "used": true
                            }
                        }
                    },
                    {
                        "scale": {
                            "id": 1,
                            "name": "Cân 01",
                            "model": "MODEL 1",
                            "direction": "IMPORT",
                            "location": {
                                "id": 1,
                                "code": "VT01",
                                "name": "Kho "
                            },
                            "is_active": true,
                            "created_at": "2025-12-30T01:50:06.746173Z",
                            "created_by": "admin",
                            "updated_at": "2026-01-14T01:33:14.935524Z",
                            "updated_by": "admin"
                        },
                        "period": "2026-01-17 CA1",
                        "recordCount": 2882,
                        "direction": 1,
                        "ratio": {
                            "value": "0.2356",
                            "formula": "data_1/data_3"
                        },
                        "start_time": "2026-01-17 05:30:00",
                        "end_time": "2026-01-17 13:30:00",
                        "start_values": {
                            "data_5": {
                                "value": "0",
                                "name": "Data 5",
                                "unit": "",
                                "used": false
                            },
                            "data_2": {
                                "value": "279711916",
                                "name": "Lưu lượng",
                                "unit": "",
                                "used": true
                            },
                            "data_1": {
                                "value": "279711916",
                                "name": "Khối lượng",
                                "unit": "kg",
                                "used": true
                            },
                            "data_3": {
                                "value": "279711916",
                                "name": "Tốc độ",
                                "unit": "",
                                "used": true
                            }
                        },
                        "end_values": {
                            "data_5": {
                                "value": "0",
                                "name": "Data 5",
                                "unit": "",
                                "used": false
                            },
                            "data_2": {
                                "value": "163973574",
                                "name": "Lưu lượng",
                                "unit": "",
                                "used": true
                            },
                            "data_1": {
                                "value": "163973574",
                                "name": "Khối lượng",
                                "unit": "kg",
                                "used": true
                            },
                            "data_3": {
                                "value": "163973574",
                                "name": "Tốc độ",
                                "unit": "",
                                "used": true
                            }
                        },
                        "data_values": {
                            "data_5": {
                                "value": "0",
                                "name": "Data 5",
                                "unit": "",
                                "used": false
                            },
                            "data_2": {
                                "value": "491330889",
                                "name": "Lưu lượng",
                                "unit": "",
                                "used": true
                            },
                            "data_1": {
                                "value": "115738342.00",
                                "name": "Khối lượng",
                                "unit": "kg",
                                "used": true
                            },
                            "data_3": {
                                "value": "491330889",
                                "name": "Tốc độ",
                                "unit": "",
                                "used": true
                            }
                        }
                    },
                    {
                        "scale": {
                            "id": 1,
                            "name": "Cân 01",
                            "model": "MODEL 1",
                            "direction": "IMPORT",
                            "location": {
                                "id": 1,
                                "code": "VT01",
                                "name": "Kho "
                            },
                            "is_active": true,
                            "created_at": "2025-12-30T01:50:06.746173Z",
                            "created_by": "admin",
                            "updated_at": "2026-01-14T01:33:14.935524Z",
                            "updated_by": "admin"
                        },
                        "period": "2026-01-17 CA2",
                        "recordCount": 2884,
                        "direction": 1,
                        "ratio": {
                            "value": "1.0000",
                            "formula": "data_1/data_3"
                        },
                        "start_time": "2026-01-17 13:30:00",
                        "end_time": "2026-01-17 21:30:28",
                        "start_values": {
                            "data_5": {
                                "value": "0",
                                "name": "Data 5",
                                "unit": "",
                                "used": false
                            },
                            "data_2": {
                                "value": "163973574",
                                "name": "Lưu lượng",
                                "unit": "",
                                "used": true
                            },
                            "data_1": {
                                "value": "163973574",
                                "name": "Khối lượng",
                                "unit": "kg",
                                "used": true
                            },
                            "data_3": {
                                "value": "163973574",
                                "name": "Tốc độ",
                                "unit": "",
                                "used": true
                            }
                        },
                        "end_values": {
                            "data_5": {
                                "value": "0",
                                "name": "Data 5",
                                "unit": "",
                                "used": false
                            },
                            "data_2": {
                                "value": "541532231",
                                "name": "Lưu lượng",
                                "unit": "",
                                "used": true
                            },
                            "data_1": {
                                "value": "541532231",
                                "name": "Khối lượng",
                                "unit": "kg",
                                "used": true
                            },
                            "data_3": {
                                "value": "541532231",
                                "name": "Tốc độ",
                                "unit": "",
                                "used": true
                            }
                        },
                        "data_values": {
                            "data_5": {
                                "value": "0",
                                "name": "Data 5",
                                "unit": "",
                                "used": false
                            },
                            "data_2": {
                                "value": "377558657",
                                "name": "Lưu lượng",
                                "unit": "",
                                "used": true
                            },
                            "data_1": {
                                "value": "377558657.00",
                                "name": "Khối lượng",
                                "unit": "kg",
                                "used": true
                            },
                            "data_3": {
                                "value": "377558657",
                                "name": "Tốc độ",
                                "unit": "",
                                "used": true
                            }
                        }
                    },
                    {
                        "scale": {
                            "id": 1,
                            "name": "Cân 01",
                            "model": "MODEL 1",
                            "direction": "IMPORT",
                            "location": {
                                "id": 1,
                                "code": "VT01",
                                "name": "Kho "
                            },
                            "is_active": true,
                            "created_at": "2025-12-30T01:50:06.746173Z",
                            "created_by": "admin",
                            "updated_at": "2026-01-14T01:33:14.935524Z",
                            "updated_by": "admin"
                        },
                        "period": "2026-01-17 CA3",
                        "recordCount": 2878,
                        "direction": 1,
                        "ratio": {
                            "value": "0.5908",
                            "formula": "data_1/data_3"
                        },
                        "start_time": "2026-01-17 21:30:00",
                        "end_time": "2026-01-18 05:30:00",
                        "start_values": {
                            "data_5": {
                                "value": "0",
                                "name": "Data 5",
                                "unit": "",
                                "used": false
                            },
                            "data_2": {
                                "value": "541139009",
                                "name": "Lưu lượng",
                                "unit": "",
                                "used": true
                            },
                            "data_1": {
                                "value": "541139009",
                                "name": "Khối lượng",
                                "unit": "kg",
                                "used": true
                            },
                            "data_3": {
                                "value": "541139009",
                                "name": "Tốc độ",
                                "unit": "",
                                "used": true
                            }
                        },
                        "end_values": {
                            "data_5": {
                                "value": "0",
                                "name": "Data 5",
                                "unit": "",
                                "used": false
                            },
                            "data_2": {
                                "value": "918369981",
                                "name": "Lưu lượng",
                                "unit": "",
                                "used": true
                            },
                            "data_1": {
                                "value": "918369981",
                                "name": "Khối lượng",
                                "unit": "kg",
                                "used": true
                            },
                            "data_3": {
                                "value": "918369981",
                                "name": "Tốc độ",
                                "unit": "",
                                "used": true
                            }
                        },
                        "data_values": {
                            "data_5": {
                                "value": "0",
                                "name": "Data 5",
                                "unit": "",
                                "used": false
                            },
                            "data_2": {
                                "value": "638526991",
                                "name": "Lưu lượng",
                                "unit": "",
                                "used": true
                            },
                            "data_1": {
                                "value": "377230972.00",
                                "name": "Khối lượng",
                                "unit": "kg",
                                "used": true
                            },
                            "data_3": {
                                "value": "638526991",
                                "name": "Tốc độ",
                                "unit": "",
                                "used": true
                            }
                        }
                    },
                    {
                        "scale": {
                            "id": 1,
                            "name": "Cân 01",
                            "model": "MODEL 1",
                            "direction": "IMPORT",
                            "location": {
                                "id": 1,
                                "code": "VT01",
                                "name": "Kho "
                            },
                            "is_active": true,
                            "created_at": "2025-12-30T01:50:06.746173Z",
                            "created_by": "admin",
                            "updated_at": "2026-01-14T01:33:14.935524Z",
                            "updated_by": "admin"
                        },
                        "period": "2026-01-18 CA1",
                        "recordCount": 2882,
                        "direction": 1,
                        "ratio": {
                            "value": "0.6297",
                            "formula": "data_1/data_3"
                        },
                        "start_time": "2026-01-18 05:30:00",
                        "end_time": "2026-01-18 13:30:00",
                        "start_values": {
                            "data_5": {
                                "value": "0",
                                "name": "Data 5",
                                "unit": "",
                                "used": false
                            },
                            "data_2": {
                                "value": "918369981",
                                "name": "Lưu lượng",
                                "unit": "",
                                "used": true
                            },
                            "data_1": {
                                "value": "918369981",
                                "name": "Khối lượng",
                                "unit": "kg",
                                "used": true
                            },
                            "data_3": {
                                "value": "918369981",
                                "name": "Tốc độ",
                                "unit": "",
                                "used": true
                            }
                        },
                        "end_values": {
                            "data_5": {
                                "value": "0",
                                "name": "Data 5",
                                "unit": "",
                                "used": false
                            },
                            "data_2": {
                                "value": "277155973",
                                "name": "Lưu lượng",
                                "unit": "",
                                "used": true
                            },
                            "data_1": {
                                "value": "277155973",
                                "name": "Khối lượng",
                                "unit": "kg",
                                "used": true
                            },
                            "data_3": {
                                "value": "277155973",
                                "name": "Tốc độ",
                                "unit": "",
                                "used": true
                            }
                        },
                        "data_values": {
                            "data_5": {
                                "value": "0",
                                "name": "Data 5",
                                "unit": "",
                                "used": false
                            },
                            "data_2": {
                                "value": "1018313906",
                                "name": "Lưu lượng",
                                "unit": "",
                                "used": true
                            },
                            "data_1": {
                                "value": "641214008.00",
                                "name": "Khối lượng",
                                "unit": "kg",
                                "used": true
                            },
                            "data_3": {
                                "value": "1018313906",
                                "name": "Tốc độ",
                                "unit": "",
                                "used": true
                            }
                        }
                    },
                    {
                        "scale": {
                            "id": 1,
                            "name": "Cân 01",
                            "model": "MODEL 1",
                            "direction": "IMPORT",
                            "location": {
                                "id": 1,
                                "code": "VT01",
                                "name": "Kho "
                            },
                            "is_active": true,
                            "created_at": "2025-12-30T01:50:06.746173Z",
                            "created_by": "admin",
                            "updated_at": "2026-01-14T01:33:14.935524Z",
                            "updated_by": "admin"
                        },
                        "period": "2026-01-18 CA2",
                        "recordCount": 2884,
                        "direction": 1,
                        "ratio": {
                            "value": "0.5892",
                            "formula": "data_1/data_3"
                        },
                        "start_time": "2026-01-18 13:30:00",
                        "end_time": "2026-01-18 21:30:28",
                        "start_values": {
                            "data_5": {
                                "value": "0",
                                "name": "Data 5",
                                "unit": "",
                                "used": false
                            },
                            "data_2": {
                                "value": "277155973",
                                "name": "Lưu lượng",
                                "unit": "",
                                "used": true
                            },
                            "data_1": {
                                "value": "277155973",
                                "name": "Khối lượng",
                                "unit": "kg",
                                "used": true
                            },
                            "data_3": {
                                "value": "277155973",
                                "name": "Tốc độ",
                                "unit": "",
                                "used": true
                            }
                        },
                        "end_values": {
                            "data_5": {
                                "value": "0",
                                "name": "Data 5",
                                "unit": "",
                                "used": false
                            },
                            "data_2": {
                                "value": "18219286",
                                "name": "Lưu lượng",
                                "unit": "",
                                "used": true
                            },
                            "data_1": {
                                "value": "18219286",
                                "name": "Khối lượng",
                                "unit": "kg",
                                "used": true
                            },
                            "data_3": {
                                "value": "18219286",
                                "name": "Tốc độ",
                                "unit": "",
                                "used": true
                            }
                        },
                        "data_values": {
                            "data_5": {
                                "value": "0",
                                "name": "Data 5",
                                "unit": "",
                                "used": false
                            },
                            "data_2": {
                                "value": "439491122",
                                "name": "Lưu lượng",
                                "unit": "",
                                "used": true
                            },
                            "data_1": {
                                "value": "258936687.00",
                                "name": "Khối lượng",
                                "unit": "kg",
                                "used": true
                            },
                            "data_3": {
                                "value": "439491122",
                                "name": "Tốc độ",
                                "unit": "",
                                "used": true
                            }
                        }
                    },
                    {
                        "scale": {
                            "id": 1,
                            "name": "Cân 01",
                            "model": "MODEL 1",
                            "direction": "IMPORT",
                            "location": {
                                "id": 1,
                                "code": "VT01",
                                "name": "Kho "
                            },
                            "is_active": true,
                            "created_at": "2025-12-30T01:50:06.746173Z",
                            "created_by": "admin",
                            "updated_at": "2026-01-14T01:33:14.935524Z",
                            "updated_by": "admin"
                        },
                        "period": "2026-01-18 CA3",
                        "recordCount": 2878,
                        "direction": 1,
                        "ratio": {
                            "value": "0.4188",
                            "formula": "data_1/data_3"
                        },
                        "start_time": "2026-01-18 21:30:00",
                        "end_time": "2026-01-19 05:30:00",
                        "start_values": {
                            "data_5": {
                                "value": "0",
                                "name": "Data 5",
                                "unit": "",
                                "used": false
                            },
                            "data_2": {
                                "value": "17826064",
                                "name": "Lưu lượng",
                                "unit": "",
                                "used": true
                            },
                            "data_1": {
                                "value": "17826064",
                                "name": "Khối lượng",
                                "unit": "kg",
                                "used": true
                            },
                            "data_3": {
                                "value": "17826064",
                                "name": "Tốc độ",
                                "unit": "",
                                "used": true
                            }
                        },
                        "end_values": {
                            "data_5": {
                                "value": "0",
                                "name": "Data 5",
                                "unit": "",
                                "used": false
                            },
                            "data_2": {
                                "value": "394991499",
                                "name": "Lưu lượng",
                                "unit": "",
                                "used": true
                            },
                            "data_1": {
                                "value": "394991499",
                                "name": "Khối lượng",
                                "unit": "kg",
                                "used": true
                            },
                            "data_3": {
                                "value": "394991499",
                                "name": "Tốc độ",
                                "unit": "",
                                "used": true
                            }
                        },
                        "data_values": {
                            "data_5": {
                                "value": "0",
                                "name": "Data 5",
                                "unit": "",
                                "used": false
                            },
                            "data_2": {
                                "value": "900543917",
                                "name": "Lưu lượng",
                                "unit": "",
                                "used": true
                            },
                            "data_1": {
                                "value": "377165435.00",
                                "name": "Khối lượng",
                                "unit": "kg",
                                "used": true
                            },
                            "data_3": {
                                "value": "900543917",
                                "name": "Tốc độ",
                                "unit": "",
                                "used": true
                            }
                        }
                    },
                    {
                        "scale": {
                            "id": 1,
                            "name": "Cân 01",
                            "model": "MODEL 1",
                            "direction": "IMPORT",
                            "location": {
                                "id": 1,
                                "code": "VT01",
                                "name": "Kho "
                            },
                            "is_active": true,
                            "created_at": "2025-12-30T01:50:06.746173Z",
                            "created_by": "admin",
                            "updated_at": "2026-01-14T01:33:14.935524Z",
                            "updated_by": "admin"
                        },
                        "period": "2026-01-19 CA1",
                        "recordCount": 2882,
                        "direction": 1,
                        "ratio": {
                            "value": "1.0000",
                            "formula": "data_1/data_3"
                        },
                        "start_time": "2026-01-19 05:30:00",
                        "end_time": "2026-01-19 13:30:00",
                        "start_values": {
                            "data_5": {
                                "value": "0",
                                "name": "Data 5",
                                "unit": "",
                                "used": false
                            },
                            "data_2": {
                                "value": "394991499",
                                "name": "Lưu lượng",
                                "unit": "",
                                "used": true
                            },
                            "data_1": {
                                "value": "394991499",
                                "name": "Khối lượng",
                                "unit": "kg",
                                "used": true
                            },
                            "data_3": {
                                "value": "394991499",
                                "name": "Tốc độ",
                                "unit": "",
                                "used": true
                            }
                        },
                        "end_values": {
                            "data_5": {
                                "value": "0",
                                "name": "Data 5",
                                "unit": "",
                                "used": false
                            },
                            "data_2": {
                                "value": "772353545",
                                "name": "Lưu lượng",
                                "unit": "",
                                "used": true
                            },
                            "data_1": {
                                "value": "772353545",
                                "name": "Khối lượng",
                                "unit": "kg",
                                "used": true
                            },
                            "data_3": {
                                "value": "772353545",
                                "name": "Tốc độ",
                                "unit": "",
                                "used": true
                            }
                        },
                        "data_values": {
                            "data_5": {
                                "value": "0",
                                "name": "Data 5",
                                "unit": "",
                                "used": false
                            },
                            "data_2": {
                                "value": "377362046",
                                "name": "Lưu lượng",
                                "unit": "",
                                "used": true
                            },
                            "data_1": {
                                "value": "377362046.00",
                                "name": "Khối lượng",
                                "unit": "kg",
                                "used": true
                            },
                            "data_3": {
                                "value": "377362046",
                                "name": "Tốc độ",
                                "unit": "",
                                "used": true
                            }
                        }
                    },
                    {
                        "scale": {
                            "id": 1,
                            "name": "Cân 01",
                            "model": "MODEL 1",
                            "direction": "IMPORT",
                            "location": {
                                "id": 1,
                                "code": "VT01",
                                "name": "Kho "
                            },
                            "is_active": true,
                            "created_at": "2025-12-30T01:50:06.746173Z",
                            "created_by": "admin",
                            "updated_at": "2026-01-14T01:33:14.935524Z",
                            "updated_by": "admin"
                        },
                        "period": "2026-01-19 CA2",
                        "recordCount": 2884,
                        "direction": 1,
                        "ratio": {
                            "value": "1.0000",
                            "formula": "data_1/data_3"
                        },
                        "start_time": "2026-01-19 13:30:00",
                        "end_time": "2026-01-19 21:30:28",
                        "start_values": {
                            "data_5": {
                                "value": "0",
                                "name": "Data 5",
                                "unit": "",
                                "used": false
                            },
                            "data_2": {
                                "value": "772353545",
                                "name": "Lưu lượng",
                                "unit": "",
                                "used": true
                            },
                            "data_1": {
                                "value": "772353545",
                                "name": "Khối lượng",
                                "unit": "kg",
                                "used": true
                            },
                            "data_3": {
                                "value": "772353545",
                                "name": "Tốc độ",
                                "unit": "",
                                "used": true
                            }
                        },
                        "end_values": {
                            "data_5": {
                                "value": "0",
                                "name": "Data 5",
                                "unit": "",
                                "used": false
                            },
                            "data_2": {
                                "value": "1149977739",
                                "name": "Lưu lượng",
                                "unit": "",
                                "used": true
                            },
                            "data_1": {
                                "value": "1149977739",
                                "name": "Khối lượng",
                                "unit": "kg",
                                "used": true
                            },
                            "data_3": {
                                "value": "1149977739",
                                "name": "Tốc độ",
                                "unit": "",
                                "used": true
                            }
                        },
                        "data_values": {
                            "data_5": {
                                "value": "0",
                                "name": "Data 5",
                                "unit": "",
                                "used": false
                            },
                            "data_2": {
                                "value": "377624194",
                                "name": "Lưu lượng",
                                "unit": "",
                                "used": true
                            },
                            "data_1": {
                                "value": "377624194.00",
                                "name": "Khối lượng",
                                "unit": "kg",
                                "used": true
                            },
                            "data_3": {
                                "value": "377624194",
                                "name": "Tốc độ",
                                "unit": "",
                                "used": true
                            }
                        }
                    },
                    {
                        "scale": {
                            "id": 1,
                            "name": "Cân 01",
                            "model": "MODEL 1",
                            "direction": "IMPORT",
                            "location": {
                                "id": 1,
                                "code": "VT01",
                                "name": "Kho "
                            },
                            "is_active": true,
                            "created_at": "2025-12-30T01:50:06.746173Z",
                            "created_by": "admin",
                            "updated_at": "2026-01-14T01:33:14.935524Z",
                            "updated_by": "admin"
                        },
                        "period": "2026-01-19 CA3",
                        "recordCount": 2877,
                        "direction": 1,
                        "ratio": {
                            "value": "0.3333",
                            "formula": "data_1/data_3"
                        },
                        "start_time": "2026-01-19 21:30:00",
                        "end_time": "2026-01-20 05:30:00",
                        "start_values": {
                            "data_5": {
                                "value": "0",
                                "name": "Data 5",
                                "unit": "",
                                "used": false
                            },
                            "data_2": {
                                "value": "1149584517",
                                "name": "Lưu lượng",
                                "unit": "",
                                "used": true
                            },
                            "data_1": {
                                "value": "1149584517",
                                "name": "Khối lượng",
                                "unit": "kg",
                                "used": true
                            },
                            "data_3": {
                                "value": "1149584517",
                                "name": "Tốc độ",
                                "unit": "",
                                "used": true
                            }
                        },
                        "end_values": {
                            "data_5": {
                                "value": "0",
                                "name": "Data 5",
                                "unit": "",
                                "used": false
                            },
                            "data_2": {
                                "value": "1526749952",
                                "name": "Lưu lượng",
                                "unit": "",
                                "used": true
                            },
                            "data_1": {
                                "value": "1526749952",
                                "name": "Khối lượng",
                                "unit": "kg",
                                "used": true
                            },
                            "data_3": {
                                "value": "1526749952",
                                "name": "Tốc độ",
                                "unit": "",
                                "used": true
                            }
                        },
                        "data_values": {
                            "data_5": {
                                "value": "0",
                                "name": "Data 5",
                                "unit": "",
                                "used": false
                            },
                            "data_2": {
                                "value": "1131496305",
                                "name": "Lưu lượng",
                                "unit": "",
                                "used": true
                            },
                            "data_1": {
                                "value": "377165435.00",
                                "name": "Khối lượng",
                                "unit": "kg",
                                "used": true
                            },
                            "data_3": {
                                "value": "1131496305",
                                "name": "Tốc độ",
                                "unit": "",
                                "used": true
                            }
                        }
                    },
                    {
                        "scale": {
                            "id": 1,
                            "name": "Cân 01",
                            "model": "MODEL 1",
                            "direction": "IMPORT",
                            "location": {
                                "id": 1,
                                "code": "VT01",
                                "name": "Kho "
                            },
                            "is_active": true,
                            "created_at": "2025-12-30T01:50:06.746173Z",
                            "created_by": "admin",
                            "updated_at": "2026-01-14T01:33:14.935524Z",
                            "updated_by": "admin"
                        },
                        "period": "2026-01-20 CA1",
                        "recordCount": 2881,
                        "direction": 1,
                        "ratio": {
                            "value": "0.7807",
                            "formula": "data_1/data_3"
                        },
                        "start_time": "2026-01-20 05:30:00",
                        "end_time": "2026-01-20 13:30:00",
                        "start_values": {
                            "data_5": {
                                "value": "0",
                                "name": "Data 5",
                                "unit": "",
                                "used": false
                            },
                            "data_2": {
                                "value": "1526749952",
                                "name": "Lưu lượng",
                                "unit": "",
                                "used": true
                            },
                            "data_1": {
                                "value": "1526749952",
                                "name": "Khối lượng",
                                "unit": "kg",
                                "used": true
                            },
                            "data_3": {
                                "value": "1526749952",
                                "name": "Tốc độ",
                                "unit": "",
                                "used": true
                            }
                        },
                        "end_values": {
                            "data_5": {
                                "value": "0",
                                "name": "Data 5",
                                "unit": "",
                                "used": false
                            },
                            "data_2": {
                                "value": "185994006",
                                "name": "Lưu lượng",
                                "unit": "",
                                "used": true
                            },
                            "data_1": {
                                "value": "185994006",
                                "name": "Khối lượng",
                                "unit": "kg",
                                "used": true
                            },
                            "data_3": {
                                "value": "185994006",
                                "name": "Tốc độ",
                                "unit": "",
                                "used": true
                            }
                        },
                        "data_values": {
                            "data_5": {
                                "value": "0",
                                "name": "Data 5",
                                "unit": "",
                                "used": false
                            },
                            "data_2": {
                                "value": "1717397085",
                                "name": "Lưu lượng",
                                "unit": "",
                                "used": true
                            },
                            "data_1": {
                                "value": "1340755946.00",
                                "name": "Khối lượng",
                                "unit": "kg",
                                "used": true
                            },
                            "data_3": {
                                "value": "1717397085",
                                "name": "Tốc độ",
                                "unit": "",
                                "used": true
                            }
                        }
                    },
                    {
                        "scale": {
                            "id": 1,
                            "name": "Cân 01",
                            "model": "MODEL 1",
                            "direction": "IMPORT",
                            "location": {
                                "id": 1,
                                "code": "VT01",
                                "name": "Kho "
                            },
                            "is_active": true,
                            "created_at": "2025-12-30T01:50:06.746173Z",
                            "created_by": "admin",
                            "updated_at": "2026-01-14T01:33:14.935524Z",
                            "updated_by": "admin"
                        },
                        "period": "2026-01-20 CA2",
                        "recordCount": 2884,
                        "direction": 1,
                        "ratio": {
                            "value": "0.4428",
                            "formula": "data_1/data_3"
                        },
                        "start_time": "2026-01-20 13:30:00",
                        "end_time": "2026-01-20 21:30:28",
                        "start_values": {
                            "data_5": {
                                "value": "0",
                                "name": "Data 5",
                                "unit": "",
                                "used": false
                            },
                            "data_2": {
                                "value": "185994006",
                                "name": "Lưu lượng",
                                "unit": "",
                                "used": true
                            },
                            "data_1": {
                                "value": "185994006",
                                "name": "Khối lượng",
                                "unit": "kg",
                                "used": true
                            },
                            "data_3": {
                                "value": "185994006",
                                "name": "Tốc độ",
                                "unit": "",
                                "used": true
                            }
                        },
                        "end_values": {
                            "data_5": {
                                "value": "0",
                                "name": "Data 5",
                                "unit": "",
                                "used": false
                            },
                            "data_2": {
                                "value": "333779941",
                                "name": "Lưu lượng",
                                "unit": "",
                                "used": true
                            },
                            "data_1": {
                                "value": "333779941",
                                "name": "Khối lượng",
                                "unit": "kg",
                                "used": true
                            },
                            "data_3": {
                                "value": "333779941",
                                "name": "Tốc độ",
                                "unit": "",
                                "used": true
                            }
                        },
                        "data_values": {
                            "data_5": {
                                "value": "0",
                                "name": "Data 5",
                                "unit": "",
                                "used": false
                            },
                            "data_2": {
                                "value": "333779941",
                                "name": "Lưu lượng",
                                "unit": "",
                                "used": true
                            },
                            "data_1": {
                                "value": "147785935.00",
                                "name": "Khối lượng",
                                "unit": "kg",
                                "used": true
                            },
                            "data_3": {
                                "value": "333779941",
                                "name": "Tốc độ",
                                "unit": "",
                                "used": true
                            }
                        }
                    },
                    {
                        "scale": {
                            "id": 1,
                            "name": "Cân 01",
                            "model": "MODEL 1",
                            "direction": "IMPORT",
                            "location": {
                                "id": 1,
                                "code": "VT01",
                                "name": "Kho "
                            },
                            "is_active": true,
                            "created_at": "2025-12-30T01:50:06.746173Z",
                            "created_by": "admin",
                            "updated_at": "2026-01-14T01:33:14.935524Z",
                            "updated_by": "admin"
                        },
                        "period": "2026-01-20 CA3",
                        "recordCount": 2879,
                        "direction": 1,
                        "ratio": {
                            "value": "0.3162",
                            "formula": "data_1/data_3"
                        },
                        "start_time": "2026-01-20 21:30:00",
                        "end_time": "2026-01-21 05:30:00",
                        "start_values": {
                            "data_5": {
                                "value": "0",
                                "name": "Data 5",
                                "unit": "",
                                "used": false
                            },
                            "data_2": {
                                "value": "333386719",
                                "name": "Lưu lượng",
                                "unit": "",
                                "used": true
                            },
                            "data_1": {
                                "value": "333386719",
                                "name": "Khối lượng",
                                "unit": "kg",
                                "used": true
                            },
                            "data_3": {
                                "value": "333386719",
                                "name": "Tốc độ",
                                "unit": "",
                                "used": true
                            }
                        },
                        "end_values": {
                            "data_5": {
                                "value": "0",
                                "name": "Data 5",
                                "unit": "",
                                "used": false
                            },
                            "data_2": {
                                "value": "710748765",
                                "name": "Lưu lượng",
                                "unit": "",
                                "used": true
                            },
                            "data_1": {
                                "value": "710748765",
                                "name": "Khối lượng",
                                "unit": "kg",
                                "used": true
                            },
                            "data_3": {
                                "value": "710748765",
                                "name": "Tốc độ",
                                "unit": "",
                                "used": true
                            }
                        },
                        "data_values": {
                            "data_5": {
                                "value": "0",
                                "name": "Data 5",
                                "unit": "",
                                "used": false
                            },
                            "data_2": {
                                "value": "1193363233",
                                "name": "Lưu lượng",
                                "unit": "",
                                "used": true
                            },
                            "data_1": {
                                "value": "377362046.00",
                                "name": "Khối lượng",
                                "unit": "kg",
                                "used": true
                            },
                            "data_3": {
                                "value": "1193363233",
                                "name": "Tốc độ",
                                "unit": "",
                                "used": true
                            }
                        }
                    },
                    {
                        "scale": {
                            "id": 1,
                            "name": "Cân 01",
                            "model": "MODEL 1",
                            "direction": "IMPORT",
                            "location": {
                                "id": 1,
                                "code": "VT01",
                                "name": "Kho "
                            },
                            "is_active": true,
                            "created_at": "2025-12-30T01:50:06.746173Z",
                            "created_by": "admin",
                            "updated_at": "2026-01-14T01:33:14.935524Z",
                            "updated_by": "admin"
                        },
                        "period": "2026-01-21 CA1",
                        "recordCount": 2881,
                        "direction": 1,
                        "ratio": {
                            "value": "1.0000",
                            "formula": "data_1/data_3"
                        },
                        "start_time": "2026-01-21 05:30:00",
                        "end_time": "2026-01-21 13:30:00",
                        "start_values": {
                            "data_5": {
                                "value": "0",
                                "name": "Data 5",
                                "unit": "",
                                "used": false
                            },
                            "data_2": {
                                "value": "710748765",
                                "name": "Lưu lượng",
                                "unit": "",
                                "used": true
                            },
                            "data_1": {
                                "value": "710748765",
                                "name": "Khối lượng",
                                "unit": "kg",
                                "used": true
                            },
                            "data_3": {
                                "value": "710748765",
                                "name": "Tốc độ",
                                "unit": "",
                                "used": true
                            }
                        },
                        "end_values": {
                            "data_5": {
                                "value": "0",
                                "name": "Data 5",
                                "unit": "",
                                "used": false
                            },
                            "data_2": {
                                "value": "1087979737",
                                "name": "Lưu lượng",
                                "unit": "",
                                "used": true
                            },
                            "data_1": {
                                "value": "1087979737",
                                "name": "Khối lượng",
                                "unit": "kg",
                                "used": true
                            },
                            "data_3": {
                                "value": "1087979737",
                                "name": "Tốc độ",
                                "unit": "",
                                "used": true
                            }
                        },
                        "data_values": {
                            "data_5": {
                                "value": "0",
                                "name": "Data 5",
                                "unit": "",
                                "used": false
                            },
                            "data_2": {
                                "value": "377230972",
                                "name": "Lưu lượng",
                                "unit": "",
                                "used": true
                            },
                            "data_1": {
                                "value": "377230972.00",
                                "name": "Khối lượng",
                                "unit": "kg",
                                "used": true
                            },
                            "data_3": {
                                "value": "377230972",
                                "name": "Tốc độ",
                                "unit": "",
                                "used": true
                            }
                        }
                    },
                    {
                        "scale": {
                            "id": 1,
                            "name": "Cân 01",
                            "model": "MODEL 1",
                            "direction": "IMPORT",
                            "location": {
                                "id": 1,
                                "code": "VT01",
                                "name": "Kho "
                            },
                            "is_active": true,
                            "created_at": "2025-12-30T01:50:06.746173Z",
                            "created_by": "admin",
                            "updated_at": "2026-01-14T01:33:14.935524Z",
                            "updated_by": "admin"
                        },
                        "period": "2026-01-21 CA2",
                        "recordCount": 501,
                        "direction": 1,
                        "ratio": {
                            "value": "1.0000",
                            "formula": "data_1/data_3"
                        },
                        "start_time": "2026-01-21 13:30:00",
                        "end_time": "2026-01-21 14:55:18",
                        "start_values": {
                            "data_5": {
                                "value": "0",
                                "name": "Data 5",
                                "unit": "",
                                "used": false
                            },
                            "data_2": {
                                "value": "1087979737",
                                "name": "Lưu lượng",
                                "unit": "",
                                "used": true
                            },
                            "data_1": {
                                "value": "1087979737",
                                "name": "Khối lượng",
                                "unit": "kg",
                                "used": true
                            },
                            "data_3": {
                                "value": "1087979737",
                                "name": "Tốc độ",
                                "unit": "",
                                "used": true
                            }
                        },
                        "end_values": {
                            "data_5": {
                                "value": "0",
                                "name": "Data 5",
                                "unit": "",
                                "used": false
                            },
                            "data_2": {
                                "value": "1153451200",
                                "name": "Lưu lượng",
                                "unit": "",
                                "used": true
                            },
                            "data_1": {
                                "value": "1153451200",
                                "name": "Khối lượng",
                                "unit": "kg",
                                "used": true
                            },
                            "data_3": {
                                "value": "1153451200",
                                "name": "Tốc độ",
                                "unit": "",
                                "used": true
                            }
                        },
                        "data_values": {
                            "data_5": {
                                "value": "0",
                                "name": "Data 5",
                                "unit": "",
                                "used": false
                            },
                            "data_2": {
                                "value": "65471463",
                                "name": "Lưu lượng",
                                "unit": "",
                                "used": true
                            },
                            "data_1": {
                                "value": "65471463.00",
                                "name": "Khối lượng",
                                "unit": "kg",
                                "used": true
                            },
                            "data_3": {
                                "value": "65471463",
                                "name": "Tốc độ",
                                "unit": "",
                                "used": true
                            }
                        }
                    },
                    {
                        "scale": {
                            "id": 1,
                            "name": "Cân 01",
                            "model": "MODEL 1",
                            "direction": "IMPORT",
                            "location": {
                                "id": 1,
                                "code": "VT01",
                                "name": "Kho "
                            },
                            "is_active": true,
                            "created_at": "2025-12-30T01:50:06.746173Z",
                            "created_by": "admin",
                            "updated_at": "2026-01-14T01:33:14.935524Z",
                            "updated_by": "admin"
                        },
                        "period": "2026-01-21 CA3",
                        "recordCount": 1981,
                        "direction": 1,
                        "ratio": {
                            "value": "3.4489",
                            "formula": "data_1/data_3"
                        },
                        "start_time": "2026-01-21 21:30:00",
                        "end_time": "2026-01-21 14:55:18",
                        "start_values": {
                            "data_5": {
                                "value": "0",
                                "name": "Data 5",
                                "unit": "",
                                "used": false
                            },
                            "data_2": {
                                "value": "259264372",
                                "name": "Lưu lượng",
                                "unit": "",
                                "used": true
                            },
                            "data_1": {
                                "value": "259264372",
                                "name": "Khối lượng",
                                "unit": "kg",
                                "used": true
                            },
                            "data_3": {
                                "value": "259264372",
                                "name": "Tốc độ",
                                "unit": "",
                                "used": true
                            }
                        },
                        "end_values": {
                            "data_5": {
                                "value": "0",
                                "name": "Data 5",
                                "unit": "",
                                "used": false
                            },
                            "data_2": {
                                "value": "1153451200",
                                "name": "Lưu lượng",
                                "unit": "",
                                "used": true
                            },
                            "data_1": {
                                "value": "1153451200",
                                "name": "Khối lượng",
                                "unit": "kg",
                                "used": true
                            },
                            "data_3": {
                                "value": "1153451200",
                                "name": "Tốc độ",
                                "unit": "",
                                "used": true
                            }
                        },
                        "data_values": {
                            "data_5": {
                                "value": "0",
                                "name": "Data 5",
                                "unit": "",
                                "used": false
                            },
                            "data_2": {
                                "value": "259264372",
                                "name": "Lưu lượng",
                                "unit": "",
                                "used": true
                            },
                            "data_1": {
                                "value": "894186828.00",
                                "name": "Khối lượng",
                                "unit": "kg",
                                "used": true
                            },
                            "data_3": {
                                "value": "259264372",
                                "name": "Tốc độ",
                                "unit": "",
                                "used": true
                            }
                        }
                    }
                ],
                "dataFieldSummaries": {
                    "data_2": {
                        "value": "841524571.75",
                        "aggregation": "AVG",
                        "name": "Lưu lượng",
                        "unit": "",
                        "used": true
                    },
                    "data_1": {
                        "value": "13223531566.00",
                        "aggregation": "SUM",
                        "name": "Khối lượng",
                        "unit": "kg",
                        "used": true
                    },
                    "data_3": {
                        "value": "841524571.75",
                        "aggregation": "AVG",
                        "name": "Tốc độ",
                        "unit": "",
                        "used": true
                    }
                }
            }
        ],
        "page": 0,
        "size": 20,
        "total_elements": 60,
        "total_pages": 3,
        "is_first": true,
        "is_last": false,
        "has_next": true,
        "has_previous": false
    }
}
```