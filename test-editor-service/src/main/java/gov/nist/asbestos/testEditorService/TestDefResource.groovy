package gov.nist.asbestos.testEditorService

import groovy.transform.TypeChecked;

import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@TypeChecked
@Path("TestDef")
class TestDefResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{qualifier}/{id}")
    String getTestDefResource(@PathParam("qualifier") String qualifier, @PathParam("id") String id) {
        return getFile(qualifier, id).text
    }

    static File getFile(String qualifier, String id) {
        List<String> qEle = qualifier.split("\\.") as List<String>
        File file = Config.getTestDefDir()
        qEle.each {String q ->
            file = new File(file, q)
        }
        return new File(file, id + '.json')
    }
}
