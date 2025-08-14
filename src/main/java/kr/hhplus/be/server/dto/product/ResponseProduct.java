package kr.hhplus.be.server.dto.product;


import kr.hhplus.be.server.domain.Product;

public record ResponseProduct(
        Long productId,
        String productName,
        int productQuantity,
        int sellQuantity,
        long price,
        String productType
) {
    public static ResponseProduct from(Product product) {
        return new ResponseProduct(
                product.getId(),
                product.getName(),
                product.getQuantity(),
                product.getSellQuantity(),
                product.getPrice(),
                product.getType()
        );
    }
}
