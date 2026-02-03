package nvt.backend.services.factory;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.QueryApi;
import com.influxdb.client.WriteApiBlocking;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import nvt.backend.dto.factory.FactoryAvailabilityStatisticsDTO;
import nvt.backend.dto.factory.FactoryProductionStatisticsDTO;
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
public class FactoryTelemetryService {

    private static final Logger log = LoggerFactory.getLogger(FactoryTelemetryService.class);
    private static final int MAX_DATA_POINTS = 100;

    private final InfluxDBClient influxDBClient;
    private final QueryApi queryApi;
    private final String org;
    private final String bucket;

    public FactoryTelemetryService(
            InfluxDBClient influxDBClient,
            @Value("${influxdb.org}") String org,
            @Value("${influxdb.bucket.factory:factory_telemetry}") String bucket) {
        this.influxDBClient = influxDBClient;
        this.queryApi = influxDBClient.getQueryApi();
        this.org = org;
        this.bucket = bucket;
    }

    @Async("telemetryExecutor")
    public void recordHeartbeat(Long factoryId, String factoryName, boolean online) {
        try {
            WriteApiBlocking writeApi = influxDBClient.getWriteApiBlocking();
            Point point = Point.measurement("factory_availability")
                    .addTag("factoryId", String.valueOf(factoryId))
                    .addTag("factoryName", factoryName)
                    .addField("online", online ? 1 : 0)
                    .time(Instant.now(), WritePrecision.MS);
            writeApi.writePoint(bucket, org, point);
            log.debug("Recorded heartbeat for factory {}: online={}", factoryId, online);
        } catch (Exception e) {
            log.error("Failed to record heartbeat for factory {}", factoryId, e);
        }
    }

    @Async("telemetryExecutor")
    public void recordProduction(Long factoryId, String factoryName, Long productId, 
                                  String productName, Integer quantity, String reportType) {
        try {
            WriteApiBlocking writeApi = influxDBClient.getWriteApiBlocking();
            Point point = Point.measurement("factory_production")
                    .addTag("factoryId", String.valueOf(factoryId))
                    .addTag("factoryName", factoryName)
                    .addTag("productId", String.valueOf(productId))
                    .addTag("productName", productName)
                    .addTag("reportType", reportType)
                    .addField("quantity", quantity)
                    .time(Instant.now(), WritePrecision.MS);
            writeApi.writePoint(bucket, org, point);
            log.debug("Recorded production for factory {} product {}: quantity={}", factoryId, productName, quantity);
        } catch (Exception e) {
            log.error("Failed to record production for factory {} product {}", factoryId, productName, e);
        }
    }

    @Cacheable(value = "factoryProductionStats", key = "#factoryId + '-' + #productId + '-' + #startDate + '-' + #endDate")
    public FactoryProductionStatisticsDTO getAggregatedProduction(Long factoryId, String factoryName,
                                                                   Long productId, String productName,
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

        List<FactoryProductionStatisticsDTO.ProductionDataPoint> dataPoints = new ArrayList<>();
        long totalQuantity = 0;
        int minQuantity = Integer.MAX_VALUE;
        int maxQuantity = Integer.MIN_VALUE;
        int dataPointCount = 0;

        try {
            String startTimeStr = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toString();
            String endTimeStr = endDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toString();

            String flux = String.format(
                    "from(bucket: \"%s\") " +
                            "|> range(start: %s, stop: %s) " +
                            "|> filter(fn: (r) => r._measurement == \"factory_production\") " +
                            "|> filter(fn: (r) => r.factoryId == \"%d\") " +
                            "|> filter(fn: (r) => r.productId == \"%d\") " +
                            "|> filter(fn: (r) => r._field == \"quantity\") " +
                            "|> aggregateWindow(every: %s, fn: sum, createEmpty: false) " +
                            "|> sort(columns: [\"_time\"], desc: false) " +
                            "|> limit(n: %d)",
                    bucket, startTimeStr, endTimeStr, factoryId, productId, windowPeriod, MAX_DATA_POINTS
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
                        Integer sumQuantity = ((Number) record.getValue()).intValue();

                        totalQuantity += sumQuantity;
                        minQuantity = Math.min(minQuantity, sumQuantity);
                        maxQuantity = Math.max(maxQuantity, sumQuantity);
                        dataPointCount++;

                        LocalDate pointEndDate = switch (aggregationType) {
                            case "daily" -> pointDate;
                            case "weekly" -> pointDate.plusDays(6);
                            default -> pointDate.plusMonths(1).minusDays(1);
                        };

                        String label = labelFormatter.format(pointDate);
                        if (!aggregationType.equals("daily")) {
                            label += " - " + labelFormatter.format(pointEndDate);
                        }

                        dataPoints.add(FactoryProductionStatisticsDTO.ProductionDataPoint.builder()
                                .date(pointDate)
                                .endDate(pointEndDate)
                                .label(label)
                                .quantity(sumQuantity)
                                .build());
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to get aggregated production for factory {} product {}", factoryId, productId, e);
        }

        double avgQuantity = dataPointCount > 0 ? (double) totalQuantity / dataPointCount : 0.0;

        return FactoryProductionStatisticsDTO.builder()
                .factoryId(factoryId)
                .factoryName(factoryName)
                .productId(productId)
                .productName(productName)
                .startDate(startDate)
                .endDate(endDate)
                .aggregationType(aggregationType)
                .dataPoints(dataPoints)
                .totalQuantity(totalQuantity)
                .averageQuantity(Math.round(avgQuantity * 100.0) / 100.0)
                .minQuantity(minQuantity == Integer.MAX_VALUE ? 0 : minQuantity)
                .maxQuantity(maxQuantity == Integer.MIN_VALUE ? 0 : maxQuantity)
                .dataPointCount(dataPointCount)
                .build();
    }

    @Cacheable(value = "factoryAvailabilityStats", key = "#factoryId + '-' + #startDate + '-' + #endDate")
    public FactoryAvailabilityStatisticsDTO getAvailabilityStatistics(Long factoryId, String factoryName,
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

        List<FactoryAvailabilityStatisticsDTO.AvailabilityDataPoint> dataPoints = new ArrayList<>();
        double totalUptime = 0.0;
        int dataPointCount = 0;

        try {
            String startTimeStr = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toString();
            String endTimeStr = endDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toString();

            String flux = String.format(
                    "from(bucket: \"%s\") " +
                            "|> range(start: %s, stop: %s) " +
                            "|> filter(fn: (r) => r._measurement == \"factory_availability\") " +
                            "|> filter(fn: (r) => r.factoryId == \"%d\") " +
                            "|> filter(fn: (r) => r._field == \"online\") " +
                            "|> aggregateWindow(every: %s, fn: mean, createEmpty: false) " +
                            "|> sort(columns: [\"_time\"], desc: false) " +
                            "|> limit(n: %d)",
                    bucket, startTimeStr, endTimeStr, factoryId, windowPeriod, MAX_DATA_POINTS
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
                        Double uptimePercentage = ((Number) record.getValue()).doubleValue() * 100;

                        totalUptime += uptimePercentage;
                        dataPointCount++;

                        LocalDate pointEndDate = switch (aggregationType) {
                            case "daily" -> pointDate;
                            case "weekly" -> pointDate.plusDays(6);
                            default -> pointDate.plusMonths(1).minusDays(1);
                        };

                        String label = labelFormatter.format(pointDate);
                        if (!aggregationType.equals("daily")) {
                            label += " - " + labelFormatter.format(pointEndDate);
                        }

                        dataPoints.add(FactoryAvailabilityStatisticsDTO.AvailabilityDataPoint.builder()
                                .date(pointDate)
                                .endDate(pointEndDate)
                                .label(label)
                                .uptimePercentage(Math.round(uptimePercentage * 100.0) / 100.0)
                                .build());
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to get availability statistics for factory {}", factoryId, e);
        }

        double avgUptime = dataPointCount > 0 ? totalUptime / dataPointCount : 0.0;

        return FactoryAvailabilityStatisticsDTO.builder()
                .factoryId(factoryId)
                .factoryName(factoryName)
                .startDate(startDate)
                .endDate(endDate)
                .aggregationType(aggregationType)
                .dataPoints(dataPoints)
                .averageUptimePercentage(Math.round(avgUptime * 100.0) / 100.0)
                .dataPointCount(dataPointCount)
                .build();
    }
}
