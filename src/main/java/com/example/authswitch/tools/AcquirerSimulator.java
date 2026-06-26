package com.example.authswitch.tools;

import org.jpos.iso.ISOMsg;
import org.jpos.iso.channel.ASCIIChannel;
import org.jpos.iso.packager.ISO87APackager;

/**
 * A tiny acquirer/terminal simulator: connects to the ISO 8583 server, sends a
 * 0100 authorization request, and prints the 0110 response. This is the "live
 * switch" demo you can show alongside the REST API.
 *
 * Run (after the app is up):
 *   mvn -q compile exec:java -Dexec.mainClass=com.example.authswitch.tools.AcquirerSimulator
 * or with java once the jar/classes are built:
 *   java -cp target/classes:... com.example.authswitch.tools.AcquirerSimulator 127.0.0.1 10000
 */
public final class AcquirerSimulator {

    private AcquirerSimulator() {
    }

    public static void main(String[] args) throws Exception {
        String host = args.length > 0 ? args[0] : "127.0.0.1";
        int port = args.length > 1 ? Integer.parseInt(args[1]) : 10000;
        String pan = args.length > 2 ? args[2] : "4111111111111111";
        long amount = args.length > 3 ? Long.parseLong(args[3]) : 1000L; // $10.00

        ASCIIChannel channel = new ASCIIChannel(host, port, new ISO87APackager());
        channel.connect();

        ISOMsg m = new ISOMsg();
        m.setMTI("0100");
        m.set(2, pan);
        m.set(3, "000000");                 // processing code: purchase
        m.set(4, String.valueOf(amount));   // amount, minor units
        m.set(7, "0101120000");             // transmission date/time (MMDDhhmmss)
        m.set(11, "000001");                // STAN
        m.set(14, "3012");                  // expiry YYMM
        m.set(37, "000000000001");          // RRN
        m.set(41, "TERM0001");              // terminal id (8)
        m.set(42, "MERCHANT0000001");       // merchant id (15)
        m.set(49, "840");                   // currency USD

        System.out.println(">> sending 0100 pan=****" + pan.substring(pan.length() - 4)
                + " amount=" + amount);
        channel.send(m);

        ISOMsg r = channel.receive();
        System.out.println("<< received " + r.getMTI()
                + " responseCode(39)=" + r.getString(39)
                + (r.hasField(38) ? " authCode(38)=" + r.getString(38) : ""));

        channel.disconnect();
    }
}
