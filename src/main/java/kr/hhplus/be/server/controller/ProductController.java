package kr.hhplus.be.server.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.hhplus.be.server.dto.product.ResponseProductList;
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
    public ResponseEntity<List<ResponseProductList>> getProductList() {

        List<ResponseProductList> productListDto = List.of(
                new ResponseProductList(1,"상품1",3,1000,"의류"),
                new ResponseProductList(2,"상품2",5,2000,"침구류")
                );

        return ResponseEntity.ok(productListDto);

    }

    @Operation(summary = "상위 5개 상품 조회")
    @GetMapping("/list/top5")
    public ResponseEntity<List<ResponseProductList>> getProductListTop5() {

        List<ResponseProductList> productListDto = List.of(
                new ResponseProductList(1,"상품1",3,1000,"의류"),
                new ResponseProductList(2,"상품2",4,2000,"침구류"),
                new ResponseProductList(3,"상품3",5,3000,"가구류"),
                new ResponseProductList(4,"상품4",1,6100,"필기구"),
                new ResponseProductList(5,"상품5",7,5000,"의류")
                );

        return ResponseEntity.ok(productListDto);
    }

}
