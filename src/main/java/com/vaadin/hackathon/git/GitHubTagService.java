package com.vaadin.hackathon.git;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class GitHubTagService {

    @Value("${tagservice.tagcache.enabled}")
    private boolean tagCacheEnabled;
    @Value("${tagservice.tagcache.maxAge}")
    private long tagCacheDuration;
    @Value("${tagservice.tagcache.folder}")
    private String tagCacheFolder;
    @Value("${github.personal.token}")
    private String githubToken;


    private Instant lastCachedTime = Instant.now();

    private static final Logger LOGGER = LogManager.getLogger(GitHubTagService.class);
    private static final String GITHUB_API_URL = "https://api.github.com/graphql";

    private static final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(20))
            .build();

    // GraphQL query with pagination: pageInfo and cursor argument
    private static final String QUERY = "query ListAllTagsWithCreator($owner: String!, $name: String!, $pageSize: Int!, $after: String) {"
            + " repository(owner: $owner, name: $name) {"
            + "   refs(refPrefix: \"refs/tags/\", first: $pageSize, after: $after, orderBy: { field: TAG_COMMIT_DATE, direction: DESC }) {"
            + "     pageInfo { hasNextPage endCursor }"
            + "     nodes {"
            + "       name"
            + "       target { __typename"
            + "         ... on Tag { tagger { name date user { login } } }"
            + "         ... on Commit { committedDate author { name user { login } } }"
            + "       }"
            + "     }"
            + "   }"
            + " }"
            + "}";

    /**
     * Returns a list of all tags together with the committer and commit date for a given Github repository
     * @param repoOwner The user or organization that owns a repository. For example, "vaadin". Must match the spelling on Github.
     * @param repoName The repository inside an organization. For example, "platform". Must match the spelling on Github.
     * @param An optional (can be null!) Github to be used when fetching large amounts of data
     */
    public List<VersionDetails> fetchAllTags(String repoOwner, String repoName) throws IOException, InterruptedException {

        if(tagCacheEnabled && lastCachedTime.plusSeconds(tagCacheDuration).isAfter(Instant.now()) ){
            //try fetching data from cache
            var cachedData = fetchTagsFromCache(repoOwner, repoName);
            if(cachedData!=null){
                return cachedData;
            }
        }

        int pageSize = 100;
        String cursor = null;

        List<VersionDetails> result = new ArrayList<>();

        do {
            JsonNode data = fetchTags(repoOwner, repoName, pageSize, cursor, githubToken);
            JsonNode refs = data.path("repository").path("refs");

            // Process each tag
            for (JsonNode node : refs.path("nodes")) {
                String tagName = node.path("name").asText();
                JsonNode target = node.path("target");
                String type = target.path("__typename").asText();
                String createdBy = null;
                String createdAt = null;

                if ("Tag".equals(type)) {
                    JsonNode tagger = target.path("tagger");
                    createdBy = tagger.path("user").path("login").asText(null);
                    if (createdBy == null) createdBy = tagger.path("name").asText();
                    createdAt = tagger.path("date").asText();
                } else if ("Commit".equals(type)) {
                    JsonNode author = target.path("author");
                    createdBy = author.path("user").path("login").asText(null);
                    if (createdBy == null) createdBy = author.path("name").asText();
                    createdAt = target.path("committedDate").asText();
                }
                result.add(new VersionDetails(tagName, OffsetDateTime.parse(createdAt) ,createdBy));
            }

            // Handle pagination
            JsonNode pageInfo = refs.path("pageInfo");
            boolean hasNext = pageInfo.path("hasNextPage").asBoolean();
            cursor = hasNext ? pageInfo.path("endCursor").asText() : null;
        } while (cursor != null);

        if(tagCacheEnabled){
            saveToCache(repoOwner, repoName, result);
        }
        return result;
    }

    private static JsonNode fetchTags(String owner, String name, int pageSize, String after, String githubToken) throws IOException, InterruptedException {
        // Build GraphQL variables
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        ObjectNode variables = mapper.createObjectNode()
                .put("owner", owner)
                .put("name", name)
                .put("pageSize", pageSize);
        if (after != null) variables.put("after", after);

        ObjectNode payload = mapper.createObjectNode();
        payload.put("query", QUERY);
        payload.set("variables", variables);
        String requestBody = payload.toString();

        var builder = HttpRequest.newBuilder()
                .uri(URI.create(GITHUB_API_URL))
                .timeout(Duration.ofSeconds(60))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody));

        if(githubToken!=null && !githubToken.isEmpty()){
            builder.header("Authorization", "Bearer " + githubToken);
        }else{
            LOGGER.info("Querying API without bearer token. You might run into rate limitations.");
        }

        HttpRequest request = builder.build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 403 || response.statusCode() == 429){
            throw new IOException("GraphQL API quota exceeded!");
        }else if (response.statusCode() / 100 != 2) {
            throw new IOException("Unexpected response status: " + response.statusCode() + " body: " + response.body());
        }

        JsonNode root = mapper.readTree(response.body());
        if (root.has("errors")) {
            throw new IOException("GraphQL errors: " + root.path("errors").toString());
        }
        return root.path("data");
    }

    private List<VersionDetails> fetchTagsFromCache(String repoOwner, String repoName) {
        try {
            File cacheFile = new File(tagCacheFolder,
                "github-tags-" + repoOwner + "_" + repoName + ".json");

            if (!cacheFile.exists()) {
                return null;
            }

            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());

            List<VersionDetails> ret = mapper.readValue(cacheFile,
                mapper.getTypeFactory().constructCollectionType(List.class, VersionDetails.class));
            LOGGER.info("Using cached data from {}", cacheFile.getAbsolutePath());
            return ret;
        } catch (IOException e) {
            LOGGER.warn("Problems when trying to read tag cache file. Using life query instead.", e);
            return null;
        }
    }

private void saveToCache(String repoOwner, String repoName, List<VersionDetails> details) {
    try {
        File cacheFile = new File(tagCacheFolder,
            "github-tags-" + repoOwner + "_" + repoName + ".json");

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModules(new JavaTimeModule());
        mapper.writeValue(cacheFile, details);
        // Update the last cached time
        lastCachedTime = Instant.now();

        LOGGER.info("Using cached data from {}", cacheFile.getAbsolutePath());
    } catch (IOException e) {
        LOGGER.warn("Failed to write tag cache file", e);
    }
}

}
