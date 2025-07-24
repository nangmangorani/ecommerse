package kr.hhplus.be.dto.point;

import kr.hhplus.be.domain.User;

public record ResponseUserPoint(
        long userId,
        String userName,
        long userPoint
) {
    public static ResponseUserPoint from(User user) {
        return new ResponseUserPoint(
                user.getId(),
                user.getName(),
                user.getPoint()
        );
    }


}
