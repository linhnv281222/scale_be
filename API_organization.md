## ORGANIZATION API
### GET http://localhost:8080/api/v1/api/organization-settings

* Response code: 200
* Response body: 
``` json
{
  "id": 4,
  "companyName": "string",
  "companyNameEn": "string",
  "address": "string",
  "phone": "string",
  "email": "string",
  "website": "string",
  "taxCode": "string",
  "logoUrl": "images/logos/org-4-d3d6193f-c1bd-4a6d-9b6b-740b7fdf2ee6.png",
  "logoBase64": "base64",
  "hasLogo": true,
  "watermarkText": "string",
  "isActive": true,
  "isDefault": true,
  "createdAt": "2025-12-27T02:07:34.081753Z",
  "updatedAt": "2026-01-09T04:36:53.121559Z"
}
```

### POST http://localhost:8080/api/v1/api/organization-settings

* Parameters:
-  `companyName`: string
- `companyNameEn`: string
- `address`: string
- `phone`: string
- `email`: string
- `website`: string
- `taxCode`: string
- `watermarkText`: dtring
* Request body: 
- `logo`: file

* Response code: 201
* Response body: 
``` json
{
  "id": 0,
  "companyName": "string",
  "companyNameEn": "string",
  "address": "string",
  "phone": "string",
  "email": "string",
  "website": "string",
  "taxCode": "string",
  "logoUrl": "string",
  "logoBase64": "string",
  "hasLogo": true,
  "watermarkText": "string",
  "isActive": true,
  "isDefault": true,
  "createdAt": "2026-01-10T08:17:15.515Z",
  "updatedAt": "2026-01-10T08:17:15.516Z"
}
```


### PUT http://localhost:8080/api/v1/api/organization-settings

* Parameters:
-  `companyName`: string
- `companyNameEn`: string
- `address`: string
- `phone`: string
- `email`: string
- `website`: string
- `taxCode`: string
- `watermarkText`: dtring
* Request body: 
- `logo`: file

* Response code: 201
* Response body: 
``` json
{
  "id": 0,
  "companyName": "string",
  "companyNameEn": "string",
  "address": "string",
  "phone": "string",
  "email": "string",
  "website": "string",
  "taxCode": "string",
  "logoUrl": "string",
  "logoBase64": "string",
  "hasLogo": true,
  "watermarkText": "string",
  "isActive": true,
  "isDefault": true,
  "createdAt": "2026-01-10T08:17:15.515Z",
  "updatedAt": "2026-01-10T08:17:15.516Z"
}
```