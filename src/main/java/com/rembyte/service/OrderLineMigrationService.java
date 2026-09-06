package com.rembyte.service;

import com.rembyte.model.Order;
import com.rembyte.model.OrderLine;
import com.rembyte.repository.OrderLineRepository;
import com.rembyte.repository.OrderRepository;
import com.rembyte.repository.RepairServiceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Разовая миграция состава заказов из старой связи many-to-many
 * ({@code order_services}) в строки заказа ({@code order_lines}).
 * <p>
 * Проверка наличия старой таблицы и чтение из неё идут «сырым» JDBC вне
 * JPA-транзакции — иначе отсутствие таблицы на свежей БД помечало бы транзакцию
 * запуска как rollback-only. Запись строк выполняется в отдельной транзакции.
 */
@Service
public class OrderLineMigrationService {

    private static final Logger log = LoggerFactory.getLogger(OrderLineMigrationService.class);

    private final DataSource dataSource;
    private final TransactionTemplate transactionTemplate;
    private final OrderRepository orderRepository;
    private final OrderLineRepository orderLineRepository;
    private final RepairServiceRepository repairServiceRepository;

    @Value("${fixbyte.orderlines.migration.enabled:true}")
    private boolean migrationEnabled;

    public OrderLineMigrationService(DataSource dataSource,
                                     PlatformTransactionManager transactionManager,
                                     OrderRepository orderRepository,
                                     OrderLineRepository orderLineRepository,
                                     RepairServiceRepository repairServiceRepository) {
        this.dataSource = dataSource;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.orderRepository = orderRepository;
        this.orderLineRepository = orderLineRepository;
        this.repairServiceRepository = repairServiceRepository;
    }

    public void migrateOnStartup() {
        if (!migrationEnabled) {
            log.info("OrderLine migration skipped: fixbyte.orderlines.migration.enabled=false");
            return;
        }

        if (!legacyTableExists()) {
            log.info("OrderLine migration skipped: legacy table order_services not found");
            return;
        }

        Map<Long, List<Object[]>> byOrder;
        try {
            byOrder = readLegacyRows();
        } catch (Exception e) {
            log.warn("OrderLine migration skipped: cannot read order_services ({})", e.getMessage());
            return;
        }

        if (byOrder.isEmpty()) {
            log.info("OrderLine migration: legacy table order_services is empty, nothing to do");
            return;
        }

        int[] stats = {0, 0, 0}; // migratedOrders, createdLines, skippedOrders
        transactionTemplate.executeWithoutResult(status -> migrateAll(byOrder, stats));

        log.info("OrderLine migration completed: migratedOrders={}, createdLines={}, skippedOrders={}",
                stats[0], stats[1], stats[2]);
    }

    private boolean legacyTableExists() {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData meta = connection.getMetaData();
            for (String name : new String[]{"order_services", "ORDER_SERVICES"}) {
                try (ResultSet rs = meta.getTables(connection.getCatalog(), null, name, new String[]{"TABLE"})) {
                    if (rs.next()) return true;
                }
            }
        } catch (Exception e) {
            log.warn("OrderLine migration: table existence check failed ({})", e.getMessage());
        }
        return false;
    }

    private Map<Long, List<Object[]>> readLegacyRows() throws Exception {
        Map<Long, List<Object[]>> byOrder = new LinkedHashMap<>();
        String sql = "SELECT os.order_id, os.service_id, s.name, s.base_price " +
                     "FROM order_services os JOIN services s ON s.id = os.service_id " +
                     "ORDER BY os.order_id, os.service_id";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                long orderId = rs.getLong(1);
                Long serviceId = rs.getObject(2) != null ? rs.getLong(2) : null;
                String name = rs.getString(3);
                double basePrice = rs.getDouble(4);
                byOrder.computeIfAbsent(orderId, k -> new ArrayList<>())
                        .add(new Object[]{serviceId, name, basePrice});
            }
        }
        return byOrder;
    }

    private void migrateAll(Map<Long, List<Object[]>> byOrder, int[] stats) {
        for (Map.Entry<Long, List<Object[]>> entry : byOrder.entrySet()) {
            Long orderId = entry.getKey();

            if (orderLineRepository.countByOrder_Id(orderId) > 0) {
                stats[2]++;
                continue;
            }

            Order order = orderRepository.findById(orderId).orElse(null);
            if (order == null) {
                log.warn("OrderLine migration: order {} not found, skipped", orderId);
                stats[2]++;
                continue;
            }

            List<OrderLine> lines = new ArrayList<>();
            int sortOrder = 0;
            for (Object[] cols : entry.getValue()) {
                Long serviceId = (Long) cols[0];
                String name = cols[1] != null ? String.valueOf(cols[1]) : "Позиция";
                double basePrice = cols[2] != null ? ((Number) cols[2]).doubleValue() : 0.0;

                OrderLine line = new OrderLine();
                line.setService(serviceId != null
                        ? repairServiceRepository.findById(serviceId).orElse(null)
                        : null);
                line.setName(name);
                line.setUnitPrice(basePrice);
                line.setQuantity(1);
                line.setSortOrder(sortOrder++);
                lines.add(line);
            }

            order.setLines(lines);
            orderRepository.save(order);
            stats[0]++;
            stats[1] += lines.size();
        }
    }
}
