package gov.nist.asbestos.asbestosProxy.servlet;

import java.io.File;
import java.nio.file.Files;

public class Task {
    int index;
    String label;
    String description;
    String requestHeader;
    String requestBody;
    String responseHeader;
    String responseBody;

    public Task(File eventDir, String taskLabel) {
        description = read(eventDir, taskLabel, "description.txt");
        requestHeader = read(eventDir, taskLabel, "request_header.txt");
        requestBody = read(eventDir, taskLabel, "request_body.txt");
        responseHeader = read(eventDir, taskLabel, "response_header.txt");
        responseBody = read(eventDir, taskLabel, "response_body.txt");
    }

    public static String read(File theEvent, String theSection, String thePart) {
        File file = new File(new File(theEvent, theSection), thePart);
        if (!file.exists() || !file.canRead()) {
            String fileSt = file.toString();
            if (fileSt.endsWith(".txt")) {
                fileSt = fileSt.replace(".txt", ".bin");
                file = new File(fileSt);
            }
            if (!file.exists() || !file.canRead()) {
                return "";
            }
        }

        try {
            String content = new String(Files.readAllBytes(file.toPath()));
            content = content.replaceAll("<", "&lt;");
            return content;
        } catch (Exception e) {
            ;
        }
        return "";
    }

}
