package gov.nist.asbestos.http.support;

import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 *
 */
class MultipartParser {
    static Multipart parse(String header, String body) {
        Multipart multipart = new Multipart();
        multipart.startPartId = parseStart(header);

        Part part = new Part();
        body = body.trim();
        StringBuilder output = new StringBuilder();
        StringBuilder buf = new StringBuilder();
        boolean xmlMode = true;
        StringTokenizer st = new StringTokenizer(body, "\n");

        while( st.hasMoreTokens() ) {
            String line = st.nextToken();
            line = line.trim();
            if (xmlMode) {
                if (line.startsWith("--")) {
                    xmlMode = false;

                    // end of body - input contains XML
                    if (buf.length() > 0) {
                        part.body = buf.toString();
                        multipart.parts.add(part);
                        part = new Part();
                    }

                    buf = new StringBuilder();

                    //output.append(line).append('\n')
                } else {
                    buf.append(line).append("\n");
                }
            } else {
                output.append(line).append("\n");
                if (line.equals("")) {
                    xmlMode = true;
                    // end of header - output contains header
                    part.header = output.toString();
                    part.id = parsePartId(part.header);
                    output = new StringBuilder();
                }
            }
        }
        return multipart;
    }

    static String parsePartId(String partHeader) {
        String id = null;
        StringTokenizer st = new StringTokenizer(partHeader, "\n");

        while (st.hasMoreTokens()) {
            String line = st.nextToken();
            if (line.toLowerCase().startsWith("content-id")) {
                int colonI = line.indexOf(":");
                if (colonI > -1) {
                    String value = line.substring(colonI+1).trim();
                    if (value.startsWith("<") && value.endsWith(">")) {
                        id = value.substring(1, value.length()-1);
                    }
                }
            }
        }

        return id;
    }

    static String parseStart(String messageHeader) {
        String startId = null;
        StringTokenizer st = new StringTokenizer(messageHeader, "\n");
        while (st.hasMoreTokens()) {
            String line = st.nextToken();
            if (line.toLowerCase().startsWith("content-type")) {
                int colonI = line.indexOf(':');
                if (colonI > -1) {
                    String value = line.substring(colonI+1).trim();
                    Map<String, String> params = parseHeaderParams(value);
                    if (params.containsKey("start")) {
                        if (params.get("start").startsWith("\""))
                            startId = trim(params.get("start"), "\"<", ">\"");
                        else
                            startId = trim(params.get("start"), "<", ">");
                    }
                }
            }
        }
        return startId;
    }

    private static Map<String, String> parseHeaderParams(String header) {
        // Map<String, String> params = new HashMap<>();
        List<String> parts = Arrays.asList(header.split(";"));
        parts = parts.stream()
                .map(String::trim)
                .collect(Collectors.toList());

        Map<String, String> params = parts.stream()
                .filter(x -> x.contains("="))
                .map(string -> string.split("=", 2))
                .filter(array -> !StringUtils.isEmpty(array[0]) && !StringUtils.isEmpty(array[1]))
                .collect(Collectors.toMap(array -> array[0].toLowerCase(), array -> array[1]));
        return params;
    }

    private static String trim(String input, String left, String right) {
        Objects.requireNonNull(input);
        Objects.requireNonNull(left);
        Objects.requireNonNull(right);
        if (input.startsWith(left) && input.endsWith(right))
            return input.substring(left.length(), input.length() - right.length());
        return input;
    }
}
