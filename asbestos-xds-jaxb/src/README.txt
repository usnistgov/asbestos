Resources contain the schema files for XDS.  To generate the JAXB java files run

    xjc .

in that directory.

It  generates the directories ihe, oasis, and org which hold the generated source code. Copy them into src/java.

This should only have to be repeated if the schema is updated.
