package client;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class SkierDataGenerator {
    private final static ThreadLocalRandom random = ThreadLocalRandom.current();

    //1. skierID - between 1 and 100000
    //2. resortID - between 1 and 10
    //3. liftID - between 1 and 40
    //4. seasonID - 2024
    //5. dayID - 1
    //6. time - between 1 and 360
    public static Map<String, Object> getSkyierMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("skierID", random.nextInt(1, 100001));
        map.put("resortID", random.nextInt(1, 11));
        map.put("liftID", random.nextInt(1, 41));
        map.put("seasonID", 2024);
        map.put("dayID", 1);
        map.put("time", random.nextInt(1, 361));
        return map;
    }

}
