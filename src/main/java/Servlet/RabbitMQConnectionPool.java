package Servlet;

import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

public class RabbitMQConnectionPool {
    private static final String rabbitmqUrl = "localhost";
    private static final int conectionPoolSize = 50;
    private static ObjectPool<Connection> connectionPool;

    static {
        ConnectionFactory connectionFactory = new ConnectionFactory();
        connectionFactory.setHost(rabbitmqUrl);

        GenericObjectPoolConfig<Connection> poolConfig = new GenericObjectPoolConfig<>();
        poolConfig.setMaxTotal(conectionPoolSize);
        connectionPool = new GenericObjectPool<>(new RabbitMQConnectionFactory(connectionFactory), poolConfig);
    }

    public static Connection getConnection() throws Exception {
        return connectionPool.borrowObject();
    }

    public static void releaseConnection(Connection connection) {
        try {
            connectionPool.returnObject(connection);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static class RabbitMQConnectionFactory extends org.apache.commons.pool2.BasePooledObjectFactory<Connection> {
        private final ConnectionFactory connectionFactory;

        public RabbitMQConnectionFactory(ConnectionFactory connectionFactory) {
            this.connectionFactory = connectionFactory;
        }

        @Override
        public Connection create() throws Exception {
            return connectionFactory.newConnection();
        }

        @Override
        public PooledObject<Connection> wrap(Connection connection) {
            return new DefaultPooledObject<>(connection);
        }

        @Override
        public void destroyObject(PooledObject<Connection> p) throws Exception {
            p.getObject().close();
        }

        @Override
        public boolean validateObject(PooledObject<Connection> p) {
            return p.getObject().isOpen();
        }
    }
}
