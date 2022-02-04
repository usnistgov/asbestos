package gov.nist.asbestos.client.Base;

import gov.nist.asbestos.client.client.Format;
import gov.nist.asbestos.serviceproperties.ServiceProperties;
import gov.nist.asbestos.serviceproperties.ServicePropertiesEnum;
import org.hl7.fhir.r4.model.ValueSet;

import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MhdValueSets {
    private File ec;
    private String environment;

    public MhdValueSets(File ec, String environment) {
        this.ec = ec;
        this.environment = environment;
    }

    public void build(EC ec) throws FileNotFoundException, JAXBException {
        File codesFile = ec.getCodesFile(environment);
        File dir = valueSetDir();
        dir.mkdirs();

        Map<String, ValueSet> valueSetMap = new CodesToValueSets(codesFile).run();
        for (String name : valueSetMap.keySet()) {
            ValueSet value = valueSetMap.get(name);
            value.setUrl(getUrl(name));
            String json = ParserBase.encode(value, Format.JSON);
            ec.writeToFile(new File(dir, name + ".json"), json);
        }
    }

    public String getUrl(String name) {
        File base = new File(ServiceProperties.getInstance().getPropertyOrThrow(ServicePropertiesEnum.FHIR_TOOLKIT_BASE));
        return base.toString() + "/valueset/" + environment + "/" + name;
    }

    public List<ValueSet> load() {
        List<ValueSet> valueSets = new ArrayList<>();

        File[] files = valueSetDir().listFiles();
        if (files == null || files.length == 0)
            return valueSets;

        for (File file : files) {
            ValueSet valueSet = (ValueSet) ParserBase.parse(file);
            valueSets.add(valueSet);
        }

        return valueSets;
    }

    public boolean needsBuilding() {
        if (!exists())
            return true;
        File[] files = valueSetDir().listFiles();
        if (files == null || files.length == 0)
            return true;
        return false;
    }

    public boolean exists() {
        return valueSetDir().exists();
    }

    public File valueSetDir() {
        return new File(new File(new File(ec, "environment"), environment), "MhdValueSets");
    }
}
