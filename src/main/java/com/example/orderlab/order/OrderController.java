package com.example.orderlab.order;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController @RequestMapping("/orders")
public class OrderController {
    private final OrderService service; public OrderController(OrderService service){this.service=service;}
    @PostMapping @ResponseStatus(HttpStatus.CREATED) public OrderDtos.Response create(@Valid @RequestBody OrderDtos.Request r){return service.create(r);}
    @GetMapping("/{id}") public OrderDtos.Response get(@PathVariable UUID id){return service.get(id);}
    @PutMapping("/{id}") public OrderDtos.Response update(@PathVariable UUID id,@Valid @RequestBody OrderDtos.Request r){return service.update(id,r);}
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void delete(@PathVariable UUID id){service.delete(id);}
    @ExceptionHandler(OrderNotFoundException.class) @ResponseStatus(HttpStatus.NOT_FOUND) public String notFound(OrderNotFoundException e){return e.getMessage();}
}
