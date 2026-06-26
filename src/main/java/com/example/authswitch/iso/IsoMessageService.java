package com.example.authswitch.iso;

import com.example.authswitch.api.dto.AuthorizationRequest;
import com.example.authswitch.api.dto.AuthorizationResponse;
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.springframework.stereotype.Service;

/**
 * Translates between ISO 8583 messages and our plain request/response objects, so
 * the REST path and the ISO/TCP path feed the exact same authorization engine.
 *
 * Field reference (ISO 8583 "data elements", DE):
 *   DE2  PAN          DE3  processing code   DE4  amount (minor units)
 *   DE11 STAN         DE14 expiry (YYMM)     DE37 RRN
 *   DE38 auth code    DE39 response code     DE49 currency
 */
@Service
public class IsoMessageService {

    /** Parse an incoming 0100 message into an AuthorizationRequest. */
    public AuthorizationRequest toRequest(ISOMsg m) throws ISOException {
        AuthorizationRequest req = new AuthorizationRequest();
        req.setPan(m.getString(2));
        req.setExpiry(m.getString(14));
        req.setAmount(Long.parseLong(m.getString(4)));   // leading zeros tolerated
        req.setCurrency(m.hasField(49) ? m.getString(49) : "840");
        req.setStan(m.getString(11));
        req.setRrn(m.getString(37));
        return req;
    }

    /** Build the 0110 response: echo the request fields, then set DE38/DE39. */
    public ISOMsg toResponse(ISOMsg request, AuthorizationResponse resp) throws ISOException {
        ISOMsg reply = (ISOMsg) request.clone();
        reply.setResponseMTI();                  // 0100 -> 0110
        if (resp.isApproved() && resp.getAuthCode() != null) {
            reply.set(38, resp.getAuthCode());
        }
        reply.set(39, resp.getResponseCode());
        return reply;
    }
}
