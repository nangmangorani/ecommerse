package kr.hhplus.be.server.dto.product;


import kr.hhplus.be.server.domain.Product;

public record ResponseProduct(
        long productId,
        String productName,
        int sellQuantity,
        int productQuantity,
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
                product.getProductType()
        );
    }

}
