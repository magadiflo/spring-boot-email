package com.magadiflo.app.resource;

import com.magadiflo.app.domain.HttpResponse;
import com.magadiflo.app.domain.User;
import com.magadiflo.app.service.IUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.Map;


@RequiredArgsConstructor
@RestController
@RequestMapping(path = "/api/v1/users")
public class UserResource {

    private final IUserService userService;

    @PostMapping
    public ResponseEntity<HttpResponse> createUser(@RequestBody User user) {
        User userDB = this.userService.saveUser(user);
        URI uriUser = URI.create("");
        HttpResponse httpResponse = HttpResponse.builder()
                .timeStamp(LocalDateTime.now().toString())
                .data(Map.of("user", userDB))
                .message("Usuario creado")
                .statusCode(HttpStatus.CREATED.value())
                .status(HttpStatus.CREATED)
                .build();
        return ResponseEntity.created(uriUser).body(httpResponse);
    }

    @GetMapping
    public ResponseEntity<HttpResponse> confirmUserAccount(@RequestParam String token) {
        Boolean isSuccess = this.userService.verifyToken(token);
        HttpResponse httpResponse = HttpResponse.builder()
                .timeStamp(LocalDateTime.now().toString())
                .data(Map.of("success", isSuccess))
                .message("Cuenta verificada")
                .statusCode(HttpStatus.OK.value())
                .status(HttpStatus.OK)
                .build();
        return ResponseEntity.ok(httpResponse);
    }

}
