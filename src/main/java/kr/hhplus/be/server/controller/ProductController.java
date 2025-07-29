package kr.hhplus.be.server.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.hhplus.be.server.dto.product.ResponseProduct;
import kr.hhplus.be.server.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/product")
@Tag(name = "상품 API", description = "상품조회")
public class ProductController {

    private final ProductService productService;

    @Operation(summary = "상품 list를 조회")
    @GetMapping("/list")
    public ResponseEntity<List<ResponseProduct>> getProductList() {

        List<ResponseProduct> productListDto = productService.getProductList();

        return ResponseEntity.ok(productListDto);
    }

    @Operation(summary = "상위 5개 상품 조회")
    @GetMapping("/list/top5")
    public ResponseEntity<List<ResponseProduct>> getProductListTop5() {

        List<ResponseProduct> productListDto = productService.getProductListTop5();

        return ResponseEntity.ok(productListDto);
    }

}
