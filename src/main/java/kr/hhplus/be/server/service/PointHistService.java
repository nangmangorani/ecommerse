package kr.hhplus.be.server.service;

import kr.hhplus.be.server.enums.TransactionType;
import kr.hhplus.be.server.domain.PointHist;
import kr.hhplus.be.server.domain.User;
import kr.hhplus.be.server.repository.PointHistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PointHistService {

    public final PointHistRepository pointHistRepository;

    public void createPointHist(User user, TransactionType transactionType, long amount, long point, long paymentNo) {

        PointHist pointHist = new PointHist(
                user,
                transactionType,
                amount,
                point,
                paymentNo
        );

        pointHistRepository.save(pointHist);

    }





}
