package tests;

import com.google.gson.Gson;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.json.JSONObject;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import preparation.CreateBookingBody;
import preparation.PatchBookingBody;


import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

public class RestAssuredTests {

    public String cookiesToken;
    private int bookingId;
    private int testBookingId;

    private String baseURI;

    static final String FIRST_NAME = "Alex";

    static final String FIRST_NAME_CHANGED = "Kris";
    static final String ADDITIONAL_NEEDS = "Dog";
    static final int TOTAL_PRICE = 200;
    @BeforeMethod
    public void setup() {
        baseURI = "https://restful-booker.herokuapp.com";

        RestAssured.requestSpecification = new RequestSpecBuilder()
                .setBaseUri(baseURI)
                .setContentType(ContentType.JSON)
                .build();
        Response response = given()
                .body("{\"username\":\"admin\",\"password\":\"password123\"}")
                .post("/auth");

        String token = response.jsonPath().getString("token");

        if (cookiesToken == null) {
            cookiesToken = token;
            // RestAssured.requestSpecification.header("Authorization", "Bearer " + bearerToken);
            RestAssured.requestSpecification.cookie("token", token);

            System.out.println(">>> token = "+ cookiesToken);
        }

        response.then().statusCode(200);
    }
    @Test
    public void CreateBooking() {

        CreateBookingBody.BookingDates bookingDates = new CreateBookingBody.BookingDates().builder()
                .checkin("2018-01-01")
                .checkout("2019-01-01")
                .build();

        CreateBookingBody requestBody = new CreateBookingBody().builder()
                .firstname(FIRST_NAME)
                .lastname("Levchenko")
                .totalprice(100)
                .depositpaid(true)
                .bookingdates(bookingDates)
                .additionalneeds("Dinner")
                .build();

        Response response = given().log().all()
                .baseUri(baseURI)
                .contentType("application/json")
                .header("Accept", "application/json")
                .cookies("token", cookiesToken)
                .body(requestBody)
                .post("/booking");


        String responseBody = response.getBody().asString();

        JSONObject responseJson = new JSONObject(responseBody);

        bookingId = responseJson.getInt("bookingid");

        System.out.println(">>> create bookingid = "+bookingId);

        response.then().statusCode(200);
    }

    @Test
    public void getAllBookingIdsTest() {

        Response response = given().log().all()
                .baseUri(baseURI)
                .contentType("application/json")
                .header("Accept", "application/json")
                .cookies("token", cookiesToken)
                .get("/booking");

        response.then().statusCode(200);
        testBookingId = response.jsonPath().getInt("[0].bookingid");
    }

    @Test
    public void getBookingByIdTest() {

        Response response = given().log().all()
                .baseUri(baseURI)
                .contentType("application/json")
                .header("Accept", "application/json")
                .cookies("token", cookiesToken)
                .pathParam("id", bookingId)
                .get("/booking/{id}");

        response.then().assertThat().body("firstname", equalTo(FIRST_NAME));
        response.then().statusCode(200);
        response.prettyPrint();
    }

    @Test
    public void updateBookingTotalPriceTest() {

        PatchBookingBody requestBody = PatchBookingBody.builder()
                .totalprice(TOTAL_PRICE)
                .build();

        Response response = given().log().all()
                .baseUri(baseURI)
                .contentType("application/json")
                .header("Accept", "application/json")
                .cookies("token", cookiesToken)
                .body(requestBody)
                .pathParam("id", bookingId)
                .patch("/booking/{id}");


        response.then().assertThat().body("totalprice", equalTo(TOTAL_PRICE));
        response.then().statusCode(200);
        response.prettyPrint();

    }
    @Test
    public void updateBookingAdditionalNeedsTest()  {

        // get info by id
        Response response = given().log().all()
                .baseUri(baseURI)
                .contentType("application/json")
                .header("Accept", "application/json")
                .cookies("token", cookiesToken)
                .pathParam("id", testBookingId)
                .get("/booking/{id}");


        Gson gson = new Gson();
        CreateBookingBody responseObj = gson.fromJson(response.asString(), CreateBookingBody.class);

        responseObj.setFirstname(FIRST_NAME_CHANGED);
        responseObj.setAdditionalneeds(ADDITIONAL_NEEDS);

        Response response2 = given().log().all()
                .baseUri(baseURI)
                .contentType("application/json")
                .header("Accept", "application/json")
                .cookies("token", cookiesToken)
                .body(responseObj)
                .pathParam("id", testBookingId)
                .put("/booking/{id}");

        response2.then().assertThat().body("firstname", equalTo(FIRST_NAME_CHANGED));
        response2.then().assertThat().body("additionalneeds", equalTo(ADDITIONAL_NEEDS));
        response2.then().statusCode(200);

    }
    @Test
    public void deleteBookingTest() {

        // get some id
        Response response = given().log().all()
                .baseUri(baseURI)
                .contentType("application/json")
                .header("Accept", "application/json")
                .cookies("token", cookiesToken)
                .get("/booking");

        int id = response.jsonPath().getInt("[0].bookingid");

        // delete this id
        Response response2 = given().log().all()
                .baseUri(baseURI)
                .header("Authorization", "Basic YWRtaW46cGFzc3dvcmQxMjM=")
                .contentType("text/plain")
                .cookies("token", cookiesToken)
                .pathParam("id", id)
                .delete("/booking/{id}");

        response2.then().statusCode(201);
    }
}
