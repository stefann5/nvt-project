package nvt.backend.services.warehouse;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.QueryApi;
import com.influxdb.client.WriteApiBlocking;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import nvt.backend.dto.warehouse.TemperatureStatisticsDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
public class WarehouseTelemetryService {

    private static final Logger log = LoggerFactory.getLogger(WarehouseTelemetryService.class);
    private static final int MAX_DATA_POINTS = 100;

    private final InfluxDBClient influxDBClient;
    private final QueryApi queryApi;
    private final String org;
    private final String bucket;

    public WarehouseTelemetryService(
            InfluxDBClient influxDBClient,
            @Value("${influxdb.org}") String org,
            @Value("${influxdb.bucket.warehouse:warehouse_telemetry}") String bucket) {
        this.influxDBClient = influxDBClient;
        this.queryApi = influxDBClient.getQueryApi();
        this.org = org;
        this.bucket = bucket;
    }

    @Async("telemetryExecutor")
    public void recordHeartbeat(Long warehouseId, String warehouseName, boolean online) {
        try {
            WriteApiBlocking writeApi = influxDBClient.getWriteApiBlocking();
            Point point = Point.measurement("warehouse_availability")
                    .addTag("warehouseId", String.valueOf(warehouseId))
                    .addTag("warehouseName", warehouseName)
                    .addField("online", online ? 1 : 0)
                    .time(Instant.now(), WritePrecision.MS);
            writeApi.writePoint(bucket, org, point);
            log.debug("Recorded heartbeat for warehouse {}: online={}", warehouseId, online);
        } catch (Exception e) {
            log.error("Failed to record heartbeat for warehouse {}", warehouseId, e);
        }
    }

    @Async("telemetryExecutor")
    public void recordTemperature(Long warehouseId, String warehouseName, Long sectorId, String sectorName, Double temperature) {
        try {
            WriteApiBlocking writeApi = influxDBClient.getWriteApiBlocking();
            Point point = Point.measurement("warehouse_temperature")
                    .addTag("warehouseId", String.valueOf(warehouseId))
                    .addTag("warehouseName", warehouseName)
                    .addTag("sectorId", String.valueOf(sectorId))
                    .addTag("sectorName", sectorName)
                    .addField("temperature", temperature)
                    .time(Instant.now(), WritePrecision.MS);
            writeApi.writePoint(bucket, org, point);
            log.debug("Recorded temperature for warehouse {} sector {}: temperature={}", warehouseId, sectorName, temperature);
        } catch (Exception e) {
            log.error("Failed to record temperature for warehouse {} sector {}", warehouseId, sectorName, e);
        }
    }

    @Cacheable(value = "temperatureStats", key = "#warehouseId + '-' + #sectorId + '-' + #startDate + '-' + #endDate")
    public TemperatureStatisticsDTO getAggregatedTemperature(Long warehouseId, String warehouseName,
                                                              Long sectorId, String sectorName,
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

        List<TemperatureStatisticsDTO.TemperatureDataPoint> dataPoints = new ArrayList<>();
        double totalTemperature = 0.0;
        double minTemp = Double.MAX_VALUE;
        double maxTemp = Double.MIN_VALUE;
        int dataPointCount = 0;

        try {
            String startTimeStr = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toString();
            String endTimeStr = endDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toString();

            String flux = String.format(
                    "from(bucket: \"%s\") " +
                            "|> range(start: %s, stop: %s) " +
                            "|> filter(fn: (r) => r._measurement == \"warehouse_temperature\") " +
                            "|> filter(fn: (r) => r.warehouseId == \"%d\") " +
                            "|> filter(fn: (r) => r.sectorId == \"%d\") " +
                            "|> filter(fn: (r) => r._field == \"temperature\") " +
                            "|> aggregateWindow(every: %s, fn: mean, createEmpty: false) " +
                            "|> sort(columns: [\"_time\"], desc: false) " +
                            "|> limit(n: %d)",
                    bucket, startTimeStr, endTimeStr, warehouseId, sectorId, windowPeriod, MAX_DATA_POINTS
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
                        Double avgTemperature = ((Number) record.getValue()).doubleValue();

                        totalTemperature += avgTemperature;
                        minTemp = Math.min(minTemp, avgTemperature);
                        maxTemp = Math.max(maxTemp, avgTemperature);
                        dataPointCount++;

                        LocalDate pointEndDate = switch (aggregationType) {
                            case "daily" -> pointDate;
                            case "weekly" -> pointDate.plusDays(6);
                            default -> pointDate.plusMonths(1).minusDays(1);
                        };

                        String label = switch (aggregationType) {
                            case "weekly" -> pointDate.format(labelFormatter) + " - " + pointEndDate.format(labelFormatter);
                            default -> pointDate.format(labelFormatter);
                        };

                        dataPoints.add(TemperatureStatisticsDTO.TemperatureDataPoint.builder()
                                .label(label)
                                .startDate(pointDate)
                                .endDate(pointEndDate)
                                .avgTemperature(avgTemperature)
                                .minTemperature(avgTemperature)
                                .maxTemperature(avgTemperature)
                                .build());
                    }
                }
            }

            String minMaxFlux = String.format(
                    "minData = from(bucket: \"%s\") " +
                            "|> range(start: %s, stop: %s) " +
                            "|> filter(fn: (r) => r._measurement == \"warehouse_temperature\") " +
                            "|> filter(fn: (r) => r.warehouseId == \"%d\") " +
                            "|> filter(fn: (r) => r.sectorId == \"%d\") " +
                            "|> filter(fn: (r) => r._field == \"temperature\") " +
                            "|> aggregateWindow(every: %s, fn: min, createEmpty: false) " +
                            "|> yield(name: \"min\")\n" +
                            "maxData = from(bucket: \"%s\") " +
                            "|> range(start: %s, stop: %s) " +
                            "|> filter(fn: (r) => r._measurement == \"warehouse_temperature\") " +
                            "|> filter(fn: (r) => r.warehouseId == \"%d\") " +
                            "|> filter(fn: (r) => r.sectorId == \"%d\") " +
                            "|> filter(fn: (r) => r._field == \"temperature\") " +
                            "|> aggregateWindow(every: %s, fn: max, createEmpty: false) " +
                            "|> yield(name: \"max\")",
                    bucket, startTimeStr, endTimeStr, warehouseId, sectorId, windowPeriod,
                    bucket, startTimeStr, endTimeStr, warehouseId, sectorId, windowPeriod
            );

        } catch (Exception e) {
            log.error("Failed to query aggregated temperature for warehouse {} sector {}", warehouseId, sectorId, e);
        }

        double avgTemp = dataPointCount > 0 ? totalTemperature / dataPointCount : 0.0;
        if (minTemp == Double.MAX_VALUE) minTemp = 0.0;
        if (maxTemp == Double.MIN_VALUE) maxTemp = 0.0;

        return TemperatureStatisticsDTO.builder()
                .warehouseId(warehouseId)
                .warehouseName(warehouseName)
                .sectorId(sectorId)
                .sectorName(sectorName)
                .startDate(startDate)
                .endDate(endDate)
                .aggregationType(aggregationType)
                .averageTemperature(Math.round(avgTemp * 100.0) / 100.0)
                .minTemperature(Math.round(minTemp * 100.0) / 100.0)
                .maxTemperature(Math.round(maxTemp * 100.0) / 100.0)
                .dataPoints(dataPoints)
                .build();
    }
}
