package com.vaadin.hackathon.git;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.revwalk.RevWalk;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class GitService {

    class VersionComparator implements Comparator<String> {
        @Override
        public int compare(String version1, String version2) {
            String[] parts1 = version1.substring(1).split("[\\.-]"); // Remove 'v' and split on '.'
            String[] parts2 = version2.substring(1).split("[\\.-]");
            System.err.println(version1 + " " + version2);

            int length = Math.min(parts1.length, parts2.length);
            for (int i = 0; i < length; i++) {
                System.err.println(
                        length + " " + i + " " + parts1.length + " " + parts2.length + " " + parts1 + " " + parts2);
                int num1 = i < parts1.length ? Integer.parseInt(parts1[i]) : 0;
                int num2 = i < parts2.length ? Integer.parseInt(parts2[i]) : 0;

                if (num1 != num2) {
                    return Integer.compare(num1, num2);
                }
            }

            return 0;
        }
    }

    class VersionDetailsComparator implements Comparator<VersionDetails> {

        @Override
        public int compare(VersionDetails v1, VersionDetails v2) {
            int versionComparison = compareVersions(v1.getVersion(), v2.getVersion());
            if (versionComparison != 0) {
                return versionComparison;
            }
            // If versions are the same, compare by release date
            return v1.getReleasedOn().compareTo(v2.getReleasedOn());
        }

        private int compareVersions(String version1, String version2) {

            String[] parts1 = version1.split("[\\.-]");
            String[] parts2 = version2.split("[\\.-]");

            int maxLength = Math.min(parts1.length, parts2.length);
            for (int i = 0; i < maxLength; i++) {
                String part1 = i < parts1.length ? parts1[i].replaceAll("[^\\d]", "") : "0";
                String part2 = i < parts2.length ? parts2[i].replaceAll("[^\\d]", "") : "0";

                int numPart1 = part1.isEmpty() ? 0 : Integer.parseInt(part1);
                int numPart2 = part2.isEmpty() ? 0 : Integer.parseInt(part2);

                if (numPart1 != numPart2) {
                    return Integer.compare(numPart1, numPart2);
                }

                // Compare pre-release identifiers if numeric parts are equal
                if (i < parts1.length && i < parts2.length) {
                    String tail1 = parts1[i].replaceAll("\\d", "");
                    String tail2 = parts2[i].replaceAll("\\d", "");

                    if (!tail1.equals(tail2)) {
                        return comparePreRelease(tail1, tail2);
                    }
                }
            }

            return 0;
        }

        private int comparePreRelease(String pre1, String pre2) {
            String[] order = { "SNAPSHOT", "alpha", "beta", "rc", "" }; // "" represents stable

            int index1 = getPreReleaseIndex(pre1, order);
            int index2 = getPreReleaseIndex(pre2, order);

            return Integer.compare(index1, index2);
        }

        private int getPreReleaseIndex(String identifier, String[] order) {
            for (int i = 0; i < order.length; i++) {
                if (identifier.startsWith(order[i])) {
                    return i;
                }
            }
            return order.length;  // For unknown identifiers, place them last
        }
    }

    @Value("${github.repository.path}")
    private String githubRepositoryPath;

    public List<MajorVersionInfo> consolidatedVersionsInfo() throws IOException {
        final var versionMap = new TreeMap<String, List<VersionDetails>>(new VersionComparator());
        final var versionDetails = this.fetchVersionDetails();

        for (final var item : versionDetails) {
            var tmp = item.getVersion().split("[\\.-]");
            final var major = tmp[0] + "." + tmp[1];
            versionMap.computeIfAbsent(major, key -> new ArrayList<>()).add(item);
        }

        return  versionMap.entrySet().stream().map(entry -> {
            final var versionInfo = new MajorVersionInfo();
            versionInfo.setMajorVersion(entry.getKey());

            final List<VersionDetails> details = entry.getValue();
            details.sort(Comparator.comparing(VersionDetails::getReleasedOn));
            final List<VersionDetails> preVersions = details.stream().filter(r -> {
                return r.getVersion().matches(".*(\\.0|(SNAPSHOT|alpha|beta|rc)\\d+)");
            }).toList();

            versionInfo.setFirstRelease(details.get(0).getReleasedOn());
            versionInfo.setLastRelease(details.get(details.size() - 1).getReleasedOn());

            int idx = preVersions.size() - 1;
            VersionDetails last = idx >= 0 ? preVersions.get(idx) : details.get(details.size() - 1);
            versionInfo.setLastPreRelease(last.getReleasedOn());

            versionInfo.setNumberOfReleases(details.size());
            versionInfo.setNumberOfPreReleases(preVersions.size());
            versionInfo.setAllVersions(details);
            versionInfo.setPreVersions(preVersions);
            return versionInfo;
        }).toList();
    }

    public List<VersionDetails> fetchVersionDetails() throws IOException {
//
//        Map<String, OffsetDateTime> tagMap = fetchAllTags();
//        if (tagMap != null) {
//            tagMap.forEach((tag, date) -> System.out.println(tag + ": " + date));
//        }
//

        final var versionDetails = new ArrayList<VersionDetails>();

        final var repositoryFile = Path.of(this.githubRepositoryPath).toFile();
        final var git = Git.open(repositoryFile);
        final var repository = git.getRepository();

        try (final var walk = new RevWalk(repository)) {
            final var tags = repository.getRefDatabase().getRefsByPrefix(Constants.R_TAGS);
            tags.forEach(ref -> {
                final var tagName = " " + ref.getName().replace("refs/tags/", "");
                try {
                    final var committerIdent = walk.parseCommit(ref.getObjectId()).getCommitterIdent();
                    final var instant = Instant.ofEpochMilli(committerIdent.getWhen().getTime());
                    final var when = OffsetDateTime.ofInstant(instant, ZoneId.systemDefault());
                    versionDetails.add(new VersionDetails(tagName, when));
                } catch (final IOException e) {
                    e.printStackTrace();
                }
            });
        }

        versionDetails.sort(new VersionDetailsComparator());
        return versionDetails;
    }

    private static final String TOKEN = System.getenv("GITHUB_TOKEN");
    private static final String BASE_URL = "https://api.github.com/repos/vaadin/platform";
    private static final HttpClient client = HttpClient.newHttpClient();
    private static final ObjectMapper objectMapper = new ObjectMapper();


    private static Map<String, OffsetDateTime> fetchAllTags() {
        Map<String, OffsetDateTime> tagMap = new HashMap<>();
        int page = 1;
        int perPage = 100;

        while (true) {
            String url = String.format("%s/tags?page=%d&per_page=%d", BASE_URL, page, perPage);
            System.err.println(url);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "token " + TOKEN)
                    .header("Accept", "application/vnd.github.v3+json")
                    .timeout(Duration.ofMinutes(1))
                    .build();

            try {
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                System.err.println("response ");
                JsonNode tags = objectMapper.readTree(response.body());

                if (!tags.isArray() || tags.isEmpty()) {
                    break;  // No more tags, exit the loop
                }

                tags.forEach(tag -> {
                    String tagName = tag.get("name").asText();
                    String sha = tag.get("commit").get("sha").asText();

                    OffsetDateTime date = fetchCommitDate(sha);
                    if (date != null) {
                        tagMap.put(tagName, date);
                    }
                });
                page++;

            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
            System.err.println("OK");
        }

        return tagMap;
    }

    private static OffsetDateTime fetchCommitDate(String sha) {
        String commitUrl = String.format("%s/commits/%s", BASE_URL, sha);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(commitUrl))
                .header("Authorization", "token " + TOKEN)
                .header("Accept", "application/vnd.github.v3+json")
                .timeout(Duration.ofMinutes(1))
                .build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode commit = objectMapper.readTree(response.body());
            String dateStr = commit.get("commit").get("author").get("date").asText();
            return OffsetDateTime.parse(dateStr, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

}
