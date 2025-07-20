package kr.hhplus.be.server.dto.point;

public record ResponsePointCharge(
        long userId,
        String userName,

        int userPoint
)
{}
