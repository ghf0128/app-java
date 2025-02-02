// Task: Rewrite the AuthService to allow users to authenticate against the database
// Outcome: A user will be able to authenticate against their database record
package neoflix;

import neoflix.services.FavoriteService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Values;

import static org.junit.jupiter.api.Assertions.*;

class _07_FavoritesListTest {
    private static Driver driver;

    private static final String toyStory = "862";
    private static final String goodfellas = "769";
    private static final String userId = "9f965bf6-7e32-4afb-893f-756f502b2c2a";
    private static final String email = "graphacademy.favorite@neo4j.com";

    @BeforeAll
    static void initDriver() {
        AppUtils.loadProperties();
        driver = AppUtils.initDriver();
        if (driver != null) {
            try (var session = driver.session()) {
                session.writeTransaction(tx -> tx.run("""
                        MERGE (u:User {userId: $userId}) SET u.email = $email
                        """, Values.parameters("userId", userId, "email", email)));
            }
        }
    }

    @AfterAll
    static void closeDriver() {
        if (driver!=null) driver.close();
    }

    @Test
    void notFoundIfMovieOrUserNotExist() {
        FavoriteService favoriteService = new FavoriteService(driver);

        try {
            favoriteService.add("unknown", "x999");
            fail("Adding favorite with unknown userId or movieId should fail");
        } catch (Exception e) {
            assertEquals("Couldn't create a favorite relationship for user", e.getMessage());
        }
    }

    @BeforeEach
    void removeFavorites() {
        if (driver != null)
            try (var session = driver.session()) {
                session.writeTransaction(tx ->
                    tx.run("MATCH (u:User {userId: $userId})-[r:HAS_FAVORITE]->(m:Movie) DELETE r",
                        Values.parameters("userId", userId)));
            }
    }

    @Test
    void saveMovieToUserFavorites() {
        FavoriteService favoriteService = new FavoriteService(driver);

        var output = favoriteService.add(userId, goodfellas);

        assertNotNull(output);
        assertEquals(goodfellas, output.get("tmdbId"));
        assertTrue((Boolean)output.get("favorite"), "goodfellas is favorite");

        var favorites = favoriteService.all(userId, new Params(null, Params.Sort.title, Params.Order.DESC, 10, 0));

        var movieFavorite = favorites.stream().anyMatch(movie -> movie.get("tmdbId").equals(goodfellas));
        assertTrue(movieFavorite, "goodfellas is a favorite movie");
    }

    @Test
    void addAndRemoveMovieFromFavorites() {
        FavoriteService favoriteService = new FavoriteService(driver);

        var add = favoriteService.add(userId, goodfellas);
        assertEquals(goodfellas, add.get("tmdbId"));
        assertTrue((Boolean)add.get("favorite"), "goodfellas is favorite");

        var addToyStory = favoriteService.add(userId, toyStory);
        assertEquals(toyStory, addToyStory.get("tmdbId"));
        assertTrue((Boolean)addToyStory.get("favorite"), "toyStory is favorite");


        var addCheck = favoriteService.all(userId, new Params(null, Params.Sort.title, Params.Order.DESC, 10, 0));
        var found = addCheck.stream().anyMatch(movie -> movie.get("tmdbId").equals(goodfellas));
        assertTrue(found, "goodfellas is a favorite");

        var addCheck_toyStory = favoriteService.all(userId, new Params(null, Params.Sort.title, Params.Order.DESC, 10, 0));
        var found_toyStory = addCheck_toyStory.stream().anyMatch(movie -> movie.get("tmdbId").equals(toyStory));
        assertTrue(found_toyStory, "toyStory is a favorite");

        var remove = favoriteService.remove(userId, goodfellas);
        assertEquals(goodfellas, remove.get("tmdbId"));
        assertEquals(false, remove.get("favorite"), "goodfellas is not a favorite anymore");

        var removeToyStory = favoriteService.remove(userId, toyStory);
        assertEquals(toyStory, removeToyStory.get("tmdbId"));
        assertEquals(false, removeToyStory.get("favorite"), "toyStory is not a favorite anymore");


        var removeCheck = favoriteService.all(userId, new Params(null, Params.Sort.title, Params.Order.DESC, 10, 0));
        var notFound = removeCheck.stream().anyMatch(movie -> movie.get("tmdbId").equals(goodfellas));
        assertFalse(notFound, "goodfellas is not a favorite anymore");


        var removeCheckToyStory = favoriteService.all(userId, new Params(null, Params.Sort.title, Params.Order.DESC, 10, 0));
        var notFoundToyStory = removeCheckToyStory.stream().anyMatch(movie -> movie.get("tmdbId").equals(toyStory));
        assertFalse(notFoundToyStory, "toyStory is not a favorite anymore");

    }
}
