package com.vaadin.hackathon.git;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.revwalk.RevWalk;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class GitService {

    @Value("${github.repository.path}")
    private String githubRepositoryPath;

    public List<MajorVersionInfo> consolidatedVersionsInfo() throws IOException {
        final var versionMap = new HashMap<String, List<VersionDetails>>();
        final var versionDetails = this.fetchVersionDetails();

        for (final var item : versionDetails) {
            final var major = item.getVersion().split("\\.")[0];
            versionMap.computeIfAbsent(major, key -> new ArrayList<>()).add(item);
        }

        return versionMap.entrySet().stream().map(entry -> {
            final var versionInfo = new MajorVersionInfo();
            versionInfo.setMajorVersion(entry.getKey());

            final List<VersionDetails> details = entry.getValue();
            details.sort(Comparator.comparing(VersionDetails::getReleasedOn));

            versionInfo.setFirstRelease(details.get(0).getReleasedOn());
            versionInfo.setLastRelease(details.get(details.size() - 1).getReleasedOn());
            versionInfo.setNumberOfReleases(details.size());
            versionInfo.setAllVersions(details);
            return versionInfo;
        })
                         .sorted(Comparator.comparing(MajorVersionInfo::getMajorVersion))
                         .toList();
    }

    public List<VersionDetails> fetchVersionDetails() throws IOException {
        final var versionDetails = new ArrayList<VersionDetails>();

        final var repositoryFile = Path.of(this.githubRepositoryPath).toFile();
        final var git = Git.open(repositoryFile);
        final var repository = git.getRepository();

        try (final var walk = new RevWalk(repository)) {
            final var tags = repository.getRefDatabase().getRefsByPrefix(Constants.R_TAGS);
            tags.forEach(ref -> {
                final var tagName = "v" + ref.getName().replace("refs/tags/", "");
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
        return versionDetails;
    }
}
