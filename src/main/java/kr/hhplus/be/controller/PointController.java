package kr.hhplus.be.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.hhplus.be.dto.point.RequestPointCharge;
import kr.hhplus.be.dto.point.ResponseUserPoint;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/point")
@Tag(name = "포인트 API", description = "사용자 포인트 조회 및 충전")
public class PointController {

    @Operation(summary = "id값을 통해 사용자 조회")
    @GetMapping("/{id}")
    public ResponseEntity<ResponseUserPoint> getUserPoint(@PathVariable("id") long id) {

        ResponseUserPoint userPointInfo = new ResponseUserPoint(1,"이승준",1000);

        return ResponseEntity.ok(userPointInfo);
    }

    @Operation(summary = "사용자 포인트 충전")
    @PostMapping("/charge")
    public ResponseEntity<ResponseUserPoint> chargeUserPoint(@RequestBody RequestPointCharge requestPointCharge) {

        // 포인트 충전(서비스)

        // 포인트 충전 후 현잔고 반환
        ResponseUserPoint userPointInfo = new ResponseUserPoint(1,"이승준",1000);

        return ResponseEntity.ok(userPointInfo);
    }
}
