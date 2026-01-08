## PROTOCOL
### POST http://localhost:8080/api/v1/protocols
* Payload
``` json
{
  "code": "MODBUS_RTU",
  "name": "Modbus RTU",
  "description": "string",
  "connection_type": "RTU",
  "default_port": 502,
  "default_baud_rate": 9600,
  "is_active": true,
  "config_template": "string"
}
```
* Response code : 201
* Response body

``` json
{
  "success": true,
  "data": {
    "id": 2,
    "code": "MODBUS_RTU",
    "name": "Modbus RTU",
    "description": "string",
    "connection_type": "RTU",
    "default_port": 502,
    "default_baud_rate": 9600,
    "is_active": true,
    "config_template": "string",
    "created_at": "2026-01-08T10:32:22.4119098+07:00",
    "created_by": "admin",
    "updated_at": "2026-01-08T10:32:22.4119098+07:00",
    "updated_by": "admin"
  }
}
```

### GET http://localhost:8080/api/v1/protocols
* Params
- `page` (integer)
- `size` (integer)
- `sort` (string) value field,asc/desc
- `search` (string)
- `code` (string)
- `connectionType` (string)

* Response
``` json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 2,
        "code": "MODBUS_RTU",
        "name": "Modbus RTU",
        "description": "string",
        "connection_type": "RTU",
        "default_port": 502,
        "default_baud_rate": 9600,
        "is_active": true,
        "config_template": "string",
        "created_at": "2026-01-08T03:32:22.41191Z",
        "created_by": "admin",
        "updated_at": "2026-01-08T03:32:22.41191Z",
        "updated_by": "admin"
      },
      {
        "id": 1,
        "code": "MODBUS_TCP",
        "name": "Modbus TCP",
        "description": "string",
        "connection_type": "TCP",
        "default_port": 502,
        "default_baud_rate": 9600,
        "is_active": true,
        "config_template": "string",
        "created_at": "2026-01-08T03:32:00.743936Z",
        "created_by": "admin",
        "updated_at": "2026-01-08T03:32:00.743936Z",
        "updated_by": "admin"
      }
    ],
    "page": 0,
    "size": 10,
    "total_elements": 2,
    "total_pages": 1,
    "is_first": true,
    "is_last": true,
    "has_next": false,
    "has_previous": false
  }
}
```

### PUT http://localhost:8080/api/v1/protocols/{id}
* Parameter: id (integer)
* Payload: 
``` json
{
  "code": "MODBUS_TCP",
  "name": "Modbus TCP",
  "description": "string",
  "connection_type": "TCP",
  "default_port": 502,
  "default_baud_rate": 9600,
  "is_active": true,
  "config_template": "string"
}
```
* Response code: 200
* Response body:

``` json
{
  "success": true,
  "data": {
    "id": 1,
    "code": "MODBUS_TCP",
    "name": "Modbus TCP",
    "description": "string",
    "connection_type": "TCP",
    "default_port": 502,
    "default_baud_rate": 9600,
    "is_active": true,
    "config_template": "string",
    "created_at": "2026-01-08T03:32:00.743936Z",
    "created_by": "admin",
    "updated_at": "2026-01-08T03:32:00.743936Z",
    "updated_by": "admin"
  }
}
```

### GET http://localhost:8080/api/v1/protocols/{id}
* Parameter: id (integer)
* Response code: 200
* Response body

``` json
{
  "success": true,
  "data": {
    "id": 1,
    "code": "MODBUS_TCP",
    "name": "Modbus TCP",
    "description": "string",
    "connection_type": "TCP",
    "default_port": 502,
    "default_baud_rate": 9600,
    "is_active": true,
    "config_template": "string",
    "created_at": "2026-01-08T03:32:00.743936Z",
    "created_by": "admin",
    "updated_at": "2026-01-08T03:32:00.743936Z",
    "updated_by": "admin"
  }
}
```

### DELETE http://localhost:8080/api/v1/protocols/{id}
* Param: id (integer)
* Response code: 204

## SCALE MANUFACTURER MANAGEMENT

### POST http://localhost:8080/api/v1/manufacturers
* Payload
``` json 
{
  "code": "string",
  "name": "string",
  "country": "string",
  "website": "string",
  "phone": "string",
  "email": "string@gmail.com",
  "address": "string",
  "description": "string",
  "is_active": true
}
```
* Response code: 201
* Response body:
``` json
{
  "success": true,
  "data": {
    "id": 1,
    "code": "string",
    "name": "string",
    "country": "string",
    "website": "string",
    "phone": "string",
    "email": "string@gmail.com",
    "address": "string",
    "description": "string",
    "is_active": true,
    "created_at": "2026-01-08T11:15:53.0898315+07:00",
    "created_by": "admin",
    "updated_at": "2026-01-08T11:15:53.0898315+07:00",
    "updated_by": "admin"
  }
}
```
### GET http://localhost:8080/api/v1/manufacturers

* Params
- `page` (integer)
- `size` (integer)
- `sort` (string), value field,asc/desc
- `search` (string)
- `code` (string)
- `country` (string)

* Response code: 200
* Response body:
``` json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 1,
        "code": "string",
        "name": "string",
        "country": "string",
        "website": "string",
        "phone": "string",
        "email": "string@gmail.com",
        "address": "string",
        "description": "string",
        "is_active": true,
        "created_at": "2026-01-08T04:15:53.089832Z",
        "created_by": "admin",
        "updated_at": "2026-01-08T04:15:53.089832Z",
        "updated_by": "admin"
      }
    ],
    "page": 0,
    "size": 10,
    "total_elements": 1,
    "total_pages": 1,
    "is_first": true,
    "is_last": true,
    "has_next": false,
    "has_previous": false
  }
}
```
### GET http://localhost:8080/api/v1/manufacturers/{id}
* Parameter: id (integer)
* Response code: 200
* Response body

``` json
{
  "success": true,
  "data": {
    "id": 1,
    "code": "string",
    "name": "string",
    "country": "string",
    "website": "string",
    "phone": "string",
    "email": "string@gmail.com",
    "address": "string",
    "description": "string",
    "is_active": true,
    "created_at": "2026-01-08T04:15:53.089832Z",
    "created_by": "admin",
    "updated_at": "2026-01-08T04:15:53.089832Z",
    "updated_by": "admin"
  }
}
```
### PUT http://localhost:8080/api/v1/manufacturers/{id}
* Parameter: id (integer)
* Payload: 

``` json
{
  "code": "string",
  "name": "string",
  "country": "string",
  "website": "string",
  "phone": "string",
  "email": "string@mail.com",
  "address": "string",
  "description": "string",
  "is_active": true
}
```
* Response code : 200

* Response body: 
``` json
{
  "success": true,
  "data": {
    "id": 1,
    "code": "string",
    "name": "string",
    "country": "string",
    "website": "string",
    "phone": "string",
    "email": "string@mail.com",
    "address": "string",
    "description": "string",
    "is_active": true,
    "created_at": "2026-01-08T04:15:53.089832Z",
    "created_by": "admin",
    "updated_at": "2026-01-08T04:15:53.089832Z",
    "updated_by": "admin"
  }
}
```

### DELETE http://localhost:8080/api/v1/manufacturers/{id}
* Parameter: id (integer)
* Response code: 204 - xóa thành công