package com.stockpilot.connector;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.stream.Stream;

/**
 * Every few seconds: read each *.json file in inbox/, send it to the WMS,
 * then move it to processed/ (success) or failed/ (with a .error.txt explaining why).
 */
@Component
public class OrderSyncJob {

    private static final Logger log = LoggerFactory.getLogger(OrderSyncJob.class);

    private final WmsApiClient wms;
    private final ObjectMapper objectMapper;
    private final ConnectorProperties props;

    public OrderSyncJob(WmsApiClient wms, ObjectMapper objectMapper, ConnectorProperties props) {
        this.wms = wms;
        this.objectMapper = objectMapper;
        this.props = props;
    }

    @Scheduled(fixedDelayString = "${connector.poll-interval-ms:15000}", initialDelay = 3000)
    public void run() throws IOException {
        Path inbox = Path.of(props.inboxDir());
        Files.createDirectories(inbox);
        List<Path> files;
        try (Stream<Path> stream = Files.list(inbox)) {
            files = stream.filter(p -> p.toString().endsWith(".json")).sorted().toList();
        }
        if (!files.isEmpty()) {
            log.info("Found {} order file(s) in {}", files.size(), inbox.toAbsolutePath());
        }
        for (Path file : files) {
            process(file);
        }
    }

    private void process(Path file) throws IOException {
        WebStoreOrder order;
        try {
            order = objectMapper.readValue(file.toFile(), WebStoreOrder.class);
        } catch (IOException e) {
            fail(file, "Couldn't read the file as a web store order: " + e.getMessage());
            return;
        }

        try {
            JsonNode created = wms.createSalesOrder(WmsOrderRequest.from(order));
            String soNumber = created.path("soNumber").asText();
            log.info("Web order {} -> {}", order.orderId(), soNumber);
            move(file, Path.of(props.processedDir()));
        } catch (ResourceAccessException e) {
            // WMS is down or unreachable: leave the file in the inbox and try again next run.
            log.warn("Can't reach the WMS ({}). Will retry {}.", e.getMessage(), file.getFileName());
        } catch (HttpStatusCodeException e) {
            fail(file, "WMS rejected order " + order.orderId() + ": " + e.getStatusCode() + " "
                    + e.getResponseBodyAsString());
        }
    }

    private void fail(Path file, String reason) throws IOException {
        log.error("{}: {}", file.getFileName(), reason);
        Path failedDir = Path.of(props.failedDir());
        move(file, failedDir);
        Files.writeString(failedDir.resolve(file.getFileName() + ".error.txt"), reason);
    }

    private static void move(Path file, Path targetDir) throws IOException {
        Files.createDirectories(targetDir);
        Files.move(file, targetDir.resolve(file.getFileName()), StandardCopyOption.REPLACE_EXISTING);
    }
}
