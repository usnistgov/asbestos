package gov.nist.asbestos.client.Base;

import org.apache.commons.io.IOUtils;

import java.io.*;

public class DocumentCache {
    public EC ec;

    static private class Syncable {
        int i;
    }

    private static final Syncable syncer = new Syncable();

    private final File indexFile;

    public DocumentCache(EC ec) {
        this.ec = ec;
        ec.getDocumentCache().mkdirs();
        indexFile = new File(ec.getDocumentCache(), "index.txt");
    }

    private String newDocumentId() {
        FileOutputStream os = null;
        FileInputStream is = null;
        String index = "1";
        synchronized (syncer) {
            try {
                if (indexFile.exists()) {
                    is = new FileInputStream(indexFile);
                    index = IOUtils.toString(is, "UTF-8");
                }
                int indexInt = Integer.parseInt(index);
                indexInt++;
                String outIndex = String.valueOf(indexInt);
                os = new FileOutputStream(indexFile);
                IOUtils.write(outIndex, os, "UTF-8");
                return index;
            } catch (Exception e) {
                throw new Error("Document Cache error", e);
            } finally {
                try {
                    if (os != null)
                        os.close();
                    if (is != null)
                        is.close();
                } catch (IOException e) {
                    //  oops
                }
            }
        }
    }

    public String putDocumentCache(byte[] content, String mimeType) {
        String id = newDocumentId();
        File contentFile = new File(ec.getDocumentCache(), id + ".bytes");
        File typeFile = new File(ec.getDocumentCache(), id + ".type");
        FileOutputStream outputStream = null;
        try {
            outputStream = new FileOutputStream(contentFile.toString());
            outputStream.write(content);
            outputStream.close();
            outputStream = null;

            outputStream = new FileOutputStream(typeFile.toString());
            outputStream.write(mimeType.getBytes());
            outputStream.close();
            outputStream = null;
        } catch (Exception e) {
            throw new Error("Cannot write to Document Cache", e);
        } finally {
            try {
                if (outputStream != null)
                    outputStream.close();
            } catch (IOException e) {
                //  oops
            }
        }
        return id;
    }

    public byte[] getDocumentFromCache(String id) {
        File file = new File(ec.getDocumentCache(), id + ".bytes");
        FileInputStream is = null;
        try {
            is = new FileInputStream(file);
            return IOUtils.toByteArray(is);
        } catch (Exception e) {
            throw new Error("Cannot read from Document Cache", e);
        } finally {
            try {
                if (is != null)
                    is.close();
            } catch (IOException e) {
                // oops
            }
        }
    }

    public String getDocumentTypeFromCache(String id) {
        File typeFile = new File(ec.getDocumentCache(), id + ".type");
        FileInputStream is = null;
        try {
            is = new FileInputStream(typeFile);
            return IOUtils.toString(is, "UTF-8");
        } catch (Exception e) {
            throw new Error("Cannot read from Document Cache", e);
        } finally {
            try {
                if (is != null)
                    is.close();
            } catch (IOException e) {
                // oops
            }
        }
    }

    public void clean() {
        File cache = ec.getDocumentCache();
        File[] files = cache.listFiles();
        if (files != null) {
            for (File file : files) {
                file.delete();
            }
        }
    }
}
