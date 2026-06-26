package com.example.authswitch.iso;

import com.example.authswitch.service.AuthorizationService;
import jakarta.annotation.PreDestroy;
import org.jpos.iso.ISOPackager;
import org.jpos.iso.ISOServer;
import org.jpos.iso.ServerChannel;
import org.jpos.iso.channel.ASCIIChannel;
import org.jpos.iso.packager.ISO87APackager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Starts a jPOS ISO 8583 TCP server once the app is up. This is the "switch"
 * interface a real acquirer connects to: it speaks length-prefixed ASCII ISO
 * messages on a socket, unlike the REST endpoint which speaks JSON over HTTP.
 */
@Component
public class IsoServer {

    private static final Logger log = LoggerFactory.getLogger(IsoServer.class);

    private final AuthorizationService authorizationService;
    private final IsoMessageService isoMessageService;
    private final boolean enabled;
    private final int port;

    private ISOServer server;

    public IsoServer(AuthorizationService authorizationService,
                     IsoMessageService isoMessageService,
                     @Value("${iso.server.enabled:true}") boolean enabled,
                     @Value("${iso.server.port:10000}") int port) {
        this.authorizationService = authorizationService;
        this.isoMessageService = isoMessageService;
        this.enabled = enabled;
        this.port = port;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void start() {
        if (!enabled) {
            log.info("ISO server disabled (iso.server.enabled=false)");
            return;
        }
        ISOPackager packager = new ISO87APackager();
        ServerChannel channel = new ASCIIChannel(packager);
        server = new ISOServer(port, channel, null);
        server.addISORequestListener(
                new AuthorizationRequestListener(authorizationService, isoMessageService));

        Thread t = new Thread(server, "iso-server");
        t.setDaemon(true);
        t.start();
        log.info("ISO 8583 server listening on port {}", port);
    }

    @PreDestroy
    public void stop() {
        if (server != null) {
            server.shutdown();
            log.info("ISO 8583 server stopped");
        }
    }
}
