package gov.nist.asbestos.proxyWar;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;

class ExternalCache {

    public File getExternalCache() {
        URL url = this.getClass().getResource("/external_cache/findme.txt");
        assert url != null;

        File ec = null;
        try {
            ec = Paths.get(this.getClass().getResource("/").toURI()).resolve("external_cache/findme.txt").toFile().getParentFile();
        } catch (URISyntaxException e) {
            assert false;
        }
        assert ec != null;
        return ec;
    }
}
