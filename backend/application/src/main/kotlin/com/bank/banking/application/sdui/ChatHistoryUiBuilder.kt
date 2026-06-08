package com.bank.banking.application.sdui

import com.bank.banking.application.usecase.ListChatHistoryUseCase
import com.bank.banking.domain.model.sdui.UiNode
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class ChatHistoryUiBuilder(
    private val listChatHistory: ListChatHistoryUseCase,
) : ChatUiBuilder {
    override suspend fun build(ctx: ChatUiBuildContext): UiNode {
        val entries = listChatHistory.execute(ctx.token)
        if (entries.isEmpty()) {
            return SduiNodeFactory.column(
                "chat-history-empty",
                SduiNodeFactory.assistantText("chat-history-empty-text", GroundedChatCopy.chatHistoryEmpty()),
            )
        }

        val children = mutableListOf<UiNode>(
            SduiNodeFactory.assistantText(
                "chat-history-intro",
                GroundedChatCopy.chatHistoryIntro(entries.size),
            ),
        )
        entries.forEachIndexed { index, entry ->
            children += SduiNodeFactory.chatHistoryRow(
                id = "chat-history-row-$index",
                sessionLabel = "Conversación ${entries.size - index}",
                lastMessage = entry.lastUserMessage,
                turnCount = entry.turnCount,
                timeLabel = formatEpoch(entry.updatedAtEpochMs),
                lastIntent = entry.lastIntent ?: "—",
            )
        }
        children += SduiNodeFactory.assistantText(
            "chat-history-follow-up",
            GroundedChatCopy.chatHistoryFollowUp(),
        )
        return SduiNodeFactory.column("chat-history-root", *children.toTypedArray())
    }

    private fun formatEpoch(epochMs: Long): String {
        if (epochMs <= 0L) return ""
        val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
        return Instant.ofEpochMilli(epochMs).atZone(ZoneId.systemDefault()).format(formatter)
    }
}
