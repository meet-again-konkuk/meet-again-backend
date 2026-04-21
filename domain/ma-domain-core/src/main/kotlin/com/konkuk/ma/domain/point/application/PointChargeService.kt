package com.konkuk.ma.domain.point.application

import com.konkuk.ma.domain.point.application.command.ChargePointCommand
import com.konkuk.ma.domain.point.application.result.ChargeResult
import com.konkuk.ma.domain.point.domain.PointChargeValidator
import com.konkuk.ma.domain.point.domain.PointProductWithDiscountFinder
import com.konkuk.ma.domain.point.domain.payment.PaymentApprovalRequest
import com.konkuk.ma.domain.point.domain.payment.PaymentApproverRouter
import com.konkuk.ma.domain.point.domain.port.MemberPointRepository
import com.konkuk.ma.domain.point.domain.port.PointTransactionRepository
import com.konkuk.ma.domain.point.domain.transaction.NewPointTransaction
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class PointChargeService(
    private val productFinder: PointProductWithDiscountFinder,
    private val pointChargeValidator: PointChargeValidator,
    private val paymentApproverRouter: PaymentApproverRouter,
    private val memberPointRepository: MemberPointRepository,
    private val pointTransactionRepository: PointTransactionRepository,
) {
    fun charge(command: ChargePointCommand): ChargeResult {
        val product = productFinder.findOne(command.pointProductId)
        pointChargeValidator.validate(command, product.discountedPrice())

        val approval = paymentApproverRouter.approve(
            command.paymentMethod,
            PaymentApprovalRequest.of(command, product),
        )
        val memberPoint = memberPointRepository.findOneOrInitial(command.ownerEmail)
            .charge(product.chargeQuantity())
        memberPointRepository.save(memberPoint)
        val pointTransactionId = pointTransactionRepository.save(
            NewPointTransaction.forCharge(command, product, approval),
        )

        return ChargeResult(
            pointTransactionId = pointTransactionId,
            balance = memberPoint.balance,
            chargedQuantity = product.chargeQuantity(),
            paidAmount = approval.approvedAmount,
            approvalNumber = approval.approvalNumber,
        )
    }
}
