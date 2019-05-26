package gov.nist.asbestos.toolkitApi.envSettings

import gov.nist.asbestos.simapi.tk.installation.Installation;
import groovy.transform.TypeChecked;
import org.apache.log4j.Logger;

@TypeChecked
 class EnvSetting {
	// SessionID ==> Environment Setting
	private static Map<String, EnvSetting> settings = new HashMap<String, EnvSetting>();
    static  final String DEFAULTSESSIONID = "DEFAULT";
    static  final String DEFAULTENVIRONMENTNAME = "default";
	String envName;
	File envDir;

	static Logger logger = Logger.getLogger(EnvSetting.class);

	static  EnvSetting getEnvSetting(String sessionId) {
        EnvSetting s = getEnvSettingForSession(sessionId);
        assert s : "No EnvSettings for sessionId ${sessionId}"
		return s;
	}

     String toString() {
        return String.format("ENV %s => %s", envName, envDir);
    }

    private static void addSetting(String sessionId, EnvSetting envSetting) {
	    settings.put(sessionId, envSetting);
        if (settings.keySet().size() == 3)
            logger.info("third setting");
    }

    static  EnvSetting getEnvSettingForSession(String sessionId) {
        EnvSetting s = settings.get(sessionId);
        if (s == null) {
            if (DEFAULTSESSIONID.equals(sessionId)) {
                installDefaultEnvironment();
                return settings.get(sessionId);
            } else
                return null;
        }
        return s;
    }

    static void installDefaultEnvironment() {
        File envFile = Installation.instance().defaultEnvironmentFile
        assert envFile : "Default Environment not configured - file " + envFile + " not found."
        new EnvSetting(DEFAULTSESSIONID, DEFAULTENVIRONMENTNAME, envFile);
    }

	 EnvSetting(String sessionId, String name, File dir) {
//		logger.info(sessionId + ": EnvSetting -  uses environment " + name + " ==> " + dir);
		addSetting(sessionId, new EnvSetting(name, dir));
	}

	 EnvSetting(String sessionId, String name) {
		File dir = Installation.instance().environmentFile(name);
//		logger.info("Session " + sessionId + " environment " + name + " ==> " + dir);
        addSetting(sessionId, new EnvSetting(name, dir));
	}

     EnvSetting(String envName) {
        this.envName = envName;
        this.envDir = Installation.instance().environmentFile(envName);
        validateEnvironment();
    }

	private EnvSetting(String name, File dir) {
		this.envName = name;
		this.envDir = dir;
        validateEnvironment();
	}

	 String getEnvName() {
		return envName;
	}

	 File getEnvDir() {
		return envDir;
	}

	 File getCodesFile() {
         assert envDir : "Environment ${envName} does not exist"
		File f = new File(envDir, "codes.xml");
		if (f.exists())
			return f;
		logger.warn("Codes file " + f + " does not exist");
		return null;
	}

    void validateEnvironment() {
        assert getCodesFile() : "Selected environment " + envName + " not valid - does not contain codes.xml file"
    }

}
