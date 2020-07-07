package gov.nist.asbestos.proxyWar;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;

public class ExternalCache {

    static public File getExternalCache() {
        URL url = ExternalCache.class.getResource("/external_cache/findme.txt");
        assert url != null;

        File ec = null;
        try {
            ec = Paths.get(ExternalCache.class.getResource("/").toURI()).resolve("external_cache/findme.txt").toFile().getParentFile();
        } catch (URISyntaxException e) {
            assert false;
        }
        assert ec != null;
        return ec;
    }


    static public File getExternalCache(String section) {
        URL url = ExternalCache.class.getResource("/" + section + "/external_cache/findme.txt");
        assert url != null;

        File ec = null;
        try {
            ec = Paths.get(ExternalCache.class.getResource("/").toURI())
                    .resolve(section + "/external_cache/findme.txt")
                    .toFile().getParentFile();
        } catch (URISyntaxException e) {
            assert false;
        }
        assert ec != null;
        assert ec.exists();
        return ec;
    }
}
