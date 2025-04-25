package com.vaadin.platform.git;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.TreeMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class GitService {

    class VersionComparator implements Comparator<String> {
        @Override
        public int compare(String version1, String version2) {
            String[] parts1 = version1.split("[\\.-]"); //split on '.'
            String[] parts2 = version2.split("[\\.-]");

            int length = Math.min(parts1.length, parts2.length);
            for (int i = 0; i < length; i++) {
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

    @Value("${github.repository.owner}")
    private String repoOwner;
    @Value("${github.repository.name}")
    private String repoName;
    @Autowired
    private GitHubTagService gitHubTagService;

    public List<MajorVersionInfo> consolidatedVersionsInfo() throws IOException, InterruptedException {
        final var versionMap = new TreeMap<String, List<VersionDetails>>(new VersionComparator());
        final var versionDetails = this.fetchVersionDetails(repoOwner, repoName);

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

    public List<VersionDetails> fetchVersionDetails(String repoOwner, String repoName ) throws IOException, InterruptedException {

        var versionDetails = this.gitHubTagService.fetchAllTags(repoOwner, repoName);
        versionDetails.sort(new VersionDetailsComparator());
        return versionDetails;
    }


}
