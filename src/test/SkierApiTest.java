import io.swagger.client.ApiClient;
import io.swagger.client.ApiException;
import io.swagger.client.api.SkiersApi;
import io.swagger.client.model.LiftRide;

public class SkierApiTest {

    public static void main(String[] args) {
        SkiersApi apiInstance = new SkiersApi();
        ApiClient apiClient = new ApiClient();
        apiClient.setBasePath("http://127.0.0.1:8080");
        apiInstance.setApiClient(apiClient);
        LiftRide body = new LiftRide(); // LiftRide | Specify new Season value
        Integer resortID = 1; // Integer | ID of the resort the skier is at
        String seasonID = "2019"; // String | ID of the ski season
        String dayID = "1"; // String | ID number of ski day in the ski season
        Integer skierID = 56; // Integer | ID of the skier riding the lift
        try {
            apiInstance.writeNewLiftRide(body, resortID, seasonID, dayID, skierID);
        } catch (ApiException e) {
            System.err.println("Exception when calling SkiersApi#writeNewLiftRide");
            e.printStackTrace();
        }
    }
}
