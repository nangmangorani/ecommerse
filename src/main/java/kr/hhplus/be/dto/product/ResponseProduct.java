package kr.hhplus.be.dto.product;


import kr.hhplus.be.domain.Product;

public record ResponseProduct(
        long productId,
        String productName,
        int productQuantity,
        long price,
        String productType
) {
    public static ResponseProduct from(Product product) {
        return new ResponseProduct(
                product.getId(),
                product.getName(),
                product.getQuantity(),
                product.getPrice(),
                product.getProductType()
        );
    }

}
