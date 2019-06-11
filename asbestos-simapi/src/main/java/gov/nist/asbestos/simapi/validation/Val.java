package gov.nist.asbestos.simapi.validation;


import java.util.ArrayList;
import java.util.List;

// TODO order elements are added must be preserved
public class Val {
    List<ValE> elements = new ArrayList<>();

    public Val() {}

    public Val msg(String msg) {
        elements.add(new ValE(msg));
        return this;
    }

    public Val ref(String ref) {
        elements.add(new ValE(ref).asRef());
        return this;
    }

    public Val err(String err) {
        elements.add(new ValE(err).asError());
        return this;
    }

    // TODO need test
    public Val warn(String err) {
        elements.add(new ValE(err).asWarning());
        return this;
    }

    public Val frameworkDoc(String doc) {
        elements.add(new ValE(doc).asDoc());
        return this;
    }

    public Val add(ValE vale) {
        elements.add(vale);
        return this;
    }

    public boolean hasErrors() {
        for (ValE e : elements) {
            if (e.getType().equals(ValType.Error))
                return true;
        }
        return false;
    }

    public String toString() {
        return ValFactory.toJson(this);
    }

    public List<ValE> getElements() {
        return elements;
    }
}
