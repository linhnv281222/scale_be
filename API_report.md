## Template management
### POST http://localhost:8080/api/v1/report-templates/import - tải lên template

* Parameters
- `templateCode`: string (require)
-  `templateName`: string (require)
- `description`: string
-  `titleTemplate`: string
- `importNotes`: string
-   `isActive`: boolean
- `templateType`: string; "Báo cáo cân" hoặc "Báo cáo ca"
* Request body:
- `file`: chọn file word
* Response code: 200
* Response body: 
``` json
{
  "success": true,
  "data": {
    "id": 13,
    "templateId": 25,
    "templateCode": "sssssf",
    "originalFilename": "string_SCALE1_20251201 (1).docx",
    "resourcePath": "templates/reports/sssssf_20260108_172950.docx",
    "fileSizeBytes": 11847,
    "fileHash": "9951670272a040365368704168ad471c733253b3c1f7332e3db3baa75238fb27",
    "importStatus": "ACTIVE",
    "importDate": "2026-01-08T17:29:50.4022015+07:00",
    "importNotes": null,
    "isActive": true,
    "templateType": "Báo cáo ca",
    "createdBy": "admin",
    "createdAt": "2026-01-08T17:29:50.4022015+07:00"
  }
}
```

### GET http://localhost:8080/api/v1/report-templates/imports/list - Lấy danh sách thông tin template
* Parameter
- `templateType`: string (Báo cáo cân, báo cáo ca)
* Response code: 200
* Response body (example): 
``` json
{
  "success": true,
  "data": [
    {
      "id": 10,
      "templateCode": "BCSL123",
      "originalFilename": "template.docx",
      "resourcePath": "templates/reports/BCSL123_20260106_105901.docx",
      "fileSizeBytes": 13853,
      "importStatus": "ACTIVE",
      "importDate": "2026-01-06T03:59:01.640382Z",
      "isActive": true,
      "templateType": null
    },
    {
      "id": 9,
      "templateCode": "TEST8",
      "originalFilename": "TEST7_20251231_161103.docx",
      "resourcePath": "templates/reports/TEST8_20251231_162658.docx",
      "fileSizeBytes": 13761,
      "importStatus": "ACTIVE",
      "importDate": "2025-12-31T09:26:58.901931Z",
      "isActive": true,
      "templateType": null
    },
    {
      "id": 8,
      "templateCode": "TEST7",
      "originalFilename": "template.docx",
      "resourcePath": "templates/reports/TEST7_20251231_161103.docx",
      "fileSizeBytes": 13700,
      "importStatus": "ACTIVE",
      "importDate": "2025-12-31T09:11:03.207577Z",
      "isActive": true,
      "templateType": null
    },
    {
      "id": 7,
      "templateCode": "TEST6",
      "originalFilename": "report_template.docx",
      "resourcePath": "templates/reports/TEST6_20251231_151450.docx",
      "fileSizeBytes": 27647,
      "importStatus": "ACTIVE",
      "importDate": "2025-12-31T08:14:50.630142Z",
      "isActive": true,
      "templateType": null
    },
    {
      "id": 6,
      "templateCode": "TEST5",
      "originalFilename": "report_template.docx",
      "resourcePath": "templates/reports/TEST5_20251231_151058.docx",
      "fileSizeBytes": 27059,
      "importStatus": "ACTIVE",
      "importDate": "2025-12-31T08:10:59.107662Z",
      "isActive": true,
      "templateType": null
    },
    {
      "id": 5,
      "templateCode": "TEST4",
      "originalFilename": "report_template.docx",
      "resourcePath": "templates/reports/TEST4_20251231_150335.docx",
      "fileSizeBytes": 37263,
      "importStatus": "ACTIVE",
      "importDate": "2025-12-31T08:03:35.930235Z",
      "isActive": true,
      "templateType": null
    },
    {
      "id": 4,
      "templateCode": "TEST3",
      "originalFilename": "Report_Template_v2.docx",
      "resourcePath": "templates/reports/TEST3_20251231_145238.docx",
      "fileSizeBytes": 37333,
      "importStatus": "ACTIVE",
      "importDate": "2025-12-31T07:52:38.103915Z",
      "isActive": true,
      "templateType": null
    }
  ]
}
```
### GET http://localhost:8080/api/v1/report-templates/imports/{importId} - Lấy danh sách thông tin template theo id
* Parameter
- `importId`: integer
* Response code: 200
* Response body:
``` json
{
  "success": true,
  "data": {
    "importId": 13,
    "template": {
      "id": 25,
      "code": "sssssf",
      "name": "ssss",
      "description": "s",
      "titleTemplate": null,
      "isActive": true,
      "isDefault": false,
      "wordTemplateFilename": null,
      "hasWordTemplateFile": false
    },
    "originalFilename": "string_SCALE1_20251201 (1).docx",
    "resourcePath": "templates/reports/sssssf_20260108_172950.docx",
    "fileSizeBytes": 11847,
    "fileHash": "9951670272a040365368704168ad471c733253b3c1f7332e3db3baa75238fb27",
    "importStatus": "ACTIVE",
    "importDate": "2026-01-08T10:29:50.402202Z",
    "importNotes": null
  }
}
```
### GET http://localhost:8080/api/v1/report-templates/imports/{importId}/download - tải template
* Parameter
- `importId`: integer
* Response code: 200
* Response body: binary data
### POST http://localhost:8080/api/v1/report-templates/imports/{importId}/archive - inactive template

* Parameter
- `importId`: integer
* Response code: 200
* Response body: 
``` json
{
  "success": true,
  "data": {
    "id": 13,
    "templateId": 25,
    "templateCode": "sssssf",
    "originalFilename": "string_SCALE1_20251201 (1).docx",
    "resourcePath": "templates/reports/sssssf_20260108_172950.docx",
    "fileSizeBytes": 11847,
    "fileHash": "9951670272a040365368704168ad471c733253b3c1f7332e3db3baa75238fb27",
    "importStatus": "ARCHIVED",
    "importDate": "2026-01-08T10:29:50.402202Z",
    "importNotes": null,
    "isActive": false,
    "templateType": "Báo cáo ca",
    "createdBy": "admin",
    "createdAt": "2026-01-08T10:29:50.402202Z"
  }
}
```
## Export report
### POST http://localhost:8080/api/v1/reports/export - Xuất file
* Parameters:
- `importId`: integer
* Request body:
``` json
{
  "type": "WORD",
  "scaleIds": [
    0
  ],
  "startTime": "2026-01-08T10:44:43.197Z",
  "endTime": "2026-01-08T10:44:43.197Z",
  "dataFields": [
    "data_1", "data_2"
  ],
  "aggregationMethod": "SUM",
  "aggregationByField": {
     "data_1": "SUM"
  },
  "intervalReport": true,
  "timeInterval": "SHIFT",
  "locationIds": [
    0
  ],
  "activeOnly": true,
  "reportTitle": "string",
  "reportCode": "string",
  "preparedBy": "string"
}
```
* Response code: 200
* Response body: binary