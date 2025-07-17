package kr.hhplus.be.server.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.hhplus.be.server.dto.point.PointChargeDto;
import kr.hhplus.be.server.dto.point.PointInqDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/point")
@Tag(name = "포인트 API", description = "사용자 포인트 조회 및 충전")
public class PointController {

    @Operation(summary = "id값을 통해 사용자 조회")
    @GetMapping("/{id}")
    public ResponseEntity<PointInqDto> getUserPoint(@PathVariable("id") long id) {

        PointInqDto userPointInfo = new PointInqDto(1,"이승준",1000);

        return ResponseEntity.ok(userPointInfo);
    }

