package examp.org.com.dbquerzapp.controller;

import com.example.api.ExecuteQueryApi;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ExecuteQueryController implements ExecuteQueryApi {

    @Override
    public ResponseEntity<String> executeQueryGet(String queryIdentifier) {
        return ResponseEntity.ok().body("OK");
    }
}
