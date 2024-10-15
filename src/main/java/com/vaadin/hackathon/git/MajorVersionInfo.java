package com.vaadin.hackathon.git;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;

public class MajorVersionInfo {
    private String majorVersion;
    private OffsetDateTime firstRelease;
    private OffsetDateTime lastRelease;
    private OffsetDateTime lastPreRelease;

    private int numberOfPreReleases;
    private int numberOfReleases;

    private List<VersionDetails> allVersions;
    private List<VersionDetails> preVersions;

    public String getMajorVersion() {
        return this.majorVersion;
    }

    public OffsetDateTime getFirstRelease() {
        return this.firstRelease;
    }

    public OffsetDateTime getLastRelease() {
        return this.lastRelease;
    }

    public OffsetDateTime getLastPreRelease() {
        return this.lastPreRelease;
    }

    public void setMajorVersion(final String majorVersion) {
        this.majorVersion = majorVersion;
    }

    public void setFirstRelease(final OffsetDateTime firstRelease) {
        this.firstRelease = firstRelease;
    }

    public void setLastRelease(final OffsetDateTime lastRelease) {
        this.lastRelease = lastRelease;
    }

    public void setLastPreRelease(final OffsetDateTime lastPreRelease) {
        this.lastPreRelease = lastPreRelease;
    }

    public int getNumberOfReleases() {
        return this.numberOfReleases;
    }

    public void setNumberOfReleases(final int numberOfReleases) {
        this.numberOfReleases = numberOfReleases;
    }

    public int getNumberOfPreReleases() {
        return this.numberOfPreReleases;
    }

    public void setNumberOfPreReleases(final int numberOfPreReleases) {
        this.numberOfPreReleases = numberOfPreReleases;
    }

    public List<VersionDetails> getAllVersions() {
        return this.allVersions;
    }

    public void setAllVersions(final List<VersionDetails> allVersions) {
        this.allVersions = allVersions;
    }

    public List<VersionDetails> getPreVersions() {
        return this.preVersions;
    }

    public void setPreVersions(final List<VersionDetails> preVersions) {
        this.preVersions = preVersions;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.majorVersion);
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof final MajorVersionInfo other)) {
            return false;
        }
        return Objects.equals(this.majorVersion, other.majorVersion);
    }

    @Override
    public String toString() {
        return "VersionInfo [majorVersion=" + this.majorVersion + ", firstRelease=" + this.firstRelease + ", lastRelease=" + this.lastRelease + "]";
    }

}
