package kr.hhplus.be.server.dto.point;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class PointInqDto {

    private long id;
    private String userName;
    private int userPoint;

}
