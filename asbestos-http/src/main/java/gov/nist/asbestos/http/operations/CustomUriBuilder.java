package gov.nist.asbestos.http.operations;

import java.net.URI;
import java.net.URISyntaxException;

public class CustomUriBuilder {
    private String scheme = null;
    private String authority = null;
    private String path;
    private String query = null;
    private String fragment = null;

    public CustomUriBuilder() {

    }

    public CustomUriBuilder(String url) throws URISyntaxException {
        String pathPlus;
        String[] parts;
        if (url.startsWith("/") || !url.startsWith("http")) {
            // no scheme or authority
            pathPlus = url;
        } else {
            parts = url.split("://", 2);
            String authorityPlus;
            if (parts.length == 2) {
                scheme = parts[0];
                authorityPlus = parts[1];
            } else {
                authorityPlus = parts[0];
            }
            parts = authorityPlus.split("/", 2);
            if (parts.length == 2) {
                authority = parts[0];
                if (parts[1].startsWith("/"))
                    pathPlus = parts[1];
                else
                    pathPlus = "/" + parts[1];
            } else {
                pathPlus = parts[0];
            }
        }
        String queryPlus;
        parts = pathPlus.split("\\?");
        if (parts.length == 2) {
            path = parts[0];
            queryPlus = parts[1];
            if (queryPlus.contains("#")) {
                parts = queryPlus.split("#");
                query = parts[0];
                if (parts.length == 2)
                    fragment = parts[1];
            } else {
                query = queryPlus;
            }
        } else {  // no ? found
            parts = pathPlus.split("#");
            if (parts.length == 2) {
                path = parts[0];
                fragment = parts[1];
            } else {
                parts = pathPlus.split("#");
                if (parts.length == 2) {
                    fragment = parts[1];
                }
                path = parts[0];
            }
        }
        if (query != null && query.equals(""))
            query = null;
        if (fragment != null && fragment.equals(""))
            fragment = null;
    }

    public URI build() {
        try {
            URI uri = new URI(scheme, authority, path, query, fragment);
            String uriString = uri.toString();
            if (uriString.endsWith("?"))
                throw new RuntimeException("URI ends with ?");
            return uri;
        } catch (URISyntaxException e) {
            throw new RuntimeException("Cannot parse URI: " + toString(), e);
        }
    }

    public String toString() {
        return scheme + "://" + authority + path +
                (query == null ? "" : "?" + query) +
                (fragment == null ? "" : "#" + fragment);
    }

    public String getScheme() {
        return scheme;
    }

    public String getAuthority() {
        return authority;
    }

    public String getPath() {
        return path;
    }

    public String getQuery() {
        return query;
    }

    public String getFragment() {
        return fragment;
    }

    public CustomUriBuilder setScheme(String scheme) {
        this.scheme = scheme;
        return this;
    }

    public CustomUriBuilder setAuthority(String authority) {
        this.authority = authority;
        return this;
    }

    public CustomUriBuilder setPath(String path) {
        this.path = path;
        return this;
    }

    public CustomUriBuilder setQuery(String query) {
        if (query != null && query.equals(""))
            this.query = null;
        else
            this.query = query;
        return this;
    }

    public CustomUriBuilder setFragment(String fragment) {
        this.fragment = fragment;
        return this;
    }
}
