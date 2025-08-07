package kr.hhplus.be.server.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.hhplus.be.server.dto.order.RequestOrder;
import kr.hhplus.be.server.dto.order.ResponseOrder;
import kr.hhplus.be.server.service.OrderFacade;
import kr.hhplus.be.server.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/order")
@Tag(name = "주문 API", description = "상품조회")
public class OrderController {

    private final OrderService orderService;

    private final OrderFacade orderFacade;

    @Operation(summary = "상품 주문 및 결제")
    @PostMapping("")
    public ResponseEntity<ResponseOrder> responseOrder(@RequestBody RequestOrder requestOrder) {

        ResponseOrder orderDto = orderFacade.processOrder(requestOrder);

        return ResponseEntity.ok(orderDto);
    }

}
