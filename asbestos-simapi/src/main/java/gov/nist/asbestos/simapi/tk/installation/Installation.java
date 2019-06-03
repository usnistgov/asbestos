package gov.nist.asbestos.simapi.tk.installation;


import gov.nist.asbestos.simapi.simCommon.TestSession;

import java.io.File;

import java.util.*;

public class Installation {
    private static Installation me = null;
    private File externalCache;
    String servletContextName = "http";
    private PropertyServiceManager propertyServiceManager = new PropertyServiceManager();
    private File defaultEnvironmentFile;
    String toolkitBaseUrl = "http://localhost:8080/xdstools";

    public File externalCache()  {
        Objects.requireNonNull(externalCache);
        if (!externalCache.exists())
            throw new RuntimeException("External Cache does not exist - " + externalCache);
        return externalCache;
    }

    public void setExternalCache(File externalCache) {
        this.externalCache = externalCache;
        validateExternalCache(externalCache);
        defaultEnvironmentFile = new File(externalCache.toString() + "/environment/default");
    }

    public static void validateExternalCache(File externalCache) {
        if (externalCache == null || !externalCache.exists() || !externalCache.isDirectory()|| !externalCache.canWrite())
            throw new RuntimeException("External Cache - " + externalCache + " is invalid");
    }

    public File fsimDbFile()  {
        File f = new File(externalCache(), "fsimDb");
        f.mkdirs();
        return f;
    }

    public static Installation instance() {
        if (me == null) {
            me = new Installation();
        }
        return me;
    }

    private Installation() {
    }

    public PropertyServiceManager propertyServiceManager() {
        return propertyServiceManager;
    }

    public String toString() {
      return "External Cache is " + externalCache().toString();
    }

    public File fhirSimDbFile(TestSession testSession) {
        return simDbFile(testSession);
    }

    public File simDbFile(TestSession testSession) {
        Objects.requireNonNull(testSession);
        return new File(simDbFile(), testSession.getValue());
    }

    public File simDbFile()  {
        return new File(externalCache(), "fsimdb");
    }

    public File environmentDbFile()  {
        return new File(externalCache(), "environment");
    }

    public File environmentFile(String environment)  {
        return new File(environmentDbFile(), environment);
    }

    public boolean environmentExists(String environment)  {
        return environmentFile(environment).exists();
    }

    public static String asFilenameBase(Date date) {
        Calendar c  = Calendar.getInstance();
        c.setTime(date);

        String year = Integer.toString(c.get(Calendar.YEAR));
        String month = Integer.toString(c.get(Calendar.MONTH) + 1);
        if (month.length() == 1)
            month = "0" + month;
        String day = Integer.toString(c.get(Calendar.DAY_OF_MONTH));
        if (day.length() == 1 )
            day = "0" + day;
        String hour = Integer.toString(c.get(Calendar.HOUR_OF_DAY));
        if (hour.length() == 1)
            hour = "0" + hour;
        String minute = Integer.toString(c.get(Calendar.MINUTE));
        if (minute.length() == 1)
            minute = "0" + minute;
        String second = Integer.toString(c.get(Calendar.SECOND));
        if (second.length() == 1)
            second = "0" + second;
        String mili = Integer.toString(c.get(Calendar.MILLISECOND));
        if (mili.length() == 2)
            mili = "0" + mili;
        else if (mili.length() == 1)
            mili = "00" + mili;

        String dot = "_";

        String val =
                year +
                        dot +
                        month +
                        dot +
                        day +
                        dot +
                        hour +
                        dot +
                        minute +
                        dot +
                        second +
                        dot +
                        mili
        ;
        return val;
    }

    public File actorsDir() {
        return new File(externalCache(), File.separator + "actors");
    }

    public File actorsDir(TestSession testSession) {
        Objects.requireNonNull(testSession);
        File f = new File(actorsDir(), testSession.getValue());
        f.mkdirs();
        return f;
    }

    public List<TestSession> getTestSessions() {
        Set<TestSession> ts = new HashSet<>();

        ts.addAll(findTestSessions(simDbFile()));
        ts.addAll(findTestSessions(actorsDir()));

        return new ArrayList<>(ts);
    }

    private List<TestSession> findTestSessions(File dir) {
        List<TestSession> ts = new ArrayList<>();
        if (dir.exists()) {
            for (File tlFile : dir.listFiles()) {
                if (tlFile.isDirectory() && !tlFile.getName().startsWith("."))
                    ts.add(new TestSession(tlFile.getName()));
            }
        }
        return ts;
    }

    public boolean testSessionExists(TestSession testSession) {
        return getTestSessions().contains(testSession);
    }

    public String getServletContextName() {
        return servletContextName;
    }
}
