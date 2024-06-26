package gov.nist.asbestos.utilities;

public class RegistryResponseExtractor {

    public static String extractRegistryResponse(String in) {
        int start = in.indexOf("RegistryResponse");
        if (start == -1)
            return null;
        while (in.charAt(start) != '<') start--;
        int end = in.indexOf("RegistryResponse", start+20);
        if (end == -1) {
            // no formal end - must have been successful - settle for />
            end = in.indexOf("/>", start + 10);
            if (end == -1)
                return null;
            return in.substring(start, end+2);
        }
        while(in.charAt(end) != '>') end++;
        return in.substring(start, end+1);
    }

}
