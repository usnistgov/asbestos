package gov.nist.asbestos.simapi.validation;


import java.util.ArrayList;
import java.util.List;

// TODO order elements are added must be preserved
public class Val {
    private List<String> msgs = new ArrayList<>();
    private List<String> refs = new ArrayList<>();
    private List<Val> errs = new ArrayList<>();
    private List<Val> warns = new ArrayList<>();
    private List<String> frameworkDocs = new ArrayList<>();
    private List<Val> children = new ArrayList<>();

    public Val() {

    }

    public Val(String msg) {
        msg(msg);
    }

    public Val msg(String msg) {
        msgs.add(msg);
        return this;
    }

    public Val ref(String ref) {
        refs.add(ref);
        return this;
    }

    public Val err(Val err) {
        errs.add(err);
        return this;
    }

    // TODO need test
    public Val warn(Val err) {
        warns.add(err);
        return this;
    }

    public Val frameworkDoc(String doc) {
        frameworkDocs.add(doc);
        return this;
    }

    public Val add(Val val) {
        children.add(val);
        return this;
    }

    public Val add(String msg) {
        Val v = new Val();
        v.msg(msg);
        this.add(v);
        return this;
    }

    public Val addSection(String msg) {
        Val v = new Val();
        v.msg(msg);
        this.add(v);
        return v;
    }

    public boolean hasErrors() {
        return errs.size() != 0;
    }

    public String toString() {
        StringBuilder buf = new StringBuilder();

        render(this, buf, 0);

        return buf.toString();
    }

    public List<Val> getChildren() {
        return children;
    }

    // TODO update to match new type for err
    private static void render(Val val, StringBuilder buf, int level) {
        for (String msg : val.msgs) {
            indent(level, buf);
            buf.append(msg).append('\n');
        }

        if (val.errs.size() != 0) {
            buf.append("Errors:\n");
            val.errs.forEach(err -> {
                indent(level, buf);
                buf.append(err).append('\n');
            });
        }
        if (val.frameworkDocs.size() != 0) {
            buf.append("Framework Documentation:\n");
            val.frameworkDocs.forEach(doc -> {
                indent(level, buf);
                buf.append(doc).append('\n');
            });
        } else if (val.refs.size() != 0) {
            buf.append("References:\n");
            val.refs.forEach(ref -> {
                indent(level, buf);
                buf.append(ref).append('\n');
            });
        } else {
            val.children.forEach(val1 -> {
                render(val1, buf, level + 1);
            });
        }
    }

    private static void indent(int level, StringBuilder buf) {
        for (int i=0; i<level; i++) {
            buf.append("  ");
        }
    }
}
