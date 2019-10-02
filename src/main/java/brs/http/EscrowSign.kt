package brs.http

import brs.*
import brs.http.common.Parameters.DECISION_PARAMETER
import brs.http.common.Parameters.ESCROW_PARAMETER
import brs.http.common.ResultFields.ERROR_CODE_RESPONSE
import brs.http.common.ResultFields.ERROR_DESCRIPTION_RESPONSE
import brs.util.Convert
import brs.util.parseUnsignedLong
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import javax.servlet.http.HttpServletRequest

internal class EscrowSign internal constructor(private val dp: DependencyProvider) : CreateTransaction(dp, arrayOf(APITag.TRANSACTIONS, APITag.CREATE_TRANSACTION), ESCROW_PARAMETER, DECISION_PARAMETER) {

    override suspend fun processRequest(request: HttpServletRequest): JsonElement {
        val escrowId: Long
        try {
            escrowId = Convert.emptyToNull(request.getParameter(ESCROW_PARAMETER)).parseUnsignedLong()
        } catch (e: Exception) {
            val response = JsonObject()
            response.addProperty(ERROR_CODE_RESPONSE, 3)
            response.addProperty(ERROR_DESCRIPTION_RESPONSE, "Invalid or not specified escrow")
            return response
        }

        val escrow = dp.escrowService.getEscrowTransaction(escrowId)
        if (escrow == null) {
            val response = JsonObject()
            response.addProperty(ERROR_CODE_RESPONSE, 5)
            response.addProperty(ERROR_DESCRIPTION_RESPONSE, "Escrow transaction not found")
            return response
        }

        val decision = Escrow.stringToDecision(request.getParameter(DECISION_PARAMETER))
        if (decision == null) {
            val response = JsonObject()
            response.addProperty(ERROR_CODE_RESPONSE, 5)
            response.addProperty(ERROR_DESCRIPTION_RESPONSE, "Invalid or not specified action")
            return response
        }

        val sender = dp.parameterService.getSenderAccount(request)
        if (!isValidUser(escrow, sender)) {
            val response = JsonObject()
            response.addProperty(ERROR_CODE_RESPONSE, 5)
            response.addProperty(ERROR_DESCRIPTION_RESPONSE, "Invalid or not specified action")
            return response
        }

        if (escrow.senderId == sender.id && decision != Escrow.DecisionType.RELEASE) {
            val response = JsonObject()
            response.addProperty(ERROR_CODE_RESPONSE, 4)
            response.addProperty(ERROR_DESCRIPTION_RESPONSE, "Sender can only release")
            return response
        }

        if (escrow.recipientId == sender.id && decision != Escrow.DecisionType.REFUND) {
            val response = JsonObject()
            response.addProperty(ERROR_CODE_RESPONSE, 4)
            response.addProperty(ERROR_DESCRIPTION_RESPONSE, "Recipient can only refund")
            return response
        }

        val attachment = Attachment.AdvancedPaymentEscrowSign(dp, escrow.id, decision, dp.blockchain.height)

        return createTransaction(request, sender, null, 0, attachment)
    }

    private fun isValidUser(escrow: Escrow, sender: Account): Boolean {
        return escrow.senderId == sender.id ||
                escrow.recipientId == sender.id ||
                dp.escrowService.isIdSigner(sender.id, escrow)
    }
}
