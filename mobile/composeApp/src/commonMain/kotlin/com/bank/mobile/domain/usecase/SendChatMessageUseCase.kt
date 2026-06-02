package com.bank.mobile.domain.usecase

import com.bank.mobile.domain.model.sdui.ChatUiResponse
import com.bank.mobile.domain.repository.HomeRepository

class SendChatMessageUseCase(
    private val homeRepository: HomeRepository,
) {
    suspend operator fun invoke(token: String, message: String): ChatUiResponse =
        homeRepository.sendChatMessage(token, message)
}
