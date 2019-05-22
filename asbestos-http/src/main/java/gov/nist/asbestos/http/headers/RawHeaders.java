package gov.nist.asbestos.http.headers

class RawHeaders {
    String uriLine  // GET|POST path [queryString]
    Map<String, List<String>> headers = [:]
    List<String> names = []

    RawHeaders(Enumeration names, Map<String, Enumeration> headers) {
        while (names.hasMoreElements()) {
            String name = names.nextElement()
            this.names << name
            generateNamesAsList(name, headers.get(name))
        }
    }

    RawHeaders() {

    }

    void addNames(Enumeration names) {
        while (names.hasMoreElements()) {
            String name = names.nextElement()
            this.names << name
        }
    }

    void addHeaders(String name, Enumeration headersEnum) {
        List<String> values = []
        while(headersEnum.hasMoreElements()) {
            String val = (String) headersEnum.nextElement()
            values << val
        }
        headers[name] = values
    }

    RawHeaders(Map<String, List<String>> headers) {
        this.headers = headers
        names = headers.keySet() as List<String>
    }

    static private Map<String, List<String>> generateHeadersAsList(String name, Enumeration<String> e) {
        List<String> lst = []
        while (e.hasMoreElements())
            lst << e.nextElement()
        lst
    }

//    List<String> headersAsList(String name) {
//        if (!headers) {
//            headers = generateHeadersAsList(name)
//            names = generateNamesAsList()
//        }
//        headers
//    }

//    List<String> namesAsList() {
//        if (!headers) {
//            headers = generateHeadersAsList(name)
//            names = generateNamesAsList()
//        }
//        names
//    }

    private List<String> generateNamesAsList() {
        List<String> lst = []

        while(names.hasMoreElements())
            lst << names.nextElement()

        lst
    }
}
