package org.facenet.service.scale;

import lombok.RequiredArgsConstructor;
import org.facenet.common.exception.ValidationException;
import org.facenet.common.pagination.PageRequestDto;
import org.facenet.common.pagination.PageResponseDto;
import org.facenet.common.specification.GenericSpecification;
import org.facenet.dto.scale.WeighingDataDto;
import org.facenet.entity.scale.Scale;
import org.facenet.entity.scale.ScaleConfig;
import org.facenet.entity.scale.WeighingLog;
import org.facenet.repository.scale.WeighingLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WeighingLogServiceImpl implements WeighingLogService {

    private final WeighingLogRepository weighingLogRepository;

    @Override
    public Page<WeighingDataDto.LogResponse> getHistory(WeighingDataDto.LogQueryRequest request) {
        if (request == null) {
            throw new ValidationException("request must not be null");
        }

        OffsetDateTime startTime = request.getStartTime();
        OffsetDateTime endTime = request.getEndTime();

        if ((startTime == null) != (endTime == null)) {
            throw new ValidationException("startTime and endTime must be provided together");
        }
        if (startTime != null && startTime.isAfter(endTime)) {
            throw new ValidationException("startTime must be <= endTime");
        }

        int page = request.getPage() != null ? request.getPage() : 0;
        int size = request.getSize() != null ? request.getSize() : 20;
        if (page < 0) {
            throw new ValidationException("page must be >= 0");
        }
        if (size <= 0) {
            throw new ValidationException("size must be > 0");
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Specification<WeighingLog> spec = (root, query, cb) -> {
            var predicates = cb.conjunction();

            if (request.getScaleId() != null) {
                predicates = cb.and(predicates, cb.equal(root.get("scaleId"), request.getScaleId()));
            }
            if (startTime != null && endTime != null) {
                predicates = cb.and(predicates, cb.between(root.get("createdAt"), startTime, endTime));
            }

            return predicates;
        };

        Page<WeighingLog> logs = weighingLogRepository.findAll(spec, pageable);

        return logs.map(this::toLogResponse);
    }

    private WeighingDataDto.LogResponse toLogResponse(WeighingLog log) {
        Scale scale = log.getScale();
        ScaleConfig config = scale != null ? scale.getConfig() : null;
        return WeighingDataDto.LogResponse.builder()
                .scaleId(log.getScaleId())
                .scaleName(scale != null ? scale.getName() : null)
                .createdAt(log.getCreatedAt())
                .lastTime(log.getLastTime())
                .data1(log.getData1())
                .data2(log.getData2())
                .data3(log.getData3())
                .data4(log.getData4())
                .data5(log.getData5())
                .dataValues(buildDataValues(log, config))
                .build();
    }

    private Map<String, WeighingDataDto.DataFieldValue> buildDataValues(WeighingLog log, ScaleConfig config) {
        Map<String, WeighingDataDto.DataFieldValue> values = new LinkedHashMap<>();

        values.put("data_1", toDataFieldValue(log.getData1(), config != null ? config.getData1() : null, "Data 1"));
        values.put("data_2", toDataFieldValue(log.getData2(), config != null ? config.getData2() : null, "Data 2"));
        values.put("data_3", toDataFieldValue(log.getData3(), config != null ? config.getData3() : null, "Data 3"));
        values.put("data_4", toDataFieldValue(log.getData4(), config != null ? config.getData4() : null, "Data 4"));
        values.put("data_5", toDataFieldValue(log.getData5(), config != null ? config.getData5() : null, "Data 5"));

        return values;
    }

    private WeighingDataDto.DataFieldValue toDataFieldValue(String rawValue, Map<String, Object> dataConfig, String defaultName) {
        String name = defaultName;
        boolean isUsed = false;

        if (dataConfig != null) {
            Object nameObj = dataConfig.get("name");
            if (nameObj != null && !nameObj.toString().isBlank()) {
                name = nameObj.toString();
            }

            Object usedObj = dataConfig.get("is_used");
            if (usedObj instanceof Boolean b) {
                isUsed = b;
            } else if (usedObj != null) {
                isUsed = Boolean.parseBoolean(usedObj.toString());
            }
        }

        return WeighingDataDto.DataFieldValue.builder()
                .value(rawValue)
                .name(name)
                .isUsed(isUsed)
                .build();
    }
    
    @Override
    public PageResponseDto<WeighingDataDto.HistoryResponse> getWeighingHistory(
            PageRequestDto pageRequest, 
            Map<String, String> filters) {
        
        // Build pageable with default sort by createdAt DESC if not specified
        Pageable pageable;
        if (pageRequest.getSort() == null || pageRequest.getSort().isBlank()) {
            pageable = PageRequest.of(
                pageRequest.getPage(), 
                pageRequest.getSize(), 
                Sort.by(Sort.Direction.DESC, "createdAt")
            );
        } else {
            pageable = pageRequest.toPageRequest();
        }
        
        // Build filter specification
        Specification<WeighingLog> filterSpec = buildFilterSpecification(filters);
        
        // Build search specification if provided
        Specification<WeighingLog> searchSpec = Specification.where(null);
        if (pageRequest.getSearch() != null && !pageRequest.getSearch().isBlank()) {
            searchSpec = buildSearchSpecification(pageRequest.getSearch());
        }
        
        // Combine specifications
        Specification<WeighingLog> finalSpec = Specification.where(filterSpec).and(searchSpec);
        
        // Execute query
        Page<WeighingLog> logPage = weighingLogRepository.findAll(finalSpec, pageable);
        
        // Convert to HistoryResponse
        Page<WeighingDataDto.HistoryResponse> responsePage = logPage.map(this::toHistoryResponse);
        
        return PageResponseDto.from(responsePage);
    }
    
    private Specification<WeighingLog> buildFilterSpecification(Map<String, String> filters) {
        return (root, query, criteriaBuilder) -> {
            var predicates = criteriaBuilder.conjunction();
            
            if (filters == null || filters.isEmpty()) {
                return predicates;
            }
            
            // Filter by scaleId
            if (filters.containsKey("scaleId") && filters.get("scaleId") != null) {
                predicates = criteriaBuilder.and(predicates, 
                    criteriaBuilder.equal(root.get("scaleId"), Long.parseLong(filters.get("scaleId"))));
            }
            
            // Filter by scale code (join to scale)
            if (filters.containsKey("scaleCode") && filters.get("scaleCode") != null) {
                var scaleJoin = root.join("scale");
                predicates = criteriaBuilder.and(predicates,
                    criteriaBuilder.like(
                        criteriaBuilder.lower(scaleJoin.get("name")),
                        "%" + filters.get("scaleCode").toLowerCase() + "%"
                    ));
            }
            
            // Filter by direction (join to scale)
            if (filters.containsKey("direction") && filters.get("direction") != null) {
                var scaleJoin = root.join("scale");
                predicates = criteriaBuilder.and(predicates,
                    criteriaBuilder.equal(scaleJoin.get("direction"), 
                        org.facenet.entity.scale.ScaleDirection.valueOf(filters.get("direction").toUpperCase())));
            }
            
            // Filter by locationId (join to scale -> location)
            if (filters.containsKey("locationId") && filters.get("locationId") != null) {
                var scaleJoin = root.join("scale");
                var locationJoin = scaleJoin.join("location");
                predicates = criteriaBuilder.and(predicates,
                    criteriaBuilder.equal(locationJoin.get("id"), Long.parseLong(filters.get("locationId"))));
            }
            
            // Filter by protocolId (join to scale -> protocol)
            if (filters.containsKey("protocolId") && filters.get("protocolId") != null) {
                var scaleJoin = root.join("scale");
                var protocolJoin = scaleJoin.join("protocol");
                predicates = criteriaBuilder.and(predicates,
                    criteriaBuilder.equal(protocolJoin.get("id"), Long.parseLong(filters.get("protocolId"))));
            }
            
            // Filter by startTime
            if (filters.containsKey("startTime") && filters.get("startTime") != null) {
                OffsetDateTime startTime = OffsetDateTime.parse(filters.get("startTime"));
                predicates = criteriaBuilder.and(predicates,
                    criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), startTime));
            }
            
            // Filter by endTime
            if (filters.containsKey("endTime") && filters.get("endTime") != null) {
                OffsetDateTime endTime = OffsetDateTime.parse(filters.get("endTime"));
                predicates = criteriaBuilder.and(predicates,
                    criteriaBuilder.lessThanOrEqualTo(root.get("createdAt"), endTime));
            }
            
            return predicates;
        };
    }
    
    private Specification<WeighingLog> buildSearchSpecification(String search) {
        return (root, query, criteriaBuilder) -> {
            var scaleJoin = root.join("scale");
            String searchPattern = "%" + search.toLowerCase() + "%";
            
            return criteriaBuilder.or(
                criteriaBuilder.like(criteriaBuilder.lower(scaleJoin.get("name")), searchPattern),
                criteriaBuilder.like(criteriaBuilder.lower(scaleJoin.get("model")), searchPattern)
            );
        };
    }
    
    private WeighingDataDto.HistoryResponse toHistoryResponse(WeighingLog log) {
        Scale scale = log.getScale();
        ScaleConfig config = scale != null ? scale.getConfig() : null;
        
        return WeighingDataDto.HistoryResponse.builder()
                .scaleId(log.getScaleId())
                .scaleCode(scale != null ? scale.getName() : null)
                .scaleName(scale != null ? scale.getName() : null)
                .scaleDirection(scale != null && scale.getDirection() != null 
                    ? scale.getDirection().name() : null)
                .locationName(scale != null && scale.getLocation() != null 
                    ? scale.getLocation().getName() : null)
                .protocolName(scale != null && scale.getProtocol() != null 
                    ? scale.getProtocol().getName() : null)
                .createdAt(log.getCreatedAt())
                .lastTime(log.getLastTime())
                .data1(log.getData1())
                .data2(log.getData2())
                .data3(log.getData3())
                .data4(log.getData4())
                .data5(log.getData5())
                .dataValues(buildDataValues(log, config))
                .build();
    }
}
