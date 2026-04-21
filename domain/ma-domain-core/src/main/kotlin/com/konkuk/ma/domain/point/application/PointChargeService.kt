package com.konkuk.ma.domain.point.application

import com.konkuk.ma.domain.common.domain.Email
import com.konkuk.ma.domain.point.application.command.ChargePointCommand
import com.konkuk.ma.domain.point.application.result.ChargeResult
import com.konkuk.ma.domain.point.domain.PointChargeValidator
import com.konkuk.ma.domain.point.domain.PointProduct
import com.konkuk.ma.domain.point.domain.PointProductWithDiscount
import com.konkuk.ma.domain.point.domain.balance.MemberPoint
import com.konkuk.ma.domain.point.domain.balance.PointQuantity
import com.konkuk.ma.domain.point.domain.payment.PaymentApproval
import com.konkuk.ma.domain.point.domain.payment.PaymentApprovalRequest
import com.konkuk.ma.domain.point.domain.payment.PaymentApproverRouter
import com.konkuk.ma.domain.point.domain.port.DiscountPolicyQueryRepository
import com.konkuk.ma.domain.point.domain.port.MemberPointRepository
import com.konkuk.ma.domain.point.domain.port.PointProductQueryRepository
import com.konkuk.ma.domain.point.domain.port.PointTransactionRepository
import com.konkuk.ma.domain.point.domain.transaction.NewPointTransaction
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class PointChargeService(
    private val pointProductQueryRepository: PointProductQueryRepository,
    private val discountPolicyQueryRepository: DiscountPolicyQueryRepository,
    private val memberPointRepository: MemberPointRepository,
    private val pointTransactionRepository: PointTransactionRepository,
    private val paymentApproverRouter: PaymentApproverRouter,
    private val pointChargeValidator: PointChargeValidator,
) {
    fun charge(command: ChargePointCommand): ChargeResult {
        val productWithDiscount = findProductWithDiscount(command.pointProductId)
        pointChargeValidator.validate(command, productWithDiscount.discountedPrice())

        val approval = approve(command, productWithDiscount)
        val chargedMemberPoint = chargeMemberPoint(command.ownerEmail, productWithDiscount.chargeQuantity())
        val pointTransactionId = recordTransaction(command, productWithDiscount, approval)

        return ChargeResult(
            pointTransactionId = pointTransactionId,
            balance = chargedMemberPoint.balance,
            chargedQuantity = productWithDiscount.chargeQuantity(),
            paidAmount = approval.approvedAmount,
            approvalNumber = approval.approvalNumber,
        )
    }

    private fun findProductWithDiscount(pointProductId: Long): PointProductWithDiscount {
        val product: PointProduct = pointProductQueryRepository.findOne(pointProductId)
        val discountPolicy = product.discountPolicyId?.let { discountPolicyQueryRepository.findOneOrNull(it) }
        return PointProductWithDiscount(product, discountPolicy)
    }

    private fun approve(
        command: ChargePointCommand,
        productWithDiscount: PointProductWithDiscount,
    ): PaymentApproval {
        val approver = paymentApproverRouter.resolve(command.paymentMethod)
        return approver.approve(PaymentApprovalRequest.of(command, productWithDiscount))
    }

    private fun chargeMemberPoint(ownerEmail: Email, quantity: PointQuantity): MemberPoint {
        val memberPoint = memberPointRepository.findOneOrInitial(ownerEmail)
        val chargedMemberPoint = memberPoint.charge(quantity)
        memberPointRepository.save(chargedMemberPoint)
        return chargedMemberPoint
    }

    private fun recordTransaction(
        command: ChargePointCommand,
        productWithDiscount: PointProductWithDiscount,
        approval: PaymentApproval,
    ): Long {
        return pointTransactionRepository.save(
            NewPointTransaction.forCharge(command, productWithDiscount, approval),
        )
    }
}
