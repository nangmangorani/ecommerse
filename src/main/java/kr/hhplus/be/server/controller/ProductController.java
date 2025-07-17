package kr.hhplus.be.server.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.hhplus.be.server.dto.product.ProductListDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/product")
@Tag(name = "상품 API", description = "상품조회")
public class ProductController {

    @Operation(summary = "상품 list를 조회")
    @GetMapping("/list")
    public ResponseEntity<List<ProductListDto>> getProductList() {

        List<ProductListDto> productListDto = List.of(
                new ProductListDto(1,"상품1",3,1000,"의류"),
                new ProductListDto(2,"상품2",5,2000,"침구류")
                );

        return ResponseEntity.ok(productListDto);

    }

