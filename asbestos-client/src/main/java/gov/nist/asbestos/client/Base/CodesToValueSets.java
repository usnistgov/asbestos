package gov.nist.asbestos.client.Base;

import gov.nist.asbestos.asbestorCodesJaxb.Code;
import gov.nist.asbestos.asbestorCodesJaxb.CodeType;
import gov.nist.asbestos.asbestorCodesJaxb.Codes;
import org.hl7.fhir.r4.model.ValueSet;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CodesToValueSets {
    private Codes codes;

    CodesToValueSets(InputStream codesStream) throws JAXBException {
        JAXBContext jaxbContext = JAXBContext.newInstance(Codes.class);
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        codes = (Codes) unmarshaller.unmarshal(codesStream);
    }

    public CodesToValueSets(File codesXmlFile) throws FileNotFoundException, JAXBException {
        this(new FileInputStream(codesXmlFile));
    }

    public Map<String, ValueSet> run() {
        Map<String, ValueSet> valueSets = new HashMap<>();

        for (CodeType codeType : codes.getCodeType()) {
            String codeTypeName = codeType.getName();
            ValueSet valueSet = new ValueSet();
            valueSet.setTitle(codeTypeName);
            valueSets.put(codeTypeName, valueSet);

            // system => concept collection (codes)
            Map<String, ValueSet.ConceptSetComponent> conceptSets = new HashMap<>();

            for (Code code : codeType.getCode()) {
                String codeStr = code.getCode();
                String systemStr = code.getSystem();
                String display = code.getDisplay();

                ValueSet.ConceptSetComponent csComp;
                if (conceptSets.containsKey(systemStr)) {
                    csComp = conceptSets.get(systemStr);
                } else {
                    csComp = valueSet.getCompose().addInclude();
                    csComp.setSystem(systemStr);
                    conceptSets.put(systemStr, csComp);
                }

                ValueSet.ConceptReferenceComponent concept = csComp.addConcept();
                concept.setCode(codeStr);
                concept.setDisplay(display);
            }


        }

        return valueSets;
    }

}
