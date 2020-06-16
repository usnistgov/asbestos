package gov.nist.asbestos.client.resolver;

import gov.nist.asbestos.simapi.tk.installation.Installation;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Date;
import java.util.Enumeration;

public class IdBuilder {
    private boolean override = false;

    public IdBuilder(boolean override) {
        this.override = override;
    }

    public String allocate(String defaultValue) {
        if (!override)
            return defaultValue;
        return Installation.dateAsIdentifier(new Date(), "1.2." + ip() + ".", ".");
    }

    private String ip() {
        Enumeration e = null;
        String ip = "127.0.0.1";
        try {
            e = NetworkInterface.getNetworkInterfaces();
        } catch (SocketException ex) {
            throw new Error(ex);
        }
        while(e.hasMoreElements())
        {
            NetworkInterface n = (NetworkInterface) e.nextElement();
            Enumeration ee = n.getInetAddresses();
            while (ee.hasMoreElements())
            {
                InetAddress i = (InetAddress) ee.nextElement();
                /*
                Ipv6 format contains colon characters which is not usable in composing an OID.
                Select the Ipv4 interface because it blends in with an OID format.
                 */
                if (i instanceof Inet4Address) {
                    String x = i.getHostAddress();
                    if (! x.equals("127.0.0.1") && ! containsAlpha(x))
                        ip = x;
                }
            }
        }
        return ip;
    }

    private static boolean containsAlpha(String s) {
        for (int i=0; i<s.length(); i++) {
            char c = s.charAt(i);
            if (isAlpha(c))
                return true;
        }
        return false;
    }

    private static boolean isAlpha(char c) {
        int type = Character.getType(c);
        return type == Character.LOWERCASE_LETTER || type == Character.UPPERCASE_LETTER;
    }
}
