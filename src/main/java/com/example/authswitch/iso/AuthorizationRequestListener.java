package com.example.authswitch.iso;

import com.example.authswitch.api.dto.AuthorizationRequest;
import com.example.authswitch.api.dto.AuthorizationResponse;
import com.example.authswitch.service.AuthorizationService;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISORequestListener;
import org.jpos.iso.ISOSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles each ISO 8583 message that arrives on the TCP server. jPOS calls
 * {@link #process} per request; we map it to our engine and send back the 0110.
 */
public class AuthorizationRequestListener implements ISORequestListener {

    private static final Logger log = LoggerFactory.getLogger(AuthorizationRequestListener.class);

    private final AuthorizationService authorizationService;
    private final IsoMessageService isoMessageService;

    public AuthorizationRequestListener(AuthorizationService authorizationService,
                                        IsoMessageService isoMessageService) {
        this.authorizationService = authorizationService;
        this.isoMessageService = isoMessageService;
    }

    @Override
    public boolean process(ISOSource source, ISOMsg m) {
        try {
            String mti = m.getMTI();
            if (!"0100".equals(mti)) {
                log.warn("Ignoring unsupported MTI {}", mti);
                return false;
            }
            AuthorizationRequest req = isoMessageService.toRequest(m);
            AuthorizationResponse resp = authorizationService.authorize(req);
            ISOMsg reply = isoMessageService.toResponse(m, resp);
            source.send(reply);
            log.info("ISO auth stan={} rrn={} -> {} ({}ms)",
                    req.getStan(), req.getRrn(), resp.getResponseCode(), resp.getLatencyMs());
            return true;
        } catch (Exception e) {
            log.error("Failed to process ISO message", e);
            return false;
        }
    }
}
