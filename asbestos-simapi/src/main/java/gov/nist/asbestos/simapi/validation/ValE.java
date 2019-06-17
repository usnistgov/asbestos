package gov.nist.asbestos.simapi.validation;

import java.util.ArrayList;
import java.util.List;

public class ValE {
    ValType type = ValType.Msg;
    String msg;
    List<ValE> ele = new ArrayList<>();

    public ValE() {}

    public ValE(String msg) {
        this.msg = msg;
    }

    public ValE asError() {
        type = ValType.Error;
        return this;
    }

    public ValE asWarning() {
        type = ValType.Warn;
        return this;
    }

    public ValE asRef() {
        type = ValType.Ref;
        return this;
    }

    public ValE asDoc() {
        type = ValType.Doc;
        return this;
    }

    public ValE add(ValE ele) {
        this.ele.add(ele);
        return this;
    }

    boolean ignore(String msg) {
        boolean ignored = false;
        if (msg.equals(this.msg)) {
            this.type = ValType.Ignored;
            ignored = true;
        }
        for (ValE e : ele)
            ignored = ignored || e.ignore(msg);
        return ignored;
    }

    public String getMsg() {
        return msg;
    }

    public List<ValE> getEle() {
        return ele;
    }

    public ValType getType() {
        return type;
    }
}
