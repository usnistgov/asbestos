package gov.nist.asbestos.client.events;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Reader {

    public static  String read(File theEvent, String theSection, String thePart) {
        File file = new File(new File(theEvent, theSection), thePart);
        if (!file.exists() || !file.canRead()) {
            String fileSt = file.toString();
            if (fileSt.endsWith(".txt")) {
                fileSt = fileSt.replace(".txt", ".bin");
                file = new File(fileSt);
            }
            if (!file.exists() || !file.canRead()) {
                return "";
            }
        }

        try {
            String content = new String(Files.readAllBytes(file.toPath()));
           // content = content.replaceAll("<", "&lt;");
            return content;
        } catch (Exception e) {
            ;
        }
        return "";
    }

    public static List<String> dirListingAsStringList(File dir) {
        List<String> contents = new ArrayList<>();

        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (!file.isDirectory()) continue;
                if (file.getName().startsWith(".")) continue;
                if (file.getName().startsWith("_")) continue;
                contents.add(file.getName());
            }
            contents = contents.stream().sorted().collect(Collectors.toList());
        }

        return contents;
    }


}
