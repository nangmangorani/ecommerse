package kr.hhplus.be.server.dto.product;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class ProductListDto {

    private long productId;
    private String productName;
    private int productQuantity;
    private int price;
    private String productType;

}
