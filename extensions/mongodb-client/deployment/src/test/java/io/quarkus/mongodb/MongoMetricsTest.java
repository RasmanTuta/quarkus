package io.quarkus.mongodb;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import javax.inject.Inject;

import org.eclipse.microprofile.metrics.Metric;
import org.eclipse.microprofile.metrics.MetricID;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.Tag;
import org.eclipse.microprofile.metrics.annotation.RegistryType;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.mongodb.client.MongoClient;

import io.quarkus.mongodb.metrics.ConnectionPoolGauge;
import io.quarkus.test.QuarkusUnitTest;

public class MongoMetricsTest extends MongoTestBase {

    @Inject
    MongoClient client;

    @Inject
    @RegistryType(type = MetricRegistry.Type.VENDOR)
    MetricRegistry registry;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class).addClasses(MongoTestBase.class))
            .withConfigurationResource("application-metrics-mongo.properties");

    @AfterEach
    void cleanup() {
        if (client != null) {
            client.close();
        }
    }

    @Test
    void testMetricsInitialization() {
        assertNull(getGaugeValueOrNull("mongodb.connection-pool.size", getTags()));
        assertNull(getGaugeValueOrNull("mongodb.connection-pool.checked-out-count", getTags()));

        // Just need to execute something so that an connection is opened
        String name = client.listDatabaseNames().first();

        assertEquals(1L, getGaugeValueOrNull("mongodb.connection-pool.size", getTags()));
        assertEquals(0L, getGaugeValueOrNull("mongodb.connection-pool.checked-out-count", getTags()));

        client.close();
        assertEquals(0L, getGaugeValueOrNull("mongodb.connection-pool.size", getTags()));
        assertEquals(0L, getGaugeValueOrNull("mongodb.connection-pool.checked-out-count", getTags()));
    }

    private Long getGaugeValueOrNull(String metricName, Tag[] tags) {
        MetricID metricID = new MetricID(metricName, tags);
        Metric metric = registry.getMetrics().get(metricID);

        if (metric == null) {
            return null;
        }
        return ((ConnectionPoolGauge) metric).getValue();
    }

    private Tag[] getTags() {
        return new Tag[] {
                new Tag("host", "localhost"),
                new Tag("port", "27018"),
        };
    }
}
