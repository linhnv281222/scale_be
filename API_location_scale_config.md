## Location management

### POST http://localhost:8080/api/v1/locations

* Payload:
{
  "code": "string",
  "name": "string",
  "description": "string",
  "parentId": integer
}

* Response code: 201
* Response body
``` json
{
  "success": true,
  "data": {
    "id": 3,
    "code": "string",
    "name": "string",
    "children": [],
    "parent_id": 1,
    "created_at": "2026-01-08T15:06:49.640589+07:00",
    "created_by": "admin",
    "updated_at": "2026-01-08T15:06:49.640589+07:00",
    "updated_by": "admin"
  }
}
```
### GET http://localhost:8080/api/v1/locations

* Parameters
-  `page`: integer
- `size`: integer
- `sort`: string ("field,asc/desc")
- `search`: string
- `code`: string
- `parentId`: integer

* Response code: 200
* Response body:
- Nếu filter, paging null hết, kết quả trả về dạng cây như sau:
``` json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "code": "WS_01",
      "name": "string",
      "children": [
        {
          "id": 2,
          "code": "AAA",
          "name": "AAA",
          "children": [],
          "parent_id": 1,
          "created_at": "2025-12-25T09:44:57.568227Z",
          "created_by": "admin",
          "updated_at": "2025-12-25T09:58:06.681294Z",
          "updated_by": "admin"
        },
        {
          "id": 3,
          "code": "string",
          "name": "string",
          "children": [],
          "parent_id": 1,
          "created_at": "2026-01-08T08:06:49.640589Z",
          "created_by": "admin",
          "updated_at": "2026-01-08T08:06:49.640589Z",
          "updated_by": "admin"
        }
      ],
      "created_at": "2025-12-24T07:11:33.079615Z",
      "created_by": "admin",
      "updated_at": "2026-01-08T08:06:25.163218Z",
      "updated_by": "admin"
    }
  ]
}
```
- Nếu paging, filter có giá trị, trả về danh sách phẳng:
``` json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 2,
        "code": "AAA",
        "name": "AAA",
        "parent_id": 1,
        "created_at": "2025-12-25T09:44:57.568227Z",
        "created_by": "admin",
        "updated_at": "2025-12-25T09:58:06.681294Z",
        "updated_by": "admin"
      },
      {
        "id": 1,
        "code": "WS_01",
        "name": "string",
        "created_at": "2025-12-24T07:11:33.079615Z",
        "created_by": "admin",
        "updated_at": "2026-01-08T08:06:25.163218Z",
        "updated_by": "admin"
      },
      {
        "id": 3,
        "code": "string",
        "name": "string",
        "parent_id": 1,
        "created_at": "2026-01-08T08:06:49.640589Z",
        "created_by": "admin",
        "updated_at": "2026-01-08T08:06:49.640589Z",
        "updated_by": "admin"
      }
    ],
    "page": 0,
    "size": 10,
    "total_elements": 3,
    "total_pages": 1,
    "is_first": true,
    "is_last": true,
    "has_next": false,
    "has_previous": false
  }
}
```
### GET http://localhost:8080/api/v1/locations/{id}
* Parameters:
-  `id` :integer
* Response code: 200
* Response body: 
``` json
{
  "success": true,
  "data": {
    "id": 1,
    "code": "WS_01",
    "name": "string",
    "children": [
      {
        "id": 2,
        "code": "AAA",
        "name": "AAA",
        "children": [],
        "parent_id": 1,
        "created_at": "2025-12-25T09:44:57.568227Z",
        "created_by": "admin",
        "updated_at": "2025-12-25T09:58:06.681294Z",
        "updated_by": "admin"
      },
      {
        "id": 3,
        "code": "string",
        "name": "string",
        "children": [],
        "parent_id": 1,
        "created_at": "2026-01-08T08:06:49.640589Z",
        "created_by": "admin",
        "updated_at": "2026-01-08T08:06:49.640589Z",
        "updated_by": "admin"
      }
    ],
    "created_at": "2025-12-24T07:11:33.079615Z",
    "created_by": "admin",
    "updated_at": "2026-01-08T08:06:25.163218Z",
    "updated_by": "admin"
  }
}
```

### PUT http://localhost:8080/api/v1/locations/{id}
* Parameters:
-  `id`:integer
* Payload
``` json
{
  "code": "string",
  "name": "string",
  "description": "string",
  "parentId": null
}
```
* Response code: 200
* Response body (example):
``` json
{
  "success": true,
  "data": {
    "id": 1,
    "code": "WS_01",
    "name": "string",
    "children": [
      {
        "id": 2,
        "code": "AAA",
        "name": "AAA",
        "children": [],
        "parent_id": 1,
        "created_at": "2025-12-25T09:44:57.568227Z",
        "created_by": "admin",
        "updated_at": "2025-12-25T09:58:06.681294Z",
        "updated_by": "admin"
      },
      {
        "id": 3,
        "code": "string",
        "name": "string",
        "children": [],
        "parent_id": 1,
        "created_at": "2026-01-08T08:06:49.640589Z",
        "created_by": "admin",
        "updated_at": "2026-01-08T08:06:49.640589Z",
        "updated_by": "admin"
      }
    ],
    "created_at": "2025-12-24T07:11:33.079615Z",
    "created_by": "admin",
    "updated_at": "2026-01-08T08:06:25.163218Z",
    "updated_by": "admin"
  }
}
```
### DELETE http://localhost:8080/api/v1/locations/{id}
* Parameters:
-  `id` :integer
* Response code: 204 - xóa thành công

## Scale management
### POST http://localhost:8080/api/v1/scales
* Payload: 
``` json 
{
  "name": "Truck Scale 2",
  "model": "TS-5000",
  "direction": "IMPORT",
  "protocol": "MODBUS_RTU",
  "location_id": 1,
  "manufacturer_id": 1,
  "protocol_id": 3,
  "is_active": true,
  "poll_interval": 1000,

  "conn_params": {
    "com_port": "COM3",
    "baud_rate": 9600,
    "data_bits": 8,
    "stop_bits": 1,
    "parity": "even",
    "unit_id": 1
  },

  "data_1": {
    "name": "Weight",
    "function_code": 3,
    "start_register": 0,
    "num_registers": 2,
    "data_type": "int32",
    "byte_order": "big_endian",
    "is_used": true
  },

  "data_2": null,
  "data_3": null,
  "data_4": null,
  "data_5": null
}
```
* Response code: 201
* Response body (example):
``` json
{
  "id": 5,
  "name": "Truck Scale 2",
  "model": "TS-5000",
  "direction": "IMPORT",
  "location_id": 1,
  "location_name": "string",
  "manufacturer_id": 1,
  "manufacturer_name": "string",
  "manufacturer_code": "string",
  "protocol_id": 3,
  "protocol_name": "Modbus RTU",
  "protocol_code": "MODBUS_RTU",
  "is_active": true,
  "created_at": "2026-01-08T16:48:59.1837521+07:00",
  "created_by": "admin",
  "updated_at": "2026-01-08T16:48:59.1837521+07:00",
  "updated_by": "admin"
}
```

### GET http://localhost:8080/api/v1/scales
* Parameters:
- `page`: integer
- `size`: integer
- `sort`: string (field,asc/desc)
- `search`: string
- `locationId`: integer
- `manufacturerId`: integer
- `protocolId`: integer
- `model`: string
- `direction`: string
- `isActive`: boolean

* Response code: 200
* Response body: 
``` json
{
  "content": [
    {
      "id": 5,
      "name": "Truck Scale 2",
      "model": "TS-5000",
      "direction": "IMPORT",
      "location_id": 1,
      "location_name": "string",
      "manufacturer_id": 1,
      "manufacturer_name": "string",
      "manufacturer_code": "string",
      "protocol_id": 3,
      "protocol_name": "Modbus RTU",
      "protocol_code": "MODBUS_RTU",
      "is_active": true,
      "created_at": "2026-01-08T09:48:59.183752Z",
      "created_by": "admin",
      "updated_at": "2026-01-08T09:48:59.183752Z",
      "updated_by": "admin",
      "scale_config": {
        "protocol": "MODBUS_RTU",
        "scale_id": 5,
        "poll_interval": 1000,
        "conn_params": {
          "parity": "even",
          "unit_id": 1,
          "com_port": "COM3",
          "baud_rate": 9600,
          "data_bits": 8,
          "stop_bits": 1
        },
        "data_1": {
          "name": "Weight",
          "is_used": true,
          "data_type": "int32",
          "byte_order": "big_endian",
          "function_code": 3,
          "num_registers": 2,
          "start_register": 0
        },
        "data_2": {
          "is_used": false
        },
        "data_3": {
          "is_used": false
        },
        "data_4": {
          "is_used": false
        },
        "data_5": {
          "is_used": false
        }
      }
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
```
### GET http://localhost:8080/api/v1/scales/{id}
* Parameter:
- `id`: integer
* Response code: 200
* Response body:

- example with Modbus TCP
``` json
{
  "id": 4,
  "name": "Truck Scale 1",
  "model": "TS-5000",
  "direction": "IMPORT",
  "location_id": 1,
  "location_name": "string",
  "manufacturer_id": 1,
  "manufacturer_name": "string",
  "manufacturer_code": "string",
  "protocol_id": 1,
  "protocol_name": "Modbus TCP",
  "protocol_code": "MODBUS_TCP",
  "is_active": true,
  "created_at": "2026-01-08T09:31:18.690924Z",
  "created_by": "admin",
  "updated_at": "2026-01-08T09:31:18.690924Z",
  "updated_by": "admin",
  "scale_config": {
    "protocol": "MODBUS_TCP",
    "scale_id": 4,
    "poll_interval": 1000,
    "conn_params": {
      "ip": "192.168.1.10",
      "port": 502
    },
    "data_1": {
      "name": "Weight",
      "is_used": true,
      "num_registers": 2,
      "start_registers": 40002
    },
    "data_2": {
      "additionalProp1": {},
      "additionalProp2": {},
      "additionalProp3": {}
    },
    "data_3": {
      "additionalProp1": {},
      "additionalProp2": {},
      "additionalProp3": {}
    },
    "data_4": {
      "additionalProp1": {},
      "additionalProp2": {},
      "additionalProp3": {}
    },
    "data_5": {
      "additionalProp1": {},
      "additionalProp2": {},
      "additionalProp3": {}
    }
  }
}

```
- example with Modbus RTU
``` json
{
  "id": 5,
  "name": "Truck Scale 2",
  "model": "TS-5000",
  "direction": "IMPORT",
  "location_id": 1,
  "location_name": "string",
  "manufacturer_id": 1,
  "manufacturer_name": "string",
  "manufacturer_code": "string",
  "protocol_id": 3,
  "protocol_name": "Modbus RTU",
  "protocol_code": "MODBUS_RTU",
  "is_active": true,
  "created_at": "2026-01-08T09:48:59.183752Z",
  "created_by": "admin",
  "updated_at": "2026-01-08T09:48:59.183752Z",
  "updated_by": "admin",
  "scale_config": {
    "protocol": "MODBUS_RTU",
    "scale_id": 5,
    "poll_interval": 1000,
    "conn_params": {
      "parity": "even",
      "unit_id": 1,
      "com_port": "COM3",
      "baud_rate": 9600,
      "data_bits": 8,
      "stop_bits": 1
    },
    "data_1": {
      "name": "Weight",
      "is_used": true,
      "data_type": "int32",
      "byte_order": "big_endian",
      "function_code": 3,
      "num_registers": 2,
      "start_register": 0
    },
    "data_2": {
      "is_used": false
    },
    "data_3": {
      "is_used": false
    },
    "data_4": {
      "is_used": false
    },
    "data_5": {
      "is_used": false
    }
  }
}
```
### PUT http://localhost:8080/api/v1/scales/{id}
* Parameters:
-  `id` :integer
* Payload:
``` json
{
  "name": "Truck Scale 1",
  "model": "TS-5000",
  "direction": "IMPORT",
  "protocol": "MODBUS_TCP",
  "location_id": 1,
  "manufacturer_id": 1,
  "protocol_id": 1,
  "is_active": true,
  "poll_interval": 1000,
  "conn_params": {
    "ip": "192.168.1.10",
    "port": 502
  },
  "data_1": {
    "name": "Weight",
    "start_registers": 40001,
    "num_registers": 2,
    "is_used": true
  },
  "data_2": {
    "additionalProp1": {},
    "additionalProp2": {},
    "additionalProp3": {}
  },
  "data_3": {
    "additionalProp1": {},
    "additionalProp2": {},
    "additionalProp3": {}
  },
  "data_4": {
    "additionalProp1": {},
    "additionalProp2": {},
    "additionalProp3": {}
  },
  "data_5": {
    "additionalProp1": {},
    "additionalProp2": {},
    "additionalProp3": {}
  }
}
```

* Response code: 200
* Response body:
``` json
{
  "id": 4,
  "name": "Truck Scale 1",
  "model": "TS-5000",
  "direction": "IMPORT",
  "location_id": 1,
  "location_name": "string",
  "manufacturer_id": 1,
  "manufacturer_name": "string",
  "manufacturer_code": "string",
  "protocol_id": 1,
  "protocol_name": "Modbus TCP",
  "protocol_code": "MODBUS_TCP",
  "is_active": true,
  "created_at": "2026-01-08T09:31:18.690924Z",
  "created_by": "admin",
  "updated_at": "2026-01-08T09:31:18.690924Z",
  "updated_by": "admin",
  "scale_config": {
    "protocol": "MODBUS_TCP",
    "scale_id": 4,
    "poll_interval": 1000,
    "conn_params": {
      "ip": "192.168.1.10",
      "port": 502
    },
    "data_1": {
      "name": "Weight",
      "is_used": true,
      "num_registers": 2,
      "start_registers": 40002
    },
    "data_2": {
      "additionalProp1": {},
      "additionalProp2": {},
      "additionalProp3": {}
    },
    "data_3": {
      "additionalProp1": {},
      "additionalProp2": {},
      "additionalProp3": {}
    },
    "data_4": {
      "additionalProp1": {},
      "additionalProp2": {},
      "additionalProp3": {}
    },
    "data_5": {
      "additionalProp1": {},
      "additionalProp2": {},
      "additionalProp3": {}
    }
  }
}
```
### DELETE http://localhost:8080/api/v1/scales/{id}
* Parameters:
-  `id` :integer
* Response code: 204 - xóa thành công
