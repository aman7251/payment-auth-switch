package com.example.authswitch.iso;

import com.example.authswitch.api.dto.AuthorizationRequest;
import com.example.authswitch.api.dto.AuthorizationResponse;
import org.jpos.iso.ISOMsg;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class IsoMessageServiceTest {

    private final IsoMessageService service = new IsoMessageService();

    private ISOMsg request0100() throws Exception {
        ISOMsg m = new ISOMsg();
        m.setMTI("0100");
        m.set(2, "4111111111111111");
        m.set(3, "000000");
        m.set(4, "1000");
        m.set(11, "000001");
        m.set(14, "3012");
        m.set(37, "000000000001");
        m.set(49, "840");
        return m;
    }

    @Test
    void parsesIsoIntoRequest() throws Exception {
        AuthorizationRequest req = service.toRequest(request0100());

        assertThat(req.getPan()).isEqualTo("4111111111111111");
        assertThat(req.getExpiry()).isEqualTo("3012");
        assertThat(req.getAmount()).isEqualTo(1000L);
        assertThat(req.getCurrency()).isEqualTo("840");
        assertThat(req.getStan()).isEqualTo("000001");
        assertThat(req.getRrn()).isEqualTo("000000000001");
    }

    @Test
    void buildsApprovedResponseAs0110() throws Exception {
        AuthorizationResponse resp = new AuthorizationResponse(
                true, "00", "Approved", "654321", "000001", "000000000001", 5);

        ISOMsg reply = service.toResponse(request0100(), resp);

        assertThat(reply.getMTI()).isEqualTo("0110");
        assertThat(reply.getString(39)).isEqualTo("00");
        assertThat(reply.getString(38)).isEqualTo("654321");
    }

    @Test
    void buildsDeclinedResponseWithoutAuthCode() throws Exception {
        AuthorizationResponse resp = new AuthorizationResponse(
                false, "51", "Insufficient funds", null, "000001", "000000000001", 4);

        ISOMsg reply = service.toResponse(request0100(), resp);

        assertThat(reply.getMTI()).isEqualTo("0110");
        assertThat(reply.getString(39)).isEqualTo("51");
        assertThat(reply.hasField(38)).isFalse();
    }
}
