package kr.hhplus.be.server.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.hhplus.be.server.dto.order.RequestOrderDto;
import kr.hhplus.be.server.dto.order.ResponseOrderDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/order")
@Tag(name = "주문 API", description = "상품조회")
public class OrderController {

    @Operation(summary = "상품 주문 및 결제")
    @PostMapping("/")
    public ResponseEntity<ResponseOrderDto> responseOrderDto(@RequestBody RequestOrderDto requestOrderDto) {

        ResponseOrderDto orderDto = new ResponseOrderDto(1,1,1,"이승준","상품1","의류 20%할인 쿠폰", "Y", 1000, 800);

        return ResponseEntity.ok(orderDto);
    }

}
