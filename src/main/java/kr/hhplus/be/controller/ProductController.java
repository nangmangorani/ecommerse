package kr.hhplus.be.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.hhplus.be.dto.product.ResponseProduct;
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
    public ResponseEntity<List<ResponseProduct>> getProductList() {

        List<ResponseProduct> productListDto = List.of(
                new ResponseProduct(1,"상품1",3,1000,"의류"),
                new ResponseProduct(2,"상품2",5,2000,"침구류")
                );

        return ResponseEntity.ok(productListDto);

    }

    @Operation(summary = "상위 5개 상품 조회")
    @GetMapping("/list/top5")
    public ResponseEntity<List<ResponseProduct>> getProductListTop5() {

        List<ResponseProduct> productListDto = List.of(
                new ResponseProduct(1,"상품1",3,1000,"의류"),
                new ResponseProduct(2,"상품2",4,2000,"침구류"),
                new ResponseProduct(3,"상품3",5,3000,"가구류"),
                new ResponseProduct(4,"상품4",1,6100,"필기구"),
                new ResponseProduct(5,"상품5",7,5000,"의류")
                );

        return ResponseEntity.ok(productListDto);
    }

}
