package gov.nist.asbestos.asbestosProxy.channels.mhd.transactionSupport;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;


public class CodeTranslator {
    List<CodeType> codeTypes = new ArrayList<>();

    public CodeTranslator(String codesXmlFileContent) {
        parse(codesXmlFileContent);
    }

    private void parse(String stringXML) {
        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document document = builder.parse(new ByteArrayInputStream(stringXML.getBytes()));
        NodeList codeTypesNL = document.getElementsByTagName("CodeType");
        for (int i=0; i<codeTypesNL.getLength(); i++) {
            Node codeTypeNode = codeTypesNL.item(i);
            NamedNodeMap codeTypeNodeMap = codeTypeNode.getAttributes();
            Node nameItem = codeTypeNodeMap.getNamedItem("name");
            String name = nameItem.getNodeValue();
            Node classSchemeItem = codeTypeNodeMap.getNamedItem("classScheme");
            String classScheme = classSchemeItem.getNodeValue();
            CodeType codeType = new CodeType(name, classScheme);
            codeTypes.add(codeType);
            NodeList codeTypeNL = codeTypeNode.getChildNodes();
            for (int j=0; j<codeTypeNL.getLength(); j++) {
                Node codeNode =  codeTypeNL.item(j);

            }
        }



        def codes = new XmlSlurper().parseText(stringXML)

        codes.CodeType.each { codeTypeEle ->
            CodeType codeType = new CodeType(codeTypeEle)
            codeTypes << codeType
            codeTypeEle.Code.each { codeEle ->
                codeType.codes << new Code(codeEle)
            }
        }
    }

    public Code findCodeBySystem(String systemCodeType, String system, String code) {
        codeTypes.find { it.name == systemCodeType }.codes.find { it.system == system && it.code == code }
    }

    public Code findCodeByClassificationAndSystem(String classification, String system, String code) {
        def codeType = codeTypes.find { it.classScheme == classification }
        codeType.codes.find { it.system == system && it.code == code }
    }


    static final String CONFCODE = "confidentialityCode";
    static final String HCFTCODE = "healthcareFacilityTypeCode";
    static final String PRACCODE = "practiceSettingCode";
    static final String EVENTCODE = "eventCodeList";
    static final String FOLDERCODE = "folderCodeList";
    static final String TYPECODE = "typeCode";
    static final String CONTENTTYPECODE = "contentTypeCode";
    static final String CLASSCODE = "classCode";
    static final String FORMATCODE = "formatCode";

    String[] systemCodeTypes = {
            CONFCODE,
            HCFTCODE,
            PRACCODE,
            EVENTCODE,
            FOLDERCODE,
            TYPECODE,
            CONTENTTYPECODE,
            CLASSCODE,
            FORMATCODE
    };

}
