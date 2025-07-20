package kr.hhplus.be.server.dto.product;


public record ResponseProductList(
        long productId,
        String productName,
        int productQuantity,
        int price,
        String productType
) {}
