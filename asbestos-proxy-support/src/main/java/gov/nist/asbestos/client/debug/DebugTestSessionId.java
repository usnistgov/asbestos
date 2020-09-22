package gov.nist.asbestos.client.debug;

import java.util.Objects;

public class DebugTestSessionId implements Comparable<DebugTestSessionId> {
    private String ftkTestSessionId;
    private String channelId;

    public DebugTestSessionId(String ftkTestSessionId, String channelId) {
        Objects.requireNonNull(ftkTestSessionId);
        Objects.requireNonNull(channelId);
        this.ftkTestSessionId = ftkTestSessionId;
        this.channelId = channelId;
    }

    public String getFtkTestSessionId() {
        return ftkTestSessionId;
    }

    public String getChannelId() {
        return channelId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DebugTestSessionId that = (DebugTestSessionId) o;

        if (!ftkTestSessionId.equals(that.ftkTestSessionId)) return false;
        return channelId.equals(that.channelId);
    }

    @Override
    public int hashCode() {
        int result = ftkTestSessionId.hashCode();
        result = 31 * result + channelId.hashCode();
        return result;
    }

    @Override
    public int compareTo(DebugTestSessionId o) {
        int result = this.ftkTestSessionId.compareTo(o.ftkTestSessionId);
        return result == 0 ? this.channelId.compareTo(o.channelId) : result;
    }
}
