package com.vaadin.hackathon.git;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;

public class MajorVersionInfo {
    private String majorVersion;
    private OffsetDateTime firstRelease;
    private OffsetDateTime lastRelease;
    private int numberOfReleases;

    private List<VersionDetails> allVersions;

    public String getMajorVersion() {
        return this.majorVersion;
    }

    public OffsetDateTime getFirstRelease() {
        return this.firstRelease;
    }

    public OffsetDateTime getLastRelease() {
        return this.lastRelease;
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

    public int getNumberOfReleases() {
        return this.numberOfReleases;
    }

    public void setNumberOfReleases(final int numberOfReleases) {
        this.numberOfReleases = numberOfReleases;
    }

    public List<VersionDetails> getAllVersions() {
        return this.allVersions;
    }

    public void setAllVersions(final List<VersionDetails> allVersions) {
        this.allVersions = allVersions;
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
