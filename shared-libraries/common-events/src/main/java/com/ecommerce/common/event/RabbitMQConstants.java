package com.ecommerce.common.event;

/**
 * Constants for RabbitMQ exchanges, queues, and routing keys.
 * All services reference these constants to ensure consistent naming.
 */
public final class RabbitMQConstants {

    private RabbitMQConstants() {
        // Prevent instantiation
    }

    // ===== Exchanges =====
    public static final String ORDER_EXCHANGE = "order.exchange";
    public static final String PAYMENT_EXCHANGE = "payment.exchange";
    public static final String INVENTORY_EXCHANGE = "inventory.exchange";
    public static final String NOTIFICATION_EXCHANGE = "notification.exchange";
    public static final String USER_EXCHANGE = "user.exchange";
    public static final String DLX_EXCHANGE = "dlx.exchange";

    // ===== Routing Keys =====
    public static final String ORDER_CREATED_KEY = "order.created";
    public static final String ORDER_CANCELLED_KEY = "order.cancelled";
    public static final String PAYMENT_COMPLETED_KEY = "payment.completed";
    public static final String PAYMENT_FAILED_KEY = "payment.failed";
    public static final String INVENTORY_UPDATED_KEY = "inventory.updated";
    public static final String INVENTORY_RESERVED_KEY = "inventory.reserved";
    public static final String USER_REGISTERED_KEY = "user.registered";
    public static final String NOTIFICATION_SEND_KEY = "notification.send";

    // ===== Queues =====
    // Inventory Service queues
    public static final String INVENTORY_ORDER_QUEUE = "inventory.order.queue";
    public static final String INVENTORY_PAYMENT_QUEUE = "inventory.payment.queue";

    // Payment Service queues
    public static final String PAYMENT_ORDER_QUEUE = "payment.order.queue";

    // Order Service queues
    public static final String ORDER_PAYMENT_QUEUE = "order.payment.queue";

    // User Service queues
    public static final String USER_REGISTERED_QUEUE = "user.registered.queue";

    // Notification Service queues
    public static final String NOTIFICATION_ORDER_QUEUE = "notification.order.queue";
    public static final String NOTIFICATION_PAYMENT_QUEUE = "notification.payment.queue";
    public static final String NOTIFICATION_USER_QUEUE = "notification.user.queue";
    public static final String NOTIFICATION_INVENTORY_QUEUE = "notification.inventory.queue";
    public static final String NOTIFICATION_GENERAL_QUEUE = "notification.general.queue";

    // ===== Dead Letter Queues =====
    public static final String INVENTORY_ORDER_DLQ = "inventory.order.dlq";
    public static final String PAYMENT_ORDER_DLQ = "payment.order.dlq";
    public static final String ORDER_PAYMENT_DLQ = "order.payment.dlq";
    public static final String NOTIFICATION_DLQ = "notification.dlq";

    // ===== Headers =====
    public static final String RETRY_COUNT_HEADER = "x-retry-count";
    public static final int MAX_RETRY_COUNT = 3;
}
