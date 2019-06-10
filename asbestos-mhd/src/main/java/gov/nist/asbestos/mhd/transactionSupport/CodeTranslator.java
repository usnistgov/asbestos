package gov.nist.asbestos.mhd.transactionSupport;

import gov.nist.asbestos.asbestorCodesJaxb.*;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Objects;
import java.util.Optional;

public class CodeTranslator {
    private Codes codes;

    CodeTranslator(InputStream codesStream) throws JAXBException {
            JAXBContext jaxbContext = JAXBContext.newInstance(Codes.class);
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            codes = (Codes) unmarshaller.unmarshal(codesStream);
    }

    CodeTranslator(File codesXmlFile) throws FileNotFoundException, JAXBException {
        this(new FileInputStream(codesXmlFile));
    }

    public Optional<Code> findCodeBySystem(String theName, String theSystem, String theCode) {
        Objects.requireNonNull(theName);
        Objects.requireNonNull(theSystem);
        Objects.requireNonNull(theCode);
        Optional<CodeType> codeType = codes.getCodeType().stream()
                .filter(codetype -> theName.equals(codetype.getName()))
                .findFirst();
        return codeType.flatMap(codeType1 -> codeType1.getCode().stream()
                .filter(code -> code.getSystem().equals(theSystem) && code.getCode().equals(theCode))
                .findFirst());
    }

    public Optional<Code> findCodeByClassificationAndSystem(String theClassification, String theSystem, String theCode) {
        Objects.requireNonNull(theClassification);
        Objects.requireNonNull(theSystem);
        Objects.requireNonNull(theCode);
        Optional<CodeType> codeType = codes.getCodeType().stream()
                .filter(codetype -> theClassification.equals(codetype.getClassScheme()))
                .findFirst();
        return codeType.flatMap(codeType1 -> codeType1.getCode().stream()
        .filter(code -> code.getSystem().equals(theSystem) && code.getCode().equals(theCode))
        .findFirst());
    }

    public static final String CONFCODE = "urn:uuid:f4f85eac-e6cb-4883-b524-f2705394840f";
    public static final String HCFTCODE = "urn:uuid:f33fb8ac-18af-42cc-ae0e-ed0b0bdb91e1";
    public static final String PRACCODE = "urn:uuid:cccf5598-8b07-4b77-a05e-ae952c785ead";
    public static final String EVENTCODE = "urn:uuid:2c6b8cb7-8b2a-4051-b291-b1ae6a575ef4";
    public static final String FOLDERCODE = "urn:uuid:1ba97051-7806-41a8-a48b-8fce7af683c5";
    public static final String TYPECODE = "urn:uuid:f0306f51-975f-434e-a61c-c59651d33983";
    public static final String CONTENTTYPECODE = "urn:uuid:aa543740-bdda-424e-8c96-df4873be8500";
    public static final String CLASSCODE = "urn:uuid:41a5887f-8865-4c09-adf7-e362475b143a";
    public static final String FORMATCODE = "urn:uuid:a09d5840-386c-46f2-b5ad-9c3699a4309d";

    // other non-codes
    public static final String DE_UNIQUEID = "urn:uuid:2e82c1f6-a085-4c72-9da3-8640a32e42ab";
    public static final String SS_UNIQUEID = "urn:uuid:96fdda7c-d067-4183-912e-bf5ee74998a8";
    public static final String SS_SOURCEID = "urn:uuid:554ac39e-e3fe-47fe-b233-965d2a147832";
    public static final String DE_PID = "urn:uuid:58a6f841-87b3-4a3e-92fd-a8ffeff98427";
    public static final String SS_PID = "urn:uuid:6b5aea1a-874d-4603-a4bc-96a0a7b38446";

    static final String CONFCODENAME = "confidentialityCode";
    static final String HCFTCODENAME = "healthcareFacilityTypeCode";
    static final String PRACCODENAME = "practiceSettingCode";
    static final String EVENTCODENAME = "eventCodeList";
    static final String FOLDERCODENAME = "folderCodeList";
    static final String TYPECODENAME = "typeCode";
    static final String CONTENTTYPECODENAME = "contentTypeCode";
    static final String CLASSCODENAME = "classCode";
    static final String FORMATCODENAME = "formatCode";

    String[] systemCodeTypes = {
            CONFCODENAME,
            HCFTCODENAME,
            PRACCODENAME,
            EVENTCODENAME,
            FOLDERCODENAME,
            TYPECODENAME,
            CONTENTTYPECODENAME,
            CLASSCODENAME,
            FORMATCODENAME
    };

}
