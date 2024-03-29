package client;

import io.swagger.client.ApiClient;
import io.swagger.client.ApiException;
import io.swagger.client.ApiResponse;
import io.swagger.client.api.SkiersApi;
import io.swagger.client.model.LiftRide;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class MutiThreadClient {
    /*
    if
    average Response Time == 50ms
    N == tomcat maxThreads == 200
    then
    threadNumCaculatedByLittleLaw = 200
    every thread send 20 request
     */
//    static String baseUrl = "http://54.185.212.77:8080/assignment2/";
    static String baseUrl = "http://52.13.13.167:8080";
    static int initialThreadNum = 32;

    static int initialPostNumOfPerThread = 1000;

    static int totalPostNum = 200000;

    static AtomicBoolean initFinished = new AtomicBoolean(false);

    static int   threadNumCaculatedByLittleLaw = 200;

    static AtomicInteger rest = new AtomicInteger(totalPostNum);


    public static ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(initialThreadNum);;

    public static boolean terminated = false;

    public static AtomicInteger totalSendSucceedNum = new AtomicInteger(0);

    public static AtomicInteger totalSendFailNum = new AtomicInteger(0);

    static CopyOnWriteArrayList<String[]> postRecords = new CopyOnWriteArrayList<>();

    public static void main(String[] args) throws IOException {
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < initialThreadNum; i++) {
            executor.execute(new InitSendPostThread());
        }
        while (rest.get() > 0) {
            System.out.println(rest.get());
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        terminated = true;
        executor.shutdown();
        while (executor.getActiveCount() > 0) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        long endTime = System.currentTimeMillis();
        long elapsedTime = endTime - startTime;
        System.out.println("number of successful requests sent:"  + totalSendSucceedNum);
        System.out.println("number of unsuccessful requests:"  + totalSendFailNum);
        System.out.println("the total run time:" + elapsedTime);
        System.out.println("the total throughput:" + totalPostNum * 1000L / elapsedTime );
        System.out.println("-----------------------------------------");
        writeCSV(postRecords, "postRecords.csv");
        List<Integer> responseTimeList = postRecords.stream()
                .map(e -> Integer.parseInt(e[1])).sorted(Comparator.naturalOrder()).collect(Collectors.toList());
        int totalResponseTime = responseTimeList.stream().reduce(0, Integer::sum);
        System.out.println("mean response time:" + totalResponseTime / totalPostNum);
        int median = 0;
        int responseTimeListSize = responseTimeList.size();
        if (responseTimeListSize % 2 == 0) {
            median = (responseTimeList.get(responseTimeListSize / 2 - 1) + responseTimeList.get(responseTimeListSize / 2)) / 2;
        } else {
            median = responseTimeList.get(responseTimeListSize / 2);
        }
        System.out.println("median response time:" + median);
        System.out.println("throughput:" + totalPostNum * 1000L / elapsedTime);
        System.out.println("p99 response time:" + responseTimeList.get((int)(responseTimeListSize * 0.99)));
        System.out.println("min response time:" + Collections.min(responseTimeList));
        System.out.println("max response time:" + Collections.max(responseTimeList));
    }

    public static void writeCSV(List<String[]> data, String filePath) throws IOException {
        FileWriter writer = new FileWriter(filePath);
        for (String[] row : data) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < row.length; i++) {
                sb.append(row[i]);
                if (i < row.length - 1) {
                    sb.append(",");
                }
            }
            sb.append("\n");
            writer.append(sb.toString());
        }
        writer.flush();
        writer.close();
    }
}

class InitSendPostThread implements Runnable {

    private int hasSendSucceedPostNum = 0;

    private int hasSendFailPostNum = 0;


    private SkiersApi apiInstance = new SkiersApi();

    InitSendPostThread() {
        ApiClient apiClient = new ApiClient();
        apiClient.setBasePath(MutiThreadClient.baseUrl);
        apiInstance.setApiClient(apiClient);
    }


    private boolean sendPostRequest() throws ApiException {
        Map<String, Object> skierMap = SkierDataGenerator.getSkyierMap();
        LiftRide body = new LiftRide();
        body.setLiftID((Integer) skierMap.get("liftID"));
        body.setTime((Integer) skierMap.get("time"));
        Integer resortID = (Integer)skierMap.get("resortID");
        String seasonID = skierMap.get("seasonID").toString();
        String dayID = skierMap.get("dayID").toString();
        Integer skierID = (Integer)skierMap.get("skierID");
        long startTime = System.currentTimeMillis();
        ApiResponse<Void> res = apiInstance.writeNewLiftRideWithHttpInfo(body, resortID, seasonID, dayID, skierID);
        long endTime = System.currentTimeMillis();
        long elapsedTime = endTime - startTime;
        MutiThreadClient.postRecords.add(new String[]{new Date(startTime).toString(), String.valueOf(elapsedTime), String.valueOf(res.getStatusCode())});
        return String.valueOf(res.getStatusCode()).charAt(0) != '5' && String.valueOf(res.getStatusCode()).charAt(0) != '4';
    }
    @Override
    public void run() {
        while (hasSendSucceedPostNum + hasSendFailPostNum < MutiThreadClient.initialPostNumOfPerThread) {
            int val = MutiThreadClient.rest.get();
            if (val > 0) {
                if (MutiThreadClient.rest.compareAndSet(val, val - 1)) {
                    boolean isSuccessful = false;
                    try {
                        isSuccessful = sendPostRequest();
                    } catch (Exception ex) {
                        System.out.println(ex);
                    }
                    if (!isSuccessful) {
                        int retryCount = 5;
                        while (retryCount > 0) {
                            try {
                                isSuccessful = sendPostRequest();
                            } catch (Exception ex) {
                                System.out.println(ex);
                            }
                            if (isSuccessful) break;
                            retryCount -= 1;
                        }
                    }
                    if (!isSuccessful) {
                        hasSendFailPostNum += 1;
                    } else {
                        hasSendSucceedPostNum += 1;
                    }
                }

            }
//            try {
//                Thread.sleep(50);
//            } catch (InterruptedException e) {
//                throw new RuntimeException(e);
//            }
        }
        boolean initFinishedVal = MutiThreadClient.initFinished.get();
        if (!initFinishedVal) {
            if (MutiThreadClient.initFinished.compareAndSet(initFinishedVal, true)) {
                MutiThreadClient.executor.setMaximumPoolSize(MutiThreadClient.threadNumCaculatedByLittleLaw);
                MutiThreadClient.executor.setCorePoolSize(MutiThreadClient.threadNumCaculatedByLittleLaw);
                for (int i = 0; i < MutiThreadClient.threadNumCaculatedByLittleLaw; i++) {
                    MutiThreadClient.executor.execute(new NormalSendPostThread());
                }
            }
        }

        MutiThreadClient.totalSendSucceedNum.addAndGet(hasSendSucceedPostNum);
        MutiThreadClient.totalSendFailNum.addAndGet(hasSendFailPostNum);
    }
}


class NormalSendPostThread implements Runnable {

    private int hasSendSucceedPostNum = 0;

    private int hasSendFailPostNum = 0;


    private SkiersApi apiInstance = new SkiersApi();

    NormalSendPostThread() {
        ApiClient apiClient = new ApiClient();
        apiClient.setBasePath(MutiThreadClient.baseUrl);
        apiInstance.setApiClient(apiClient);
    }


    private boolean sendPostRequest() throws ApiException {
        Map<String, Object> skierMap = SkierDataGenerator.getSkyierMap();
        LiftRide body = new LiftRide();
        body.setLiftID((Integer) skierMap.get("liftID"));
        body.setTime((Integer) skierMap.get("time"));
        Integer resortID = (Integer)skierMap.get("resortID");
        String seasonID = skierMap.get("seasonID").toString();
        String dayID = skierMap.get("dayID").toString();
        Integer skierID = (Integer)skierMap.get("skierID");
        long startTime = System.currentTimeMillis();
        ApiResponse<Void> res = apiInstance.writeNewLiftRideWithHttpInfo(body, resortID, seasonID, dayID, skierID);
        long endTime = System.currentTimeMillis();
        long elapsedTime = endTime - startTime;
        MutiThreadClient.postRecords.add(new String[]{new Date(startTime).toString(), String.valueOf(elapsedTime), String.valueOf(res.getStatusCode())});
        return String.valueOf(res.getStatusCode()).charAt(0) != '5' && String.valueOf(res.getStatusCode()).charAt(0) != '4';
    }
    @Override
    public void run() {
        while (!MutiThreadClient.terminated) {
            int val = MutiThreadClient.rest.get();
            if (val > 0) {
                if (MutiThreadClient.rest.compareAndSet(val, val - 1)) {
                    boolean isSuccessful = false;
                    try {
                        isSuccessful = sendPostRequest();
                    } catch (Exception ex) {
                        System.out.println(ex);
                    }
                    if (!isSuccessful) {
                        int retryCount = 5;
                        while (retryCount > 0) {
                            try {
                                isSuccessful = sendPostRequest();
                            } catch (Exception ex) {
                                System.out.println(ex);
                            }
                            if (isSuccessful) break;
                            retryCount -= 1;
                        }
                    }
                    if (!isSuccessful) {
                        hasSendFailPostNum += 1;
                    } else {
                        hasSendSucceedPostNum += 1;
                    }
                }

            }
//            try {
//                Thread.sleep(50);
//            } catch (InterruptedException e) {
//                throw new RuntimeException(e);
//            }
        }
        MutiThreadClient.totalSendSucceedNum.addAndGet(hasSendSucceedPostNum);
        MutiThreadClient.totalSendFailNum.addAndGet(hasSendFailPostNum);
    }
}
