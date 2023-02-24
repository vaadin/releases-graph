package com.vaadin.hackathon.git;

import java.time.OffsetDateTime;
import java.util.Objects;

public class VersionDetails {
    private String version;
    private OffsetDateTime releasedOn;

    public VersionDetails(final String version, final OffsetDateTime releasedOn) {
        this.version = version;
        this.releasedOn = releasedOn;
    }

    public String getVersion() {
        return this.version;
    }

    public OffsetDateTime getReleasedOn() {
        return this.releasedOn;
    }

    public void setVersion(final String version) {
        this.version = version;
    }

    public void setReleasedOn(final OffsetDateTime releasedOn) {
        this.releasedOn = releasedOn;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.version);
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof final VersionDetails other)) {
            return false;
        }
        return Objects.equals(this.version, other.version);
    }

    @Override
    public String toString() {
        return "VersionDetails [version=" + this.version + ", releasedOn=" + this.releasedOn + "]";
    }

}
