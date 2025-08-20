package kr.hhplus.be.server.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.hhplus.be.server.dto.point.RequestPointCharge;
import kr.hhplus.be.server.dto.point.ResponseUserPoint;
import kr.hhplus.be.server.service.PointHistService;
import kr.hhplus.be.server.service.PointService;
import kr.hhplus.be.server.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/point")
@Tag(name = "포인트 API", description = "사용자 포인트 조회 및 충전")
public class PointController {

    private final UserService userService;
    private final PointService pointService;

    @GetMapping("/{id}")
    public ResponseEntity<ResponseUserPoint> getUserPoint(@PathVariable("id") long id) {

        ResponseUserPoint userPointInfo = pointService.getPoint(id);

        return ResponseEntity.ok(userPointInfo);
    }

    @Operation(summary = "사용자 포인트 충전")
    @PostMapping("/charge")
    public ResponseEntity<ResponseUserPoint> chargeUserPoint(@RequestBody RequestPointCharge requestPointCharge) {

        ResponseUserPoint userPointInfo = pointService.chargePoint(requestPointCharge);

        return ResponseEntity.ok(userPointInfo);
    }
}
