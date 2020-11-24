package gov.nist.asbestos.simapi.tk.installation;


import gov.nist.asbestos.simapi.simCommon.TestSession;

import java.io.File;

import java.util.*;

public class Installation {
    private static Installation me = null;
    private File externalCache;
    private String servletContextName = "http";
    private PropertyServiceManager propertyServiceManager = new PropertyServiceManager();
    private File defaultEnvironmentFile;

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
            throw new RuntimeException("External Cache error - " + describeExternalCache(externalCache));
    }

    private static String describeExternalCache(File externalCache) {
        StringBuilder buf = new StringBuilder()
                .append("ExternalCache: location=")
                .append((externalCache == null) ? "null" : externalCache.getPath());
        if (externalCache != null && externalCache.exists()) {
            buf.append(" exists");
            if (!externalCache.isDirectory())
                buf.append(" not");
            else
                buf.append(" is");
            buf.append(" a directory");
            if (externalCache.canWrite())
                buf.append(" can");
            else
                buf.append(" can not");
            buf.append(" write");
        }
        return buf.toString();
    }

    public static Installation instance() {
        if (me == null) {
            me = new Installation();
        }
        return me;
    }

    private Installation() {
    }

    public String toString() {
      return "External Cache is " + externalCache().toString();
    }

    public File getCodesFile(String environmentName) {
        return new File(new File(new File(externalCache, "environment"), environmentName), "codes.xml");
    }

//    private File simDbFile()  {
//        return new File(externalCache(), "fsimdb");
//    }

    /**
     *
     * @param date
     * @param prefix
     * @param connector
     * @param isPadded If True, returns a date time components with a leading zero for a single-digit integer. Use padding where sorting is desired. If isPadded is False, there is no padding used, which produces an OID-friendly format.
     * @return
     */
    public static String dateAsIdentifier(Date date, String prefix, String connector, boolean isPadded) {
        Calendar c  = Calendar.getInstance();
        c.setTime(date);

        String year = Integer.toString(c.get(Calendar.YEAR));
        String month = Integer.toString(c.get(Calendar.MONTH) + 1);
        if (month.length() == 1 && isPadded)
            month = "0" + month;
        String day = Integer.toString(c.get(Calendar.DAY_OF_MONTH));
        if (day.length() == 1  && isPadded)
            day = "0" + day;
        String hour = Integer.toString(c.get(Calendar.HOUR_OF_DAY));
        if (hour.length() == 1 && isPadded)
            hour = "0" + hour;
        String minute = Integer.toString(c.get(Calendar.MINUTE));
        if (minute.length() == 1 && isPadded)
            minute = "0" + minute;
        String second = Integer.toString(c.get(Calendar.SECOND));
        if (second.length() == 1 && isPadded)
            second = "0" + second;
        String mili = Integer.toString(c.get(Calendar.MILLISECOND));
        if (mili.length() == 2 && isPadded)
            mili = "0" + mili;
        else if (mili.length() == 1 && isPadded)
            mili = "00" + mili;
        String dot = connector;

        String val =
                prefix +
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

    public static String asFilenameBase(Date date) {
        return dateAsIdentifier(date, "", "_", true);
    }

    private File actorsDir() {
        return new File(externalCache(), File.separator + "actors");
    }

    public File actorsDir(TestSession testSession) {
        Objects.requireNonNull(testSession);
        File f = new File(actorsDir(), testSession.getValue());
        f.mkdirs();
        return f;
    }

//    private List<TestSession> getTestSessions() {
//        Set<TestSession> ts = new HashSet<>();
//
//        ts.addAll(findTestSessions(simDbFile()));
//        ts.addAll(findTestSessions(actorsDir()));
//
//        return new ArrayList<>(ts);
//    }

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

//    public boolean testSessionExists(TestSession testSession) {
//        return getTestSessions().contains(testSession);
//    }

    public String getServletContextName() {
        return servletContextName;
    }
}
