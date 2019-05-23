package gov.nist.asbestos.http.headers;

public class WhiteSpace {

    static String removeTrailing(String param) {
        if (param == null)
            return null;
        int len = param.length();
        for (; len > 0; len--) {
            if (!Character.isWhitespace(param.charAt(len - 1)))
                break;
        }
        return param.substring(0, len);
    }

    public static String removeLeading(String param)
    {
        if (param == null) {
            return null;
        }

        if(param.isEmpty()) {
            return "";
        }

        int arrayIndex = 0;
        while(true)
        {
            if (!Character.isWhitespace(param.charAt(arrayIndex++))) {
                break;
            }
        }
        return param.substring(arrayIndex-1);
    }
}
