package Consumer;

import Consumer.mapper.LiftRideMapper;
import Consumer.model.LiftRide;
import com.google.gson.Gson;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;


@SpringBootApplication
@MapperScan("Consumer.mapper")
public class MsgConsumer implements CommandLineRunner {

    static ConnectionFactory factory = new ConnectionFactory();
    static String rabbitmqUrl = "localhost";
    static int connectionSize = 70;
    static String queueName = "skierQueue";

    public static ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(connectionSize);

    @Autowired
    private SqlSessionTemplate sqlSessionTemplate;

    public static void main(String[] args) {
        SpringApplication.run(MsgConsumer.class, args);
    }


    @Override
    public void run(String... args) throws Exception {
        factory.setHost(rabbitmqUrl);

        for (int i = 0; i < connectionSize; i++) {
            executor.execute(new ConsumerThread(sqlSessionTemplate.getMapper(LiftRideMapper.class)));
        }
    }
}

class ConsumerThread implements Runnable {

    private LiftRideMapper liftRideMapper;
    ConsumerThread(LiftRideMapper liftRideMapper) {
        this.liftRideMapper = liftRideMapper;
    }

    @Override
    public void run() {
        try {
            // 创建连接
            Connection connection = MsgConsumer.factory.newConnection();
            // 创建通道
            Channel channel = connection.createChannel();
            // 声明要消费的队列
            channel.queueDeclare(MsgConsumer.queueName, false, false, false, null);
            // 创建队列消费者
            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                String message = new String(delivery.getBody(), "UTF-8");
                this.liftRideMapper.insert(new Gson().fromJson(message, LiftRide.class));
                System.out.println(" [x] Received '" + message + "'");
            };

            channel.basicConsume(MsgConsumer.queueName, true, deliverCallback, consumerTag -> { });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
