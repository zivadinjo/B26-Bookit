package com.bookit.step_definitions;
import com.bookit.pages.LogInPage;
import com.bookit.pages.MapPage;
import com.bookit.pages.SelfPage;
import com.bookit.utilities.*;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;
import static io.restassured.RestAssured.*;
import static org.junit.Assert.assertEquals;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

public class BookItAPIStepDefs {

    public static final Logger LOG = LogManager.getLogger();
    String baseUrl = Environment.BASE_URL;
    String accessToken;
    Response response;
    Map<String, String> newRecordMap;

    @Given("User logged in to Bookit api as teacher role")
    public void user_logged_in_to_Bookit_api_as_teacher_role() {
        String email = Environment.TEACHER_EMAIL;
        String password = Environment.TEACHER_PASSWORD;
        LOG.info("Authorizing teacher user : email = " + email + ", password = " + password);
        LOG.info("Environment base url = " + baseUrl);

        accessToken = BookItApiUtil.getAccessToken(email, password);

        if (accessToken == null || accessToken.isEmpty()) {
            LOG.error("Could not authorize user in authorization server");
            fail("Could not authorize user in authorization server");
        }

    }

    @Given("User sends GET request to {string}")
    public void user_sends_GET_request_to(String endpoint) { ///api/users/me
        response = given().accept(ContentType.JSON)
                .and().header("Authorization",  accessToken)
                .when().get(baseUrl + endpoint);
        response.then().log().all();
    }

    @Then("status code should be {int}")
    public void status_code_should_be(int expStatusCode) {
        assertEquals("Status code verification failed",expStatusCode, response.statusCode());
        response.then().statusCode(expStatusCode);
    }

    @Then("content type is {string}")
    public void content_type_is(String expContentType) {
        response.then().contentType(expContentType);
        assertEquals("Content type verification failed. expected = " + expContentType +" but actual = " + response.contentType()
                ,expContentType, response.contentType());
    }

    /**
     {
     "id": 11516,
     "firstName": "Barbabas",
     "lastName": "Lyst",
     "role": "teacher"
     }
     */
    @Then("role is {string}")
    public void role_is(String expRole) {
        assertEquals(expRole, response.path("role"));

        JsonPath jsonPath = response.jsonPath();
        assertEquals(expRole, jsonPath.getString("role"));

        //deserialization: json to map or json to pojo
        Map<String, ?> responseMap = response.as(Map.class);
        assertEquals(expRole, responseMap.get("role"));
    }

    @Given("User logged in to Bookit app as teacher role")
    public void user_logged_in_to_Bookit_app_as_teacher_role() {
        //goto login page
        Driver.getDriver().get(Environment.URL);
        LogInPage logInPage = new LogInPage();
        logInPage.login(Environment.TEACHER_EMAIL, Environment.TEACHER_PASSWORD);
        //TODO: add explicit wait for url change
        //assertTrue(Driver.getDriver().getCurrentUrl().endsWith("map"));
    }

    @Given("User is on self page")
    public void user_is_on_self_page() {
        MapPage mapPage = new MapPage();
        mapPage.gotoSelfPage();
    }

    @Then("User should see same info on UI and API")
    public void user_should_see_same_info_on_UI_and_API() {
        SelfPage selfPage = new SelfPage();
        String fullName = selfPage.fullName.getText();
        String role = selfPage.role.getText();

        Map<String, String> uiUserDataMap = new HashMap<>();
        uiUserDataMap.put("role", role);
        String[] name = fullName.split(" "); //[0] = Firstname, [1] = Lastname
        uiUserDataMap.put("firstName", name[0]);
        uiUserDataMap.put("lastName", name[1]);

        System.out.println("uiUserDataMap = " + uiUserDataMap);

        Map<String, ?> responseMap = response.as(Map.class);
        responseMap.remove("id");//delete id to compare with ui
        assertThat(uiUserDataMap, equalTo(responseMap));

    }

    /**
     {
     "entryiId": 14945,
     "entryType": "Team",
     "message": "team Wooden Spoon7987 has been added to the batch 26."
     }
     */

    @When("Users sends POST request to {string} with following info:")
    public void users_sends_POST_request_to_with_following_info(String endpoint, Map<String, String> teamInfo) {
        response =given().accept(ContentType.JSON)
                .and().queryParams(teamInfo)
                .and().header("Authorization", accessToken)
                .when().post(baseUrl + endpoint);
        response.prettyPrint();
        newRecordMap = teamInfo;
    }

    @Then("Database should persist same team info")
    public void database_should_persist_same_team_info() {
        int newTeamID = response.path("entryiId");

        String sql = "SELECT * FROM team WHERE id = " + newTeamID;
        Map<String, Object> dbNewTeamMap = DBUtils.getRowMap(sql);

        System.out.println("sql = " + sql);
        System.out.println("dbNewTeamMap = " + dbNewTeamMap);

        assertThat(dbNewTeamMap.get("id"), equalTo((long)newTeamID));
        assertThat(dbNewTeamMap.get("name"), equalTo(newRecordMap.get("team-name")));
        assertThat(dbNewTeamMap.get("batch_number").toString(), equalTo(newRecordMap.get("batch-number")));
    }

    @Then("User deletes previously created team")
    public void user_deletes_previously_created_team() {
        int teamId = response.path("entryiId");
        given().accept(ContentType.JSON)
                .and().header("Authorization", accessToken)
                .and().pathParam("id", teamId)
                .when().delete(baseUrl + "/api/teams/{id}")
                .then().log().all();
    }

}


