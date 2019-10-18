package gov.nist.asbestos.asbestosProxy.servlet;

import gov.nist.asbestos.client.Base.EC;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.log4j.Logger;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

public class TestInstallerServlet  extends HttpServlet {
    private static Logger log = Logger.getLogger(TestInstallerServlet.class);
    private File externalCache = null;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        Object ec = config.getServletContext().getAttribute("ExternalCache");
        if (ec != null) {
            log.info("TestInstallerServlet - Got External Cache from ProxyServlet");
            externalCache = new File((String) ec);
        } else {
            log.fatal("TestInstallerServlet - Proxy not started");
            return;
        }

        File lockFile = new File(externalCache, "FhirTestCollectionsLock");
        if (!lockFile.exists()) {
            log.info("Updating Test Definitions in External Cache");

            initializeTestCollections();

            log.info("Locking External Cache copy of Test Definitions");
            try {
                lockFile.createNewFile();
            } catch (IOException e) {
                log.fatal("TestInstallerServlet - Cannot create FhirTestCollectionsLock");
            }
        } else {
            log.info("Not updating Test Definitions - External Cache copy is locked");
        }

    }

    private void initializeTestCollections() {
        File externalCollections = new File(externalCache, EC.TEST_COLLECTIONS_DIR);
        externalCollections.mkdirs();

        File war = warHome();
        try {
            FileUtils.copyDirectory(new File(new File(war, "data"), "TestCollections"), externalCollections);
        } catch (IOException e) {
            log.error(ExceptionUtils.getStackTrace(e));
        }
    }

    private File warHome() {
        File warMarkerFile = null;
        // String content = null;
        try {
            warMarkerFile = Paths.get(getClass().getResource("/war.txt").toURI()).toFile();
            // content = new String ( Files.readAllBytes( Paths.get(warMarkerFile.toString()) ) );
        } catch (Throwable t) {
            log.error(ExceptionUtils.getStackTrace(t));
        }

        // warMarkerFile is something like /home/bill/develop/asbestos/asbestos-war/target/asbestos-war/WEB-INF/classes/war.txt
        return warMarkerFile.getParentFile().getParentFile().getParentFile();
    }


}
