package gov.nist.asbestos.mhd.transactionSupport;

import java.util.Scanner;

public class XmlTools {

    public static String deleteXMLInstruction(String in) {
        StringBuilder buf = new StringBuilder();
        Scanner scanner = new Scanner(in);
        while(scanner.hasNextLine()) {
            String line = scanner.nextLine();
            if (!line.startsWith("<?xml"))
                buf.append(line).append(("\n"));
        }
        scanner.close();
        return  buf.toString();
    }

    public static String deleteQueryExpression(String in) {
        StringBuilder buf = new StringBuilder();
        Scanner scanner = new Scanner(in);
        while(scanner.hasNextLine()) {
            String line = scanner.nextLine();
            if (!line.contains("<QueryExpression/>"))
                buf.append(line).append(("\n"));
        }
        scanner.close();
        return  buf.toString();
    }

}
