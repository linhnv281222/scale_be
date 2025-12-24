package org.facenet.service.scale.engine.util;

import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;

/**
 * Lớp tiện ích chuyển đổi dữ liệu Modbus Registers sang String
 * 
 * Vì dữ liệu từ cân thường là:
 * - Float 32-bit (chiếm 2 thanh ghi 16-bit)
 * - Int 16-bit (chiếm 1 thanh ghi)
 * 
 * Hàm này biến chúng thành String trước khi đẩy vào Queue
 */
@Slf4j
public class ModbusDataConverter {
    
    /**
     * Chuyển đổi mảng thanh ghi Modbus sang String THEO DATA_TYPE từ config
     * 
     * @param registers Mảng thanh ghi đọc được từ Modbus (mỗi register là 16-bit)
     * @param dataType Kiểu dữ liệu từ config: "integer", "float", "string", "boolean"
     * @return String representation của dữ liệu
     */
    public static String registersToString(int[] registers, String dataType) {
        if (registers == null || registers.length == 0) {
            return null;
        }
        
        try {
            // Xử lý theo data_type từ config
            if (dataType == null) {
                dataType = "auto"; // Fallback to auto-detect
            }
            
            switch (dataType.toLowerCase()) {
                case "integer":
                case "int":
                    if (registers.length == 1) {
                        // 16-bit unsigned integer
                        return String.valueOf(registers[0] & 0xFFFF);
                    } else if (registers.length == 2) {
                        // 32-bit integer (Big Endian)
                        int int32 = (registers[0] << 16) | (registers[1] & 0xFFFF);
                        return String.valueOf(int32);
                    }
                    break;
                    
                case "float":
                    if (registers.length == 2) {
                        // 32-bit Float (Big Endian, IEEE 754)
                        int combined = (registers[0] << 16) | (registers[1] & 0xFFFF);
                        float floatValue = Float.intBitsToFloat(combined);
                        return String.format("%.2f", floatValue);
                    } else if (registers.length == 4) {
                        // 64-bit Double
                        long combined = ((long)registers[0] << 48) | 
                                      ((long)registers[1] << 32) |
                                      ((long)registers[2] << 16) | 
                                      (registers[3] & 0xFFFF);
                        double doubleValue = Double.longBitsToDouble(combined);
                        return String.format("%.2f", doubleValue);
                    }
                    break;
                    
                case "boolean":
                case "bool":
                    // Boolean: 0 = false, non-zero = true
                    return String.valueOf(registers[0] != 0);
                    
                case "string":
                case "text":
                    // ASCII string: mỗi register chứa 2 ký tự
                    StringBuilder sb = new StringBuilder();
                    for (int reg : registers) {
                        char ch1 = (char)((reg >> 8) & 0xFF);
                        char ch2 = (char)(reg & 0xFF);
                        if (ch1 != 0) sb.append(ch1);
                        if (ch2 != 0) sb.append(ch2);
                    }
                    return sb.toString().trim();
                    
                default:
                    // Auto-detect (backward compatible)
                    return registersToStringAuto(registers);
            }
            
            // Fallback nếu không match case nào
            return registersToStringAuto(registers);
            
        } catch (Exception e) {
            log.error("Error converting registers to string: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Chuyển đổi mảng thanh ghi Modbus sang String (Auto-detect - Backward compatible)
     * 
     * @param registers Mảng thanh ghi đọc được từ Modbus (mỗi register là 16-bit)
     * @return String representation của dữ liệu
     */
    public static String registersToString(int[] registers) {
        return registersToStringAuto(registers);
    }
    
    /**
     * Auto-detect data type (legacy method)
     */
    private static String registersToStringAuto(int[] registers) {
        if (registers == null || registers.length == 0) {
            return null;
        }
        
        try {
            // Nếu là 1 thanh ghi -> Trả về số nguyên 16-bit
            if (registers.length == 1) {
                return String.valueOf(registers[0]);
            }
            
            // Nếu là 2 thanh ghi -> Thường là Float 32-bit (Chuẩn IEEE 754)
            if (registers.length == 2) {
                // Gộp 2 thanh ghi 16-bit thành 1 số 32-bit
                // registers[0] là High Word, registers[1] là Low Word
                int combined = (registers[0] << 16) | (registers[1] & 0xFFFF);
                
                // Chuyển đổi từ bits sang Float theo chuẩn IEEE 754
                float floatValue = Float.intBitsToFloat(combined);
                
                // Trả về dạng chuỗi với 2 chữ số thập phân "150.50"
                return String.format("%.2f", floatValue);
            }
            
            // Nếu nhiều hơn 2 thanh ghi -> Trả về dạng mảng
            // VD: "[100, 200, 300]"
            return Arrays.toString(registers);
            
        } catch (Exception e) {
            log.error("Error converting registers to string: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Chuyển đổi 2 thanh ghi sang Float (Big Endian)
     * High Word trước, Low Word sau
     */
    public static Float registersToFloatBE(int[] registers) {
        if (registers == null || registers.length != 2) {
            return null;
        }
        int combined = (registers[0] << 16) | (registers[1] & 0xFFFF);
        return Float.intBitsToFloat(combined);
    }
    
    /**
     * Chuyển đổi 2 thanh ghi sang Float (Little Endian)
     * Low Word trước, High Word sau
     */
    public static Float registersToFloatLE(int[] registers) {
        if (registers == null || registers.length != 2) {
            return null;
        }
        int combined = (registers[1] << 16) | (registers[0] & 0xFFFF);
        return Float.intBitsToFloat(combined);
    }
    
    /**
     * Chuyển đổi 1 thanh ghi sang Integer (16-bit unsigned)
     */
    public static Integer registerToInt(int register) {
        return register & 0xFFFF;
    }
    
    /**
     * Chuyển đổi 2 thanh ghi sang Integer 32-bit (Big Endian)
     */
    public static Integer registersToInt32BE(int[] registers) {
        if (registers == null || registers.length != 2) {
            return null;
        }
        return (registers[0] << 16) | (registers[1] & 0xFFFF);
    }
}
