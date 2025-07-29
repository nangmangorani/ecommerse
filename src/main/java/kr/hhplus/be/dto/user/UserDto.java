package kr.hhplus.be.dto.user;


import lombok.*;

import java.util.Date;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Data
public class UserDto {

    private long userId;
    private String userName;
    private int userPoint;
    private Date regDateTime;

}
