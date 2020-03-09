package gov.nist.asbestos.simapi.validation;

import java.util.*;

public class ValE {
    Set<ValType> types = new HashSet<>();
    String msg;
    List<ValE> ele = new ArrayList<>();

    public ValE() {}

    public ValE(Val val) {
        val.add(this);
    }

    public ValE(ValE val) {
        val.add(this);
    }

    public ValE(String msg) {
        this.msg = msg;
    }

    public String toString() {
        StringBuilder buf = new StringBuilder();

        buf.append(types).append("  ").append(msg);

        return buf.toString();
    }

    public ValE asError() {
        types.add(ValType.Error);
        return this;
    }

    public ValE asWarning() {
        types.add(ValType.Warn);
        return this;
    }

    public ValE asRef() {
        types.add(ValType.Ref);
        return this;
    }

    public ValE asDoc() {
        types.add(ValType.Doc);
        return this;
    }

    public ValE asTranslation() {
        types.add(ValType.Translation);
        return this;
    }

    public ValE addIheRequirement(String reference) {
        ValE req = new ValE(this);
        this.getTypes().add(ValType.IHERequirement);
        req.setMsg(reference);
        req.getTypes().add(ValType.Reference);
        return this;
    }

    public ValE add(ValE ele) {
        this.ele.add(ele);
        return this;
    }

    public ValE addTr(ValE ele) {
        ele.asTranslation();
        this.ele.add(ele);
        return ele;
    }

    boolean ignore(String msg) {
        boolean ignored = false;
        if (msg.equals(this.msg)) {
            this.types.add(ValType.Ignored);
            ignored = true;
        }
        for (ValE e : ele)
            ignored = ignored || e.ignore(msg);
        return ignored;
    }

    public String getMsg() {
        return msg;
    }

    public ValE setMsg(String msg) {
        this.msg = msg;
        return this;
    }

    public List<ValE> getEle() {
        return ele;
    }

    public Set<ValType> getTypes() {
        return types;
    }

    public boolean hasErrors() {
        if (types.contains(ValType.Error) && !types.contains(ValType.Ignored))
            return true;
        for (ValE e : ele) {
            if (e.hasErrors())
                return true;
        }
        return false;
    }

    public boolean hasWarnings() {
        if (types.contains(ValType.Warn) && !types.contains(ValType.Ignored))
            return true;
        for (ValE e : ele) {
            if (e.hasWarnings())
                return true;
        }
        return false;
    }

    public boolean hasInfo() {
        for (ValType type : types) {
            if (type != ValType.Warn && type != ValType.Error)
                return true;
        }
        for (ValE e : ele) {
            if (e.hasInfo())
                return true;
        }
        return false;
    }

    // if this is names get* then translating to JSON via Jackson gets into loop
    public List<ValE> infos() {
        List<ValE> infos = new ArrayList<>();
        boolean includeMe = false;
        for (ValType type : types) {
            if (type != ValType.Warn && type != ValType.Error)
                includeMe = true;
        }
        if (includeMe)
            infos.add(this);
        for (ValE e : ele)
            infos.addAll(e.infos());
        return infos;
    }

    public List<ValE> getErrors() {
        List<ValE> errors = new ArrayList<>();
        for (ValE e : ele) {
            if (e.getTypes().contains(ValType.Error) && !e.getTypes().contains(ValType.Ignored))
                errors.add(e);
            if (!e.ele.isEmpty())
                errors.addAll(e.getErrors());
        }
        return errors;
    }

    public List<ValE> getWarnings() {
        List<ValE> warnings = new ArrayList<>();
        for (ValE e : ele) {
            if (e.getTypes().contains(ValType.Warn) && !e.getTypes().contains(ValType.Ignored))
                warnings.add(e);
            if (!e.ele.isEmpty())
                warnings.addAll(e.getWarnings());
        }
        return warnings;
    }

}
