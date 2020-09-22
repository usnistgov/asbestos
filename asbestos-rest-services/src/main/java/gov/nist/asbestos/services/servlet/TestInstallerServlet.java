package gov.nist.asbestos.services.servlet;

import gov.nist.asbestos.client.Base.EC;
import gov.nist.asbestos.client.log.SimStore;
import gov.nist.asbestos.http.operations.HttpGetter;
import gov.nist.asbestos.serviceproperties.ServiceProperties;
import gov.nist.asbestos.serviceproperties.ServicePropertiesEnum;
import gov.nist.asbestos.client.channel.ChannelConfig;
import gov.nist.asbestos.client.channel.ChannelConfigFactory;
import gov.nist.asbestos.simapi.tk.installation.Installation;
import gov.nist.toolkit.configDatatypes.server.SimulatorProperties;
import gov.nist.toolkit.toolkitApi.DocumentRegRep;
import gov.nist.toolkit.toolkitApi.SimulatorBuilder;
import gov.nist.toolkit.toolkitApi.ToolkitServiceException;
import gov.nist.toolkit.toolkitServicesCommon.SimConfig;
import gov.nist.toolkit.toolkitServicesCommon.resource.SimIdResource;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.http.HttpStatus;
import org.apache.log4j.Logger;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Optional;

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

        File lockFile = new File(externalCache, "FhirTestCollections.lock");
        if (!lockFile.exists()) {
            log.info("Updating Test Definitions and Assertions in External Cache");

            initializeTestCollections();
            initializeAssertionMap();

            log.info("Locking External Cache copy of Test Definitions and Assertions");
            try {
                lockFile.createNewFile();
            } catch (IOException e) {
                log.fatal("TestInstallerServlet - Cannot create FhirTestCollections.lock");
            }
        } else {
            log.info("Not updating Test Definitions and Assertions - External Cache copy is locked");
        }
        log.info("Updating Channels");
        initializeChannels();


        try {
            verifyCodesXml();
        } catch (Exception e) {
            log.fatal("TestInstallerServlet - codes verification failed - " + e.getMessage());
        }
    }

    private void verifyCodesXml() throws URISyntaxException, IOException {
        String url = ServiceProperties.getInstance().getProperty(ServicePropertiesEnum.XDS_TOOLKIT_BASE.toString()) + "/sim/codes/default";
        HttpGetter getter = new HttpGetter();
        getter.get(new URI(url), "text/xml");
        String xdsCodes = getter.getResponseText();

        File codesFile = Installation.instance().getCodesFile("default");
        String fhirCodes = new String(Files.readAllBytes(Paths.get(codesFile.toString())));

        if (xdsCodes.equals(fhirCodes)) {
            log.info("TestInstallerServlet - codes.xml checked - FHIR and XDS reference same version");
        } else {
            log.fatal("TestInstallerServlet - codes.xml checked - FHIR and XDS reference different versions");
        }
    }

    private void initializeChannels() {
        File externalChannels = new File(new File(externalCache, EC.CHANNELS_DIR), "default");
        externalChannels.mkdirs();
        File war = warHome();
        File internalChannels = new File(new File(war, "data"), "Channels");

        try {
            File[] channels = internalChannels.listFiles();
            if (channels != null) {
                for (File channel : channels) {
                    String name = channel.getName();
                    File target = new File(externalChannels, name);
                    if (! target.exists()) {
                        log.info(target + " does not exist - building it");
                        FileUtils.copyDirectoryToDirectory(channel, externalChannels);
                    } else {
                        log.info(target + " exists.");
                    }
                    log.info(String.format("Refreshing channel %s", name));
                    switch (name) {
                        case "default": {
                            configureDefaultChannel(externalChannels, name);
                            break;
                        }
                        case "xds": /* FALLTHROUGH */
                        case "selftest_comprehensive":
                        case "limited": {
                            configureXdsChannels(externalChannels, name);
                            break;
                        }
                    }
                }
            }
        } catch (IOException e) {
            log.error(ExceptionUtils.getStackTrace(e));
        }
    }

    private void configureXdsChannels(File externalChannels, String name) {
        Optional<String> xdsToolkitBase = ServiceProperties.getInstance().getProperty(ServicePropertiesEnum.XDS_TOOLKIT_BASE);
        if (xdsToolkitBase.isPresent()) {
            File configFile = getChannelConfigFile(externalChannels, name);
            ChannelConfig channelConfig = ChannelConfigFactory.load(configFile);
            String xdsSiteName = channelConfig.getXdsSiteName();
            if (xdsSiteName != null) {
                String simIdParts[] = xdsSiteName.split("__");
                SimIdResource simIdResource = new SimIdResource();
                simIdResource.setUser(simIdParts[0]);
                simIdResource.setId(simIdParts[1]);
                simIdResource.setActorType("rr");
                //
                SimConfig simConfig = null;
                SimulatorBuilder xdsSimApi = new SimulatorBuilder(xdsToolkitBase.get());
                try {
                    simConfig = xdsSimApi.get(simIdResource);
                } catch (ToolkitServiceException getSimEx) {
                    // Sim does not exist if 404
                    if (getSimEx.getCode() == HttpStatus.SC_NOT_FOUND) {
                        // Create the sim
                        try {
                            DocumentRegRep rr = xdsSimApi.createDocumentRegRep(simIdResource.getId(), simIdResource.getUser(), "default");
                            simConfig = rr.getConfig();
                        } catch (ToolkitServiceException createEx) {
                            log.error(createEx.toString());
                            log.error(String.format("Error: %s sim could not be created on %s", xdsSiteName, xdsToolkitBase));
                        }
                    } else {
                        log.error(String.format("Error: HTTP Status: %d. Unhandled exception %s", getSimEx.getCode(),  getSimEx.toString()));
                    }
                }
                if (simConfig != null) {
                    // Sim exists, check if configured properly according to the Rules of the "Predefined Channels" section in the Home page doc
                    boolean needsUpdate = false;
                    String pifConfigEleName = SimulatorProperties.VALIDATE_AGAINST_PATIENT_IDENTITY_FEED;
                    if (simConfig.getPropertyNames().contains(pifConfigEleName)) {
                        Boolean isPifEnabled = null;
                        if (simConfig.isBoolean(pifConfigEleName)) {
                            isPifEnabled = simConfig.asBoolean(pifConfigEleName);
                        }
                        if (isPifEnabled != null) {
                            if (isPifEnabled) {
                                // Rule 1 : This simulator must have Validate Against Patient Identity Feed unchecked as we do not send Patient Identity Feed messages to the simulator.
                                simConfig.setProperty(pifConfigEleName, false);
                                needsUpdate = true;
                            }
                        }
                    }
                    if (name.equals("limited")) {
                        String metadataLimitedConfigEleName = SimulatorProperties.METADATA_LIMITED;
                        if (simConfig.getPropertyNames().contains(metadataLimitedConfigEleName)) {
                            Boolean isMetaLimitedEnabled = null;
                            if (simConfig.isBoolean(metadataLimitedConfigEleName)) {
                                isMetaLimitedEnabled = simConfig.asBoolean(metadataLimitedConfigEleName);
                            }
                            if (isMetaLimitedEnabled != null) {
                                if (! isMetaLimitedEnabled) {
                                    // Rule 2 : This simulator must have Metadata Limited checked so validation is done using the rules for Metadata Limited messages.
                                    simConfig.setProperty(metadataLimitedConfigEleName, true);
                                    needsUpdate = true;
                                }
                            }
                        }
                    }
                    if (needsUpdate) {
                        log.info("Updating channel " + name);
                        try {
                            xdsSimApi.update(simConfig);
                        } catch (ToolkitServiceException updateEx) {
                            log.error(updateEx.toString());
                            log.error(String.format("Error: %s SimConfig could not be updated!", xdsSiteName));
                        }
                    } else {
                        log.info("Already up to date.");
                    }
                }
            }
        }
    }

    private void configureDefaultChannel(File externalChannels, String name) {
        log.info("Configure " + name + " channel");
        Optional<String> hapFhirBase = ServiceProperties.getInstance().getProperty(ServicePropertiesEnum.HAPI_FHIR_BASE);
        if (hapFhirBase.isPresent()) {
            File configFile = getChannelConfigFile(externalChannels, name);
            ChannelConfig channelConfig = ChannelConfigFactory.load(configFile);
            if (! hapFhirBase.get().equals(channelConfig.getFhirBase())) {
                log.info("Updating " + name + " channel.");
                channelConfig.setFhirBase(hapFhirBase.get());
                ChannelConfigFactory.store(channelConfig, configFile);
            } else {
                log.info("Already up to date.");
            }
        }
    }

    private File getChannelConfigFile(File externalChannels, String name) {
        return new File(new File(externalChannels, name), SimStore.CHANNEL_CONFIG_FILE);
    }

    private void initializeAssertionMap() {
        File externalAssertions = new File(externalCache, EC.TEST_ASSERTIONS_DIR);
        externalAssertions.mkdirs();

        File war = warHome();
        try {
            FileUtils.copyDirectory(new File(new File(war, "data"), "TestAssertions"), externalAssertions);
        } catch (IOException e) {
            log.error(ExceptionUtils.getStackTrace(e));
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
        File warMarkerFile;
        // String content = null;
        try {
            warMarkerFile = Paths.get(getClass().getResource("/war.txt").toURI()).toFile();
            // warMarkerFile is something like /home/bill/develop/asbestos/asbestos-war/target/asbestos-war/WEB-INF/classes/war.txt
            return warMarkerFile.getParentFile().getParentFile().getParentFile();
        } catch (Throwable t) {
            log.error(ExceptionUtils.getStackTrace(t));
            return null;
        }

    }


}
