package neoflix.services;

import neoflix.AppUtils;
import neoflix.AuthUtils;
import neoflix.Params;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.Values;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class PeopleService {
    private final Driver driver;
    private final List<Map<String,Object>> people;

    /**
     * The constructor expects an instance of the Neo4j Driver, which will be
     * used to interact with Neo4j.
     *
     * @param driver
     */
    public PeopleService(Driver driver) {
        this.driver = driver;
        this.people = AppUtils.loadFixtureList("people");
    }

    /**
     * This method should return a paginated list of People (actors or directors),
     * with an optional filter on the person's name based on the `q` parameter.
     *
     * Results should be ordered by the `sort` parameter and limited to the
     * number passed as `limit`.  The `skip` variable should be used to skip a
     * certain number of rows.
     *
     * @param params        Used to filter on the person's name, and query parameters for pagination and ordering
     * @return List<Person>
     */
    // tag::all[]
    public List<Map<String,Object>> all(Params params) {
        // Get a list of people from the database

        try(var session = driver.session()){
            var person = session.readTransaction(tx->{
                var query = """
                    MATCH (p:Person) 
                    WHERE $q IS null OR p.name CONTAINS $q
                    RETURN p {.*} as person
                    ORDER BY p.name ASC
                    SKIP $skip
                    LIMIT $limit
                    """;
                var result = tx
                    .run(query, Values.parameters("q",params.query(),"skip", params.skip(), "limit", params.limit()))
                    .list(r -> r.get("person").asMap());
                return result;
            });
            return person;

        }
    }
    // end::all[]

    /**
     * Find a user by their ID.
     *
     * If no user is found, a NotFoundError should be thrown.
     *
     * @param id   The tmdbId for the user
     * @return Person
     */
    // tag::findById[]
    public Map<String, Object> findById(String id) {
        //  Find a user by their ID
        try (var session = driver.session()) {
             var users = session.readTransaction(tx-> {

                 var query = """
                    MATCH (p:Person {tmdbId: $id})
                    RETURN p {
                      .*,
                      actedCount: size((p)-[:ACTED_IN]->()),
                      directedCount: size((p)-[:DIRECTED]->())
                    } AS person
                     """;

                var person = tx.run(query, Values.parameters("id", id)).single().get("person").asMap();
                return person;
            });
             return users;
        }


    }
    // end::findById[]

    /**
     * Get a list of similar people to a Person, ordered by their similarity score
     * in descending order.
     *
     * @param id     The ID of the user
     * @param params Query parameters for pagination and ordering
     * @return List<Person> similar people
     */
    // tag::getSimilarPeople[]
    public List<Map<String,Object>> getSimilarPeople(String id, Params params) {
        // TODO: Get a list of similar people to the person by their id

        try (var session = driver.session()) {

            var person = session.readTransaction(tx->{
                var result = tx.run(String.format("""
                        MATCH (:Person {tmdbId: $id})-[:ACTED_IN|DIRECTED]->(m)<-[r:ACTED_IN|DIRECTED]-(p)
                        RETURN p {
                          .*,
                          actedCount: size((p)-[:ACTED_IN]->()),
                          directedCount: size((p)-[:DIRECTED]->()),
                          inCommon: collect(m {.tmdbId, .title, type: type(r)})
                        } AS person
                        ORDER BY size(person.inCommon) DESC
                        SKIP $skip
                        LIMIT $limit
                        """),
                    Values.parameters("id", id, "skip", params.skip(), "limit", params.limit()))
                    .list(r -> r.get("person").asMap());
                return  result;
            }); return  person;

        }
    }
    // end::getSimilarPeople[]

}