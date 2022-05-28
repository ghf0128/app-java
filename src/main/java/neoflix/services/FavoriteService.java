package neoflix.services;

import javax.naming.NameNotFoundException;
import neoflix.AppUtils;
import neoflix.Params;
import neoflix.ValidationException;
import org.neo4j.driver.Driver;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.Values;
import org.neo4j.driver.exceptions.NoSuchRecordException;

public class FavoriteService {

    private final Driver driver;

    private final List<Map<String, Object>> popular;
    private final List<Map<String, Object>> users;
    private final Map<String, List<Map<String, Object>>> userFavorites = new HashMap<>();

    /**
     * The constructor expects an instance of the Neo4j Driver, which will be used to interact with
     * Neo4j.
     *
     * @param driver
     */
    public FavoriteService(Driver driver) {
        this.driver = driver;
        this.popular = AppUtils.loadFixtureList("popular");
        this.users = AppUtils.loadFixtureList("users");
    }

    /**
     * This method should retrieve a list of movies that have an incoming :HAS_FAVORITE relationship
     * from a User node with the supplied `userId`.
     * <p>
     * Results should be ordered by the `sort` parameter, and in the direction specified in the
     * `order` parameter. Results should be limited to the number passed as `limit`. The `skip`
     * variable should be used to skip a certain number of rows.
     *
     * @param userId The unique ID of the user
     * @param params Query params for pagination and sorting
     * @return List<Movie> An list of Movie objects
     */
    // tag::all[]
    public List<Map<String, Object>> all(String userId, Params params) {


        try (var session = driver.session()) {
            var favorites = session.readTransaction(tx-> {
                String query = String.format(
                    """
                    MATCH (u:User {userId:$userId})-[r:HAS_FAVORITE]-(m:Movie)
                    RETURN m {.*,favorite:true} as movie
                    ORDER BY m.`%s` %s
                    SKIP $skip
                    LIMIT $limit
                    """,params.sort(Params.Sort.title),params.order());
                var result = tx
                    .run(query, Values.parameters("userId", userId, "skip", params.skip(), "limit", params.limit()));
                return result.list(row -> row.get("movie").asMap());
            }) ;
            return favorites;
        }


    }
    // end::all[]

    /**
     * This method should create a `:HAS_FAVORITE` relationship between the User and Movie ID nodes
     * provided.
     * <p>
     * If either the user or movie cannot be found, a `NotFoundError` should be thrown.
     *
     * @param userId  The unique ID for the User node
     * @param movieId The unique tmdbId for the Movie node
     * @return Map<String, Object></String,Object> The updated movie record with `favorite` set to true
     */
    // tag::add[]
    public Map<String, Object> add(String userId, String movieId) {

        // Open a new Session
        try (var session = driver.session()) {
            // Create HAS_FAVORITE relationship within a Write Transaction
            var favorite = session.writeTransaction(tx -> {
                String query = """
            MATCH (u:User {userId:$userId})
            MATCH (m:Movie {tmdbId:$movieId})
            MERGE (u)-[r:HAS_FAVORITE]->(m) 
                     on CREATE SET r.createAt = datetime()
            RETURN m {.*,favorite:true} as movie
                              
            """;
                var result = tx
                    .run(query, Values.parameters("userId", userId, "movieId", movieId));
                return result.single().get("movie").asMap();
            });
            // Return movie details and `favorite` property
            return favorite;
        } catch (NoSuchRecordException e) {
            throw new ValidationException("Couldn't create a favorite relationship for user",
                Map.of("movieId", movieId, "userId", userId));
        }

    }
    // end::add[]

    /*
     *This method should remove the `:HAS_FAVORITE` relationship between
     * the User and Movie ID nodes provided.
     * If either the user, movie or the relationship between them cannot be found,
     * a `NotFoundError` should be thrown.

     * @param userId The unique ID for the User node
     * @param movieId The unique tmdbId for the Movie node
     * @return Map<String,Object></String,Object> The updated movie record with `favorite` set to true
     */
    // tag::remove[]
    public Map<String, Object> remove(String userId, String movieId) {

        try (var session = driver.session()) {
            var favorite = session.writeTransaction(tx->{
                String query = """
                  MATCH (u:User {userId:$userId})-[r:HAS_FAVORITE]->(m:Movie {tmdbId:$movieId})
                  DELETE r
                  RETURN m {.*,favorite:false} as movie
                  """;
                var result = tx
                    .run(query, Values.parameters("userId", userId, "movieId", movieId));
                return result.single().get("movie").asMap();
            });
            return favorite;
        }catch (NoSuchRecordException e) {
            throw new ValidationException("Could not find the relationship",Map.of("movieId",movieId,"userId",userId));
        }
    }
    // end::remove[]

}
