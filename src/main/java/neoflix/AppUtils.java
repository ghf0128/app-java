package neoflix;

import com.google.gson.Gson;
import org.neo4j.driver.AuthToken;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import spark.Request;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class AppUtils {
    public static void loadProperties() {
        try {
            var file = AppUtils.class.getResourceAsStream("/application.properties");
            if (file!=null) System.getProperties().load(file);
        } catch (IOException e) {
            throw new RuntimeException("Error loading application.properties", e);
        }
    }

    public static String getUserId(Request req) {
        Object user = req.attribute("user");
        if (user == null) return null;
        return user.toString();
    }

    static void handleAuthAndSetUser(Request req, String jwtSecret) {
        String token = req.headers("Authorization");
        String bearer = "Bearer ";
        if (token != null && !token.isBlank() && token.startsWith(bearer)) {
            // verify token
            token = token.substring(bearer.length());
            String userId = AuthUtils.verify(token, jwtSecret);
            req.attribute("user", userId);
        }
    }

    // tag::initDriver[]
    static Driver initDriver() {
        // Create and assign an instance of the driver here
//        return null;
        var authToken = AuthTokens.basic(getNeo4jUsername(), getNeo4jPassword());
        var driver = GraphDatabase.driver(getNeo4jUri(), authToken);
        driver.verifyConnectivity();
        return driver;
    }
    // end::initDriver[]

    static int getServerPort() {
        return Integer.parseInt(System.getProperty("APP_PORT", "3000"));
    }

    static String getJwtSecret() {
        return System.getProperty("JWT_SECRET");
    }

    static String getNeo4jUri() {
        return System.getProperty("NEO4J_URI");
    }
    static String getNeo4jUsername() {
        return System.getProperty("NEO4J_USERNAME");
    }
    static String getNeo4jPassword() {
        return System.getProperty("NEO4J_PASSWORD");
    }

    public static List<Map<String,Object>> loadFixtureList(final String name) {
        var fixture = new InputStreamReader(AppUtils.class.getResourceAsStream("/fixtures/" + name + ".json"));
        return GsonUtils.gson().fromJson(fixture,List.class);
    }
    public static List<Map<String, Object>> process(List<Map<String, Object>> result, Params params) {
        return params == null ? result : result.stream()
                .sorted((m1, m2) ->
                        (params.order() == Params.Order.ASC ? 1 : -1) *
                                ((Comparable)m1.getOrDefault(params.sort().name(),"")).compareTo(
                                        m2.getOrDefault(params.sort().name(),"")
                                ))
                .skip(params.skip()).limit(params.limit())
                .toList();
    }

    public static Map<String,Object> loadFixtureSingle(final String name) {
        var fixture = new InputStreamReader(AppUtils.class.getResourceAsStream("/fixtures/" + name + ".json"));
        return GsonUtils.gson().fromJson(fixture,Map.class);
    }
}
