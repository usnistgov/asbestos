package gov.nist.asbestos.sharedObjects.test;

import gov.nist.asbestos.sharedObjects.GenericJSFactory;
import gov.nist.asbestos.sharedObjects.StringList;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class StringListTest {

    @Test
    void twoElement() {

        List<String> aList = Arrays.asList("one", "two");
        StringList myClass = new StringList();
        myClass.setValues(aList);
        String js;
        js = GenericJSFactory.convert(myClass);
        assertNotNull(js);
        StringList copy = GenericJSFactory.convert(js, StringList.class);
        assertNotNull(copy);
        assertEquals(2, copy.getValues().size());
    }
}
