package com.vaadin.platform.git;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class GitHubService {
    private static final Logger LOGGER = LogManager
            .getLogger(GitHubService.class);

    @Value("${github.release.api.url}")
    private String githubReleaseApiUrl;

    @Value("${github.personal.token}")
    private String githubPersonalToken;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Cacheable
    public String fetchReleaseNotes(final String versionName) {
        String releaseNotes = "";
        final String url = this.githubReleaseApiUrl.formatted(versionName);

        try {
            final HttpRequest request = HttpRequest.newBuilder()
                    .header("Accept", "application/vnd.github+json")
                    .header("Authorization",
                            "Bearer " + this.githubPersonalToken)
                    .header("X-GitHub-Api-Version", "2022-11-28")
                    .uri(new URI(url)).build();

            final HttpClient httpClient = HttpClient.newHttpClient();
            final HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                final JsonNode parent = OBJECT_MAPPER.readTree(response.body());
                releaseNotes = parent.path("body").asText();
            } else {
                releaseNotes = "Failed to fetch release notes!";
                LOGGER.fatal(releaseNotes, response.body());
            }
        } catch (IOException | InterruptedException | URISyntaxException e) {
            releaseNotes = "Failed to fetch release notes!";
            LOGGER.fatal(releaseNotes, e);
        }
        return releaseNotes;
    }

}
