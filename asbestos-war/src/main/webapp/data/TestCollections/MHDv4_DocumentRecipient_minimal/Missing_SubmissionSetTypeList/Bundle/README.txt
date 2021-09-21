Comments in the FHIR Bundle show up as
text": {
        "fhir_comments": []
       ...

So, this README exists to document the following issue:

    <entry>
        <fullUrl value="urn:uuid:1e404af3-077f-4bee-b7a6-a9be97e1aaa1"/>
        <resource>
            <DocumentReference>
                <!-- XDS does not seem to return the following meta element back when the DocRef was searched for.
                If this is present, then a TestScript with an Assert for a minimumId fails with a complaint that meta lastUpdated was not present in the DocRef (from the search result).
                -->
<!--                <meta>-->
<!--                    <lastUpdated value="2013-07-01T13:11:33Z"/>-->
<!--                </meta>-->
                <text>
