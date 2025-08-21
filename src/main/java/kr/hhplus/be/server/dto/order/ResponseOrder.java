package kr.hhplus.be.server.dto.order;

import kr.hhplus.be.server.domain.Order;

public record ResponseOrder(
        String userName,
        String productName,
        String couponName,
        boolean couponYn,
        long originalPrice,
        long discountPrice
) {
    public static ResponseOrder from(Order order) {

        boolean hasCoupon = order.getCoupon() != null;
        String couponName = hasCoupon ? order.getCoupon().getName() : "쿠폰을 찾을 수 없음";

        return new ResponseOrder(
                order.getUser().getName(),
                order.getProduct().getName(),
                couponName,
                hasCoupon,
                order.getOriginalPrice(),
                order.getDiscountedPrice()
        );
    }


}
