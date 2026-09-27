package com.konkuk.ma.domain.point.application

import com.konkuk.ma.domain.point.domain.ChargeResult
import com.konkuk.ma.domain.point.domain.PointChargeValidator
import com.konkuk.ma.domain.point.domain.PointProductWithDiscountFinder
import com.konkuk.ma.domain.point.domain.history.NewPointHistory
import com.konkuk.ma.domain.point.domain.payment.PaymentApproverRouter
import com.konkuk.ma.domain.point.domain.payment.PaymentMethod
import com.konkuk.ma.domain.point.domain.payment.PaymentOrder
import com.konkuk.ma.domain.point.domain.port.MemberPointRepository
import com.konkuk.ma.domain.point.domain.port.PointHistoryRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class PointChargeService(
    private val productFinder: PointProductWithDiscountFinder,
    private val pointChargeValidator: PointChargeValidator,
    private val paymentApproverRouter: PaymentApproverRouter,
    private val memberPointRepository: MemberPointRepository,
    private val pointHistoryRepository: PointHistoryRepository,
) {
    fun charge(
        ownerId: Long,
        pointProductId: Long,
        paymentMethod: PaymentMethod,
        paymentToken: String,
        orderPointPrice: Int,
        idempotencyKey: String,
    ): ChargeResult {
        val product = productFinder.findOne(pointProductId)
        pointChargeValidator.validate(idempotencyKey, orderPointPrice, product)

        val approval = paymentApproverRouter.approve(
            paymentMethod,
            PaymentOrder.of(paymentMethod, paymentToken, idempotencyKey, product),
        )
        val memberPoint = memberPointRepository.findOneOrInitial(ownerId)
            .charge(product.chargeQuantity())
        memberPointRepository.save(memberPoint)
        val pointHistoryId = pointHistoryRepository.save(
            NewPointHistory.forCharge(ownerId, paymentMethod, idempotencyKey, product, approval),
        )

        return ChargeResult(
            pointHistoryId = pointHistoryId,
            balance = memberPoint.balance,
            chargedQuantity = product.chargeQuantity(),
            paidAmount = approval.approvedAmount,
            approvalNumber = approval.approvalNumber,
        )
    }
}
