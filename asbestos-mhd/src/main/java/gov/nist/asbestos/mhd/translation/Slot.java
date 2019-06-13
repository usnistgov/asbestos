package gov.nist.asbestos.mhd.translation;

import oasis.names.tc.ebxml_regrep.xsd.rim._3.InternationalStringType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.LocalizedStringType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.SlotType1;

import java.util.List;

public class Slot {

    static String getSlotValue1(List<SlotType1> slots, String name) {
        for (SlotType1 s : slots) {
            if (s.getName().equals(name)) {
                List<String> values = s.getValueList().getValue();
                if (values.isEmpty())
                    return null;
                return values.get(0);
            }
        }
        return null;
    }

    public static String getValue(InternationalStringType ist) {
        List<LocalizedStringType> lsts = ist.getLocalizedString();
        if (lsts.isEmpty())
            return null;
        return lsts.get(0).getValue();
    }

    static SlotType1 getSlot(String name, String value) {
        SlotType1 slot
    }
}
