package kr.hhplus.be.server.service;

import kr.hhplus.be.TransactionType;
import kr.hhplus.be.server.domain.PointHist;
import kr.hhplus.be.server.domain.User;
import kr.hhplus.be.server.repository.PointHistRepository;
import kr.hhplus.be.server.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class PointHistService {

    public final PointHistRepository pointHistRepository;

    public PointHistService(PointHistRepository pointHistRepository) {
        this.pointHistRepository = pointHistRepository;
    }

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
