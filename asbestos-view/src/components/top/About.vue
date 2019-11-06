<template>
    <div class="left">
        <h1>About NIST FHIR<img src="../../assets/copyright.png"> Toolkit</h1>
        <a href="http://ihexds.nist.gov" target="_blank">NIST XDS and FHIR Toolkits Home Page</a>
        <br />
        <a href="https://github.com/usnistgov/asbestos" target="_blank">GitHub Open Source Repository</a>
        <ul>
            <li><a href="https://github.com/usnistgov/asbestos/wiki" target="_blank">Wiki</a></li>
            <li><a href="https://github.com/usnistgov/asbestos/releases" target="_blank">Downloads</a></li>
        </ul>

        <h2>Value Sets</h2>
        <b>Assert Response Code Types</b>
        <p>These codes are used in the test descriptions representing HTTP response codes</p>
        <ul>
            <li><a href="http://hl7.org/fhir/valueset-assert-response-code-types.html#expansion" target="_blank">Codes</a></li>
        </ul>

        <h2>NIST Disclaimer</h2>
        This software was developed by employees of the National Institute of
        Standards and Technology (NIST), an agency of the Federal Government and
        is being made available as a public service. Pursuant to title 17 United
        States Code Section 105, works of NIST employees are not subject to
        copyright protection in the United States. This software may be subject
        to foreign copyright. Permission in the United States and in foreign
        countries, to the extent that NIST may hold copyright, to use, copy,
        modify, create derivative works, and distribute this software and its
        documentation without fee is hereby granted on a non-exclusive basis,
        provided that this notice and disclaimer of warranty appears in all copies.
<br />
        <br />
        THE SOFTWARE IS PROVIDED 'AS IS' WITHOUT ANY WARRANTY OF ANY KIND,
        EITHER EXPRESSED, IMPLIED, OR STATUTORY, INCLUDING, BUT NOT LIMITED TO,
        ANY WARRANTY THAT THE SOFTWARE WILL CONFORM TO SPECIFICATIONS, ANY
        IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE,
        AND FREEDOM FROM INFRINGEMENT, AND ANY WARRANTY THAT THE DOCUMENTATION
        WILL CONFORM TO THE SOFTWARE, OR ANY WARRANTY THAT THE SOFTWARE WILL BE
        ERROR FREE. IN NO EVENT SHALL NIST BE LIABLE FOR ANY DAMAGES, INCLUDING,
        BUT NOT LIMITED TO, DIRECT, INDIRECT, SPECIAL OR CONSEQUENTIAL DAMAGES,
        ARISING OUT OF, RESULTING FROM, OR IN ANY WAY CONNECTED WITH THIS
        SOFTWARE, WHETHER OR NOT BASED UPON WARRANTY, CONTRACT, TORT, OR
        OTHERWISE, WHETHER OR NOT INJURY WAS SUSTAINED BY PERSONS OR PROPERTY OR
        OTHERWISE, AND WHETHER OR NOT LOSS WAS SUSTAINED FROM, OR AROSE OUT OF
        THE RESULTS OF, OR USE OF, THE SOFTWARE OR SERVICES PROVIDED HEREUNDER.

        <h2>Icons</h2>
        <div>Icons made by <a href="https://www.flaticon.com/authors/freepik" title="Freepik" target="_blank">Freepik</a> from <a href="https://www.flaticon.com/"             title="Flaticon" target="_blank">www.flaticon.com</a></div>
    </div>
</template>

<script>
    export default {
        created() {
            this.$store.state.base.sideBar = false
        },
        name: "About"
    }
</script>

<style scoped>

</style>
