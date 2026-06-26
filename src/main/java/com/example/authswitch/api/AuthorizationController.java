package com.example.authswitch.api;

import com.example.authswitch.api.dto.AuthorizationRequest;
import com.example.authswitch.api.dto.AuthorizationResponse;
import com.example.authswitch.service.AuthorizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** REST front door to the authorization engine. Try it at /swagger-ui.html. */
@RestController
@RequestMapping("/authorize")
@Tag(name = "Authorization", description = "Real-time card authorization (0100 -> 0110)")
public class AuthorizationController {

    private final AuthorizationService authorizationService;

    public AuthorizationController(AuthorizationService authorizationService) {
        this.authorizationService = authorizationService;
    }

    @PostMapping
    @Operation(summary = "Authorize a card transaction",
            description = "Runs card/limit/balance checks and returns an approve/decline with an ISO 8583 response code.")
    public AuthorizationResponse authorize(@Valid @RequestBody AuthorizationRequest request) {
        return authorizationService.authorize(request);
    }
}
