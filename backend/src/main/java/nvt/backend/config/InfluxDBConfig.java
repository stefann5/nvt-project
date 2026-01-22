package nvt.backend.config;

import com.influxdb.client.BucketsApi;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import com.influxdb.client.OrganizationsApi;
import com.influxdb.client.domain.Bucket;
import com.influxdb.client.domain.Organization;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class InfluxDBConfig {

    private static final Logger log = LoggerFactory.getLogger(InfluxDBConfig.class);

    @Value("${influxdb.url}")
    private String url;

    @Value("${influxdb.token}")
    private String token;

    @Value("${influxdb.org}")
    private String org;

    @Value("${influxdb.bucket}")
    private String bucket;

    @Value("${influxdb.bucket.warehouse}")
    private String warehouseBucket;

    @Bean
    public InfluxDBClient influxDBClient() {
        InfluxDBClient client = InfluxDBClientFactory.create(url, token.toCharArray(), org, bucket);
        ensureBucketsExist(client);
        return client;
    }

    private void ensureBucketsExist(InfluxDBClient client) {
        try {
            BucketsApi bucketsApi = client.getBucketsApi();
            OrganizationsApi orgsApi = client.getOrganizationsApi();

            List<Organization> organizations = orgsApi.findOrganizations();
            Organization organization = organizations.stream()
                    .filter(o -> o.getName().equals(org))
                    .findFirst()
                    .orElse(null);

            if (organization == null) {
                log.warn("Organization '{}' not found in InfluxDB", org);
                return;
            }

            // Ensure vehicle_telemetry bucket exists
            createBucketIfNotExists(bucketsApi, organization, bucket);

            // Ensure warehouse_telemetry bucket exists
            createBucketIfNotExists(bucketsApi, organization, warehouseBucket);

        } catch (Exception e) {
            log.error("Failed to ensure InfluxDB buckets exist: {}", e.getMessage());
        }
    }

    private void createBucketIfNotExists(BucketsApi bucketsApi, Organization organization, String bucketName) {
        Bucket existingBucket = bucketsApi.findBucketByName(bucketName);
        if (existingBucket == null) {
            log.info("Creating InfluxDB bucket: {}", bucketName);
            bucketsApi.createBucket(bucketName, organization);
            log.info("Successfully created InfluxDB bucket: {}", bucketName);
        } else {
            log.debug("InfluxDB bucket '{}' already exists", bucketName);
        }
    }
}
