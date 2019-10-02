package brs.transaction.coloredCoins

import brs.Account
import brs.DependencyProvider
import brs.Transaction

abstract class OrderCancellation(dp: DependencyProvider) : ColoredCoins(dp) {
    override suspend fun applyAttachmentUnconfirmed(transaction: Transaction, senderAccount: Account) = true

    override suspend fun undoAttachmentUnconfirmed(transaction: Transaction, senderAccount: Account) = Unit

    override fun hasRecipient() = false
}
