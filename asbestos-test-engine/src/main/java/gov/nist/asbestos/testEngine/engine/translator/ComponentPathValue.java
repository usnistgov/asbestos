package gov.nist.asbestos.testEngine.engine.translator;

public class ComponentPathValue {
    private boolean replaced;
    private String relativePath;
    private String token;

    public ComponentPathValue(boolean replaced, String relativePath, String token) {
        this.replaced = replaced;
        if (this.replaced && relativePath == null) {
            throw new RuntimeException("relativePath cannot be null if it was replaced by token " + token);
        }
        this.relativePath = relativePath;
        this.token = token;
    }

    public boolean isReplaced() {
        return replaced;
    }

    public String getRelativePath() {
        return relativePath;
    }

    public String getToken() {
        return token;
    }
}
