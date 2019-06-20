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

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public List<ValE> getEle() {
        return ele;
    }

    public Set<ValType> getTypes() {
        return types;
    }
}
