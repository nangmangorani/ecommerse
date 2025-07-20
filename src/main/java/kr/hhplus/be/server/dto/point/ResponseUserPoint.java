package kr.hhplus.be.server.dto.point;

public record ResponseUserPoint(
        long userId,
        String userName,
        int userPoint
) {}
