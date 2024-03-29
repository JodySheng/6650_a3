package Servlet.Controller;

import Servlet.RabbitMQConnectionPool;
import Servlet.ResponseMsg;
import com.google.gson.Gson;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import io.swagger.client.model.LiftRide;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Objects;

@RestController
@RequestMapping("/skiers")
public class SkierController {

    private final static String queueName = "skierQueue";

    // springMVC自动检测路径
    @PostMapping("/{resortID}/seasons/{seasonID}/days/{dayID}/skiers/{skierID}")
    public void doPost(HttpServletResponse res,
                       @RequestBody LiftRide body,
                       @PathVariable Integer resortID,
                       @PathVariable Integer seasonID,
                       @PathVariable Integer dayID,
                       @PathVariable Integer skierID) throws IOException {
        res.setContentType("application/json");
        ResponseMsg rmsg = new ResponseMsg();
        Gson gson = new Gson();
        if (!isUrlValid(dayID)) {
            res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            rmsg.setMessage("Invalid inputs");
            res.getWriter().write(gson.toJson(rmsg));
        } else {
            res.setStatus(HttpServletResponse.SC_CREATED);
            Consumer.model.LiftRide ski = getSkier(body, resortID, seasonID, dayID, skierID);
            // send to rabbitmq
            try {
                Connection connection = RabbitMQConnectionPool.getConnection();
                Channel channel = connection.createChannel();
                channel.queueDeclare(queueName, false, false, false, null);
                channel.basicPublish("", queueName, null, gson.toJson(ski).getBytes());
                System.out.println("Sent '" + ski + "'");
                channel.close();
                RabbitMQConnectionPool.releaseConnection(connection);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            rmsg.setMessage("Write successful");
            res.getWriter().write(gson.toJson(rmsg));
        }
    }

    private Consumer.model.LiftRide getSkier(LiftRide body, Integer resortID, Integer seasonID, Integer dayID, Integer skierId) {
        //liftRideId 是主键 含义：升降机使用记录主键，也就是每一次使用升降机记录一次，设置为自增主键即可
        Consumer.model.LiftRide ski = new Consumer.model.LiftRide();
        ski.setTime(body.getTime());
        ski.setLiftId(body.getLiftID());
        ski.setResortId(resortID);
        ski.setSeasonId(seasonID);
        ski.setDayId(dayID);
        ski.setSkierId(skierId);
        return ski;
    }

    private boolean isUrlValid(Integer day) {
        return day >=  1 && day <= 366;
    }
}
