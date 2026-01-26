package nvt.backend.services.vehicle;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.QueryApi;
import com.influxdb.client.WriteApiBlocking;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import nvt.backend.dto.vehicle.AvailabilityStatisticsDTO;
import nvt.backend.dto.vehicle.DistanceStatisticsDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class VehicleTelemetryService {

    private static final Logger log = LoggerFactory.getLogger(VehicleTelemetryService.class);
    private static final int MAX_DATA_POINTS = 100;

    private final InfluxDBClient influxDBClient;
    private final QueryApi queryApi;
    private final String org;
    private final String bucket;

    public VehicleTelemetryService(
            InfluxDBClient influxDBClient,
            @Value("${influxdb.org}") String org,
            @Value("${influxdb.bucket}") String bucket) {
        this.influxDBClient = influxDBClient;
        this.queryApi = influxDBClient.getQueryApi();
        this.org = org;
        this.bucket = bucket;
    }

    private static final String MEASUREMENT = "vehicle_telemetry";

    /**
     * Records a complete telemetry point with all available data.
     * This is the primary method for recording vehicle telemetry.
     */
    @org.springframework.scheduling.annotation.Async("telemetryExecutor")
    public void recordTelemetry(Long vehicleId, String licensePlate, boolean online,
                                 Double distance, Double latitude, Double longitude, String state) {
        try {
            WriteApiBlocking writeApi = influxDBClient.getWriteApiBlocking();
            Point point = Point.measurement(MEASUREMENT)
                    .addTag("vehicleId", String.valueOf(vehicleId))
                    .addTag("licensePlate", licensePlate)
                    .addField("online", online ? 1 : 0);
            
            if (distance != null) {
                point.addField("distance", distance);
            }
            if (latitude != null && longitude != null) {
                point.addField("latitude", latitude);
                point.addField("longitude", longitude);
            }
            if (state != null) {
                point.addField("state", state);
            }
            
            point.time(Instant.now(), WritePrecision.MS);
            writeApi.writePoint(point);
            log.debug("Recorded telemetry for vehicle {}: online={}, distance={}, lat={}, lon={}, state={}",
                    vehicleId, online, distance, latitude, longitude, state);
        } catch (Exception e) {
            log.error("Failed to record telemetry for vehicle {}", vehicleId, e);
        }
    }

    /**
     * Records a heartbeat (online status only).
     */
    @org.springframework.scheduling.annotation.Async("telemetryExecutor")
    public void recordHeartbeat(Long vehicleId, String licensePlate, boolean online) {
        recordTelemetry(vehicleId, licensePlate, online, null, null, null, null);
    }

    /**
     * Records a state change.
     */
    @org.springframework.scheduling.annotation.Async("telemetryExecutor")
    public void recordStateChange(Long vehicleId, String licensePlate, String state) {
        recordTelemetry(vehicleId, licensePlate, true, null, null, null, state);
    }

    /**
     * Records distance traveled.
     */
    @org.springframework.scheduling.annotation.Async("telemetryExecutor")
    public void recordDistance(Long vehicleId, String licensePlate, Double distance) {
        recordTelemetry(vehicleId, licensePlate, true, distance, null, null, null);
    }

    /**
     * Records location update.
     */
    @org.springframework.scheduling.annotation.Async("telemetryExecutor")
    public void recordLocation(Long vehicleId, String licensePlate, Double latitude, Double longitude) {
        recordTelemetry(vehicleId, licensePlate, true, null, latitude, longitude, null);
    }

    /**
     * Records full telemetry update (distance + location) - used by VehicleMessageListener.
     */
    @org.springframework.scheduling.annotation.Async("telemetryExecutor")
    public void recordFullTelemetry(Long vehicleId, String licensePlate, Double distance,
                                     Double latitude, Double longitude) {
        recordTelemetry(vehicleId, licensePlate, true, distance, latitude, longitude, null);
    }

    public List<Map<String, Object>> getAvailabilityHistory(Long vehicleId, String range) {
        List<Map<String, Object>> results = new ArrayList<>();
        try {
            String flux = String.format(
                    "from(bucket: \"%s\") " +
                            "|> range(start: %s) " +
                            "|> filter(fn: (r) => r._measurement == \"%s\") " +
                            "|> filter(fn: (r) => r.vehicleId == \"%d\") " +
                            "|> filter(fn: (r) => r._field == \"online\") " +
                            "|> aggregateWindow(every: 10m, fn: mean, createEmpty: true) " +
                            "|> fill(value: 0.0) " +
                            "|> aggregateWindow(every: 1h, fn: mean, createEmpty: false) " +
                            "|> sort(columns: [\"_time\"], desc: true) " +
                            "|> limit(n: %d)",
                    bucket, range, MEASUREMENT, vehicleId, MAX_DATA_POINTS
            );

            List<FluxTable> tables = queryApi.query(flux, org);

            for (FluxTable table : tables) {
                for (FluxRecord record : table.getRecords()) {
                    Map<String, Object> entry = new HashMap<>();
                    entry.put("time", record.getTime());
                    entry.put("online", record.getValue());
                    results.add(entry);
                }
            }
        } catch (Exception e) {
            log.error("Failed to query availability history for vehicle {}", vehicleId, e);
        }
        return results;
    }

    public List<Map<String, Object>> getDistanceHistory(Long vehicleId, String range) {
        List<Map<String, Object>> results = new ArrayList<>();
        try {
            String flux = String.format(
                    "from(bucket: \"%s\") " +
                            "|> range(start: %s) " +
                            "|> filter(fn: (r) => r._measurement == \"%s\") " +
                            "|> filter(fn: (r) => r.vehicleId == \"%d\") " +
                            "|> filter(fn: (r) => r._field == \"distance\") " +
                            "|> aggregateWindow(every: 1h, fn: sum, createEmpty: false) " +
                            "|> sort(columns: [\"_time\"], desc: true) " +
                            "|> limit(n: %d)",
                    bucket, range, MEASUREMENT, vehicleId, MAX_DATA_POINTS
            );

            List<FluxTable> tables = queryApi.query(flux, org);

            for (FluxTable table : tables) {
                for (FluxRecord record : table.getRecords()) {
                    Map<String, Object> entry = new HashMap<>();
                    entry.put("time", record.getTime());
                    entry.put("distance", record.getValue());
                    results.add(entry);
                }
            }
        } catch (Exception e) {
            log.error("Failed to query distance history for vehicle {}", vehicleId, e);
        }
        return results;
    }


    @Cacheable(value = "distanceStats", key = "#vehicleId + '-' + #startDate + '-' + #endDate")
    public DistanceStatisticsDTO getAggregatedDistance(Long vehicleId, String licensePlate, 
                                                        LocalDate startDate, LocalDate endDate) {
        long daysBetween = ChronoUnit.DAYS.between(startDate, endDate);
        
        String aggregationType;
        String windowPeriod;
        
        if (daysBetween <= 14) {
            aggregationType = "daily";
            windowPeriod = "1d";
        } else if (daysBetween <= 90) {
            aggregationType = "weekly";
            windowPeriod = "1w";
        } else {
            aggregationType = "monthly";
            windowPeriod = "1mo";
        }

        List<DistanceStatisticsDTO.DistanceDataPoint> dataPoints = new ArrayList<>();
        double totalDistance = 0.0;

        try {
            String startTimeStr = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toString();
            String endTimeStr = endDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toString();

            String flux = String.format(
                    "from(bucket: \"%s\") " +
                            "|> range(start: %s, stop: %s) " +
                            "|> filter(fn: (r) => r._measurement == \"%s\") " +
                            "|> filter(fn: (r) => r.vehicleId == \"%d\") " +
                            "|> filter(fn: (r) => r._field == \"distance\") " +
                            "|> aggregateWindow(every: %s, fn: sum, createEmpty: false) " +
                            "|> sort(columns: [\"_time\"], desc: false) " +
                            "|> limit(n: %d)",
                    bucket, startTimeStr, endTimeStr, MEASUREMENT, vehicleId, windowPeriod, MAX_DATA_POINTS
            );

            List<FluxTable> tables = queryApi.query(flux, org);

            DateTimeFormatter labelFormatter = switch (aggregationType) {
                case "daily" -> DateTimeFormatter.ofPattern("MMM dd");
                case "weekly" -> DateTimeFormatter.ofPattern("MMM dd");
                default -> DateTimeFormatter.ofPattern("MMM yyyy");
            };

            for (FluxTable table : tables) {
                for (FluxRecord record : table.getRecords()) {
                    if (record.getTime() != null && record.getValue() != null) {
                        LocalDate pointDate = record.getTime().atZone(ZoneId.systemDefault()).toLocalDate();
                        Double distance = ((Number) record.getValue()).doubleValue();
                        totalDistance += distance;

                        LocalDate pointEndDate = switch (aggregationType) {
                            case "daily" -> pointDate;
                            case "weekly" -> pointDate.plusDays(6);
                            default -> pointDate.plusMonths(1).minusDays(1);
                        };

                        String label = switch (aggregationType) {
                            case "weekly" -> pointDate.format(labelFormatter) + " - " + pointEndDate.format(labelFormatter);
                            default -> pointDate.format(labelFormatter);
                        };

                        dataPoints.add(DistanceStatisticsDTO.DistanceDataPoint.builder()
                                .label(label)
                                .startDate(pointDate)
                                .endDate(pointEndDate)
                                .distance(distance)
                                .build());
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to query aggregated distance for vehicle {}", vehicleId, e);
        }

        return DistanceStatisticsDTO.builder()
                .vehicleId(vehicleId)
                .licensePlate(licensePlate)
                .startDate(startDate)
                .endDate(endDate)
                .aggregationType(aggregationType)
                .totalDistance(totalDistance)
                .dataPoints(dataPoints)
                .build();
    }


    @Cacheable(value = "availabilityStats", key = "#vehicleId + '-' + #startTime.toEpochMilli() + '-' + #endTime.toEpochMilli()")
    public AvailabilityStatisticsDTO getAggregatedAvailability(Long vehicleId, String licensePlate,
                                                                Instant startTime, Instant endTime) {
        Duration totalDuration = Duration.between(startTime, endTime);
        long totalHours = totalDuration.toHours();

        String aggregationType;
        Duration windowDuration;

        if (totalHours <= 24) {
            aggregationType = "hourly";
            windowDuration = Duration.ofHours(1);
        } else if (totalHours <= 720) {
            aggregationType = "daily";
            windowDuration = Duration.ofDays(1);
        } else {
            aggregationType = "weekly";
            windowDuration = Duration.ofDays(7);
        }

        Map<Instant, Double> windowData = new LinkedHashMap<>();
        Instant alignedStart = switch (aggregationType) {
            case "hourly" -> startTime.truncatedTo(ChronoUnit.HOURS);
            case "daily" -> startTime.truncatedTo(ChronoUnit.DAYS);
            default -> startTime.truncatedTo(ChronoUnit.DAYS);
        };

        Instant alignedEnd = switch (aggregationType) {
            case "hourly" -> endTime.truncatedTo(ChronoUnit.HOURS).plus(Duration.ofHours(1));
            case "daily" -> endTime.truncatedTo(ChronoUnit.DAYS).plus(Duration.ofDays(1));
            default -> endTime.truncatedTo(ChronoUnit.DAYS).plus(Duration.ofDays(7));
        };

        Instant windowStart = alignedStart;
        while (windowStart.isBefore(alignedEnd)) {
            windowData.put(windowStart, 0.0);
            windowStart = windowStart.plus(windowDuration);
        }

        try {
            String windowPeriod = aggregationType.equals("hourly") ? "1h" :
                    aggregationType.equals("daily") ? "1d" : "1w";

            String flux = String.format(
                    "from(bucket: \"%s\") " +
                            "|> range(start: %s, stop: %s) " +
                            "|> filter(fn: (r) => r._measurement == \"%s\") " +
                            "|> filter(fn: (r) => r.vehicleId == \"%d\") " +
                            "|> filter(fn: (r) => r._field == \"online\") " +
                            "|> aggregateWindow(every: 10m, fn: mean, createEmpty: true) " +
                            "|> fill(value: 0.0) " +
                            "|> aggregateWindow(every: %s, fn: mean, createEmpty: false) " +
                            "|> sort(columns: [\"_time\"], desc: false) " +
                            "|> limit(n: %d)",
                    bucket, alignedStart.toString(), alignedEnd.toString(), MEASUREMENT, vehicleId, windowPeriod, MAX_DATA_POINTS
            );

            log.info("Flux query: {}", flux);
            List<FluxTable> tables = queryApi.query(flux, org);
            log.info("InfluxDB returned {} tables", tables.size());

            for (FluxTable table : tables) {
                for (FluxRecord record : table.getRecords()) {
                    if (record.getTime() != null && record.getValue() != null) {
                        Instant pointTime = record.getTime();
                        double onlineRatio = ((Number) record.getValue()).doubleValue();
                        log.info("InfluxDB point: time={}, value={}", pointTime, onlineRatio);

                        boolean matched = false;
                        for (Instant bucket : windowData.keySet()) {
                            Instant bucketEnd = bucket.plus(windowDuration);
                            // Match point to bucket:
                            // - If pointTime is within [bucket, bucketEnd] (inclusive on both ends)
                            // Use truncatedTo for comparison to avoid nanosecond precision issues
                            Instant pointTruncated = pointTime.truncatedTo(ChronoUnit.SECONDS);
                            Instant bucketTruncated = bucket.truncatedTo(ChronoUnit.SECONDS);
                            Instant bucketEndTruncated = bucketEnd.truncatedTo(ChronoUnit.SECONDS);
                            
                            if (!pointTruncated.isBefore(bucketTruncated) && !pointTruncated.isAfter(bucketEndTruncated)) {
                                windowData.put(bucket, onlineRatio);
                                log.info("Matched to bucket: {} (pointTruncated={}, bucketEnd={})", bucket, pointTruncated, bucketEndTruncated);
                                matched = true;
                                break;
                            }
                        }
                        if (!matched) {
                            log.warn("No bucket match for pointTime={}", pointTime);
                        }
                    }
                }
            }
            log.info("Window data after matching: {}", windowData);
        } catch (Exception e) {
            log.error("Failed to query aggregated availability for vehicle {}", vehicleId, e);
        }

        List<AvailabilityStatisticsDTO.AvailabilityDataPoint> dataPoints = new ArrayList<>();
        long totalOnlineSeconds = 0;
        long totalOfflineSeconds = 0;

        DateTimeFormatter labelFormatter = switch (aggregationType) {
            case "hourly" -> DateTimeFormatter.ofPattern("MMM dd HH:mm").withZone(ZoneId.systemDefault());
            case "daily" -> DateTimeFormatter.ofPattern("MMM dd").withZone(ZoneId.systemDefault());
            default -> DateTimeFormatter.ofPattern("MMM dd").withZone(ZoneId.systemDefault());
        };

        for (Map.Entry<Instant, Double> entry : windowData.entrySet()) {
            Instant pointTime = entry.getKey();
            double onlineRatio = entry.getValue();
            Instant pointEndTime = pointTime.plus(windowDuration);

            if (pointEndTime.isAfter(endTime)) {
                pointEndTime = endTime;
            }

            // Use actual window duration (handles partial windows at the end)
            long windowSeconds = Duration.between(pointTime, pointEndTime).getSeconds();
            long onlineSeconds = (long) (windowSeconds * onlineRatio);
            long offlineSeconds = windowSeconds - onlineSeconds;

            totalOnlineSeconds += onlineSeconds;
            totalOfflineSeconds += offlineSeconds;

            double onlinePct = onlineSeconds * 100.0 / windowSeconds;
            double offlinePct = offlineSeconds * 100.0 / windowSeconds;

            String label = switch (aggregationType) {
                case "weekly" -> labelFormatter.format(pointTime) + " - " + labelFormatter.format(pointEndTime);
                default -> labelFormatter.format(pointTime);
            };

            dataPoints.add(AvailabilityStatisticsDTO.AvailabilityDataPoint.builder()
                    .label(label)
                    .startTime(pointTime)
                    .endTime(pointEndTime)
                    .onlineSeconds(onlineSeconds)
                    .offlineSeconds(offlineSeconds)
                    .onlinePercentage(Math.round(onlinePct * 100.0) / 100.0)
                    .offlinePercentage(Math.round(offlinePct * 100.0) / 100.0)
                    .build());
        }

        long totalSeconds = totalOnlineSeconds + totalOfflineSeconds;
        double overallOnlinePct = totalSeconds > 0 ? (totalOnlineSeconds * 100.0 / totalSeconds) : 0;
        double overallOfflinePct = totalSeconds > 0 ? (totalOfflineSeconds * 100.0 / totalSeconds) : 0;

        return AvailabilityStatisticsDTO.builder()
                .vehicleId(vehicleId)
                .licensePlate(licensePlate)
                .startTime(startTime)
                .endTime(endTime)
                .aggregationType(aggregationType)
                .totalOnlineSeconds(totalOnlineSeconds)
                .totalOfflineSeconds(totalOfflineSeconds)
                .onlinePercentage(Math.round(overallOnlinePct * 100.0) / 100.0)
                .offlinePercentage(Math.round(overallOfflinePct * 100.0) / 100.0)
                .dataPoints(dataPoints)
                .build();
    }

    public List<Map<String, Object>> getRawAvailabilityData(Long vehicleId, Instant startTime, Instant endTime) {
        List<Map<String, Object>> results = new ArrayList<>();
        try {
            String flux = String.format(
                    "from(bucket: \"%s\") " +
                            "|> range(start: %s, stop: %s) " +
                            "|> filter(fn: (r) => r._measurement == \"%s\") " +
                            "|> filter(fn: (r) => r.vehicleId == \"%d\") " +
                            "|> filter(fn: (r) => r._field == \"online\") " +
                            "|> sort(columns: [\"_time\"], desc: false)",
                    bucket, startTime.toString(), endTime.toString(), MEASUREMENT, vehicleId
            );

            QueryApi queryApi = influxDBClient.getQueryApi();
            List<FluxTable> tables = queryApi.query(flux, org);

            for (FluxTable table : tables) {
                for (FluxRecord record : table.getRecords()) {
                    Map<String, Object> entry = new HashMap<>();
                    entry.put("time", record.getTime());
                    entry.put("online", record.getValue());
                    results.add(entry);
                }
            }
        } catch (Exception e) {
            log.error("Failed to query raw availability for vehicle {}", vehicleId, e);
        }
        return results;
    }
}