package nvt.backend.services.vehicle;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.QueryApi;
import com.influxdb.client.WriteApiBlocking;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class VehicleTelemetryService {

    private static final Logger log = LoggerFactory.getLogger(VehicleTelemetryService.class);

    private final InfluxDBClient influxDBClient;
    private final String org;
    private final String bucket;

    public VehicleTelemetryService(
            InfluxDBClient influxDBClient,
            @Value("${influxdb.org}") String org,
            @Value("${influxdb.bucket}") String bucket) {
        this.influxDBClient = influxDBClient;
        this.org = org;
        this.bucket = bucket;
    }

    public void recordHeartbeat(Long vehicleId, String licensePlate, boolean online) {
        try {
            WriteApiBlocking writeApi = influxDBClient.getWriteApiBlocking();
            Point point = Point.measurement("vehicle_availability")
                    .addTag("vehicleId", String.valueOf(vehicleId))
                    .addTag("licensePlate", licensePlate)
                    .addField("online", online ? 1 : 0)
                    .time(Instant.now(), WritePrecision.MS);
            writeApi.writePoint(point);
            log.debug("Recorded heartbeat for vehicle {}: online={}", vehicleId, online);
        } catch (Exception e) {
            log.error("Failed to record heartbeat for vehicle {}", vehicleId, e);
        }
    }

    public void recordStateChange(Long vehicleId, String licensePlate, String state) {
        try {
            WriteApiBlocking writeApi = influxDBClient.getWriteApiBlocking();
            Point point = Point.measurement("vehicle_state")
                    .addTag("vehicleId", String.valueOf(vehicleId))
                    .addTag("licensePlate", licensePlate)
                    .addField("state", state)
                    .time(Instant.now(), WritePrecision.MS);
            writeApi.writePoint(point);
            log.debug("Recorded state change for vehicle {}: state={}", vehicleId, state);
        } catch (Exception e) {
            log.error("Failed to record state change for vehicle {}", vehicleId, e);
        }
    }

    public void recordDistance(Long vehicleId, String licensePlate, Double distance) {
        try {
            WriteApiBlocking writeApi = influxDBClient.getWriteApiBlocking();
            Point point = Point.measurement("vehicle_distance")
                    .addTag("vehicleId", String.valueOf(vehicleId))
                    .addTag("licensePlate", licensePlate)
                    .addField("distance", distance)
                    .time(Instant.now(), WritePrecision.MS);
            writeApi.writePoint(point);
            log.debug("Recorded distance for vehicle {}: distance={}", vehicleId, distance);
        } catch (Exception e) {
            log.error("Failed to record distance for vehicle {}", vehicleId, e);
        }
    }

    public void recordLocation(Long vehicleId, String licensePlate, Double latitude, Double longitude) {
        try {
            WriteApiBlocking writeApi = influxDBClient.getWriteApiBlocking();
            Point point = Point.measurement("vehicle_location")
                    .addTag("vehicleId", String.valueOf(vehicleId))
                    .addTag("licensePlate", licensePlate)
                    .addField("latitude", latitude)
                    .addField("longitude", longitude)
                    .time(Instant.now(), WritePrecision.MS);
            writeApi.writePoint(point);
            log.debug("Recorded location for vehicle {}: lat={}, lon={}", vehicleId, latitude, longitude);
        } catch (Exception e) {
            log.error("Failed to record location for vehicle {}", vehicleId, e);
        }
    }

    public List<Map<String, Object>> getAvailabilityHistory(Long vehicleId, String range) {
        List<Map<String, Object>> results = new ArrayList<>();
        try {
            String flux = String.format(
                    "from(bucket: \"%s\") " +
                            "|> range(start: %s) " +
                            "|> filter(fn: (r) => r._measurement == \"vehicle_availability\") " +
                            "|> filter(fn: (r) => r.vehicleId == \"%d\") " +
                            "|> sort(columns: [\"_time\"], desc: true)",
                    bucket, range, vehicleId
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
                            "|> filter(fn: (r) => r._measurement == \"vehicle_distance\") " +
                            "|> filter(fn: (r) => r.vehicleId == \"%d\") " +
                            "|> sort(columns: [\"_time\"], desc: true)",
                    bucket, range, vehicleId
            );

            QueryApi queryApi = influxDBClient.getQueryApi();
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

    public List<Map<String, Object>> getStateHistory(Long vehicleId, String range) {
        List<Map<String, Object>> results = new ArrayList<>();
        try {
            String flux = String.format(
                    "from(bucket: \"%s\") " +
                            "|> range(start: %s) " +
                            "|> filter(fn: (r) => r._measurement == \"vehicle_state\") " +
                            "|> filter(fn: (r) => r.vehicleId == \"%d\") " +
                            "|> sort(columns: [\"_time\"], desc: true)",
                    bucket, range, vehicleId
            );

            QueryApi queryApi = influxDBClient.getQueryApi();
            List<FluxTable> tables = queryApi.query(flux, org);

            for (FluxTable table : tables) {
                for (FluxRecord record : table.getRecords()) {
                    Map<String, Object> entry = new HashMap<>();
                    entry.put("time", record.getTime());
                    entry.put("state", record.getValue());
                    results.add(entry);
                }
            }
        } catch (Exception e) {
            log.error("Failed to query state history for vehicle {}", vehicleId, e);
        }
        return results;
    }
}