package com.vaadin.hackathon.git;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class GitHubService {

    private static final String GITHUB_RELEASE_API_URL = "https://api.github.com/repos/vaadin/platform/releases/tags/";
    private static final String PERSONAL_GITHUB_TOKEN = "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";

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Cacheable
    public String fetchReleaseNotes(final String versionName) {
        String releaseNotes = "";
        // remove the v which is always the first character
        final String tagName = versionName.substring(1);
        final String url = GITHUB_RELEASE_API_URL + tagName;

        try {
            final HttpRequest request = HttpRequest.newBuilder()
                                                   .headers("Accept", "application/vnd.github+json", "Authorization", "Bearer " + PERSONAL_GITHUB_TOKEN, "X-GitHub-Api-Version",
                                                            "2022-11-28")
                                                   .uri(new URI(url))
                                                   .build();

            final HttpClient httpClient = HttpClient.newHttpClient();
            final HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            final JsonNode parent = OBJECT_MAPPER.readTree(response.body());
            releaseNotes = parent.path("body").asText();
        } catch (IOException | InterruptedException | URISyntaxException e) {
            releaseNotes = "Failed to fetch release notes!";
        }
        return releaseNotes;
    }

}
