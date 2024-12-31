package com.example.telegram_bot.service;

import com.example.telegram_bot.config.BotConfig;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class TelegramBot extends TelegramLongPollingBot {

    private final BotConfig config;
    private static final long MANAGER_USER_ID = 6614865222L;
    private final Map<Integer, Long> messageIdToUserIdMap = new HashMap<>();

    public TelegramBot(BotConfig config) {
        this.config = config;

        List<BotCommand> listOfCommands = new ArrayList<>();
        listOfCommands.add(new BotCommand("/start", "получить приветствование"));

        try {
            this.execute(new SetMyCommands(listOfCommands, new BotCommandScopeDefault(), null));
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getBotUsername() {
        return config.getBotName();
    }

    @Override
    public String getBotToken() {
        return config.getToken();
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasCallbackQuery()) {
            String callbackData = update.getCallbackQuery().getData();
            long chatId = update.getCallbackQuery().getMessage().getChatId();

            switch (callbackData) {
                case "order":
                    sendOrderMessage(chatId);
                    break;
                case "delivery":
                    sendMessage(chatId, "Информация о доставке: Мы доставляем товары по всему миру. Свяжитесь с менеджером для уточнения деталей.");
                    break;
                case "terms":
                    sendMessage(chatId, "Условия покупки: Полные условия можно узнать на нашем сайте или у менеджера.");
                    break;
                default:
                    sendMessage(chatId, "Неизвестная команда.");
            }
        } else if (update.hasMessage() && update.getMessage().hasText()) {
            long chatId = update.getMessage().getChatId();
            String messageText = update.getMessage().getText();

            if (chatId == MANAGER_USER_ID) {
                handleManagerResponse(update.getMessage().getReplyToMessage(), messageText);
            } else if (messageText.equals("/start")) {
                String name = update.getMessage().getChat().getFirstName();
                sendWelcomeMessage(chatId, name);
            } else {
                forwardToManager(chatId, messageText);
            }
        }
    }

    private void sendWelcomeMessage(long chatId, String name) {
        String textToSend = name + ", здравствуйте! \n\n" +
                "Я - бот-помощник байер-сервиса KUPIDON, созданный для вашего удобства. Я отвечу на все ваши вопросы! \n\n" +
                "Байер-сервис KUPIDON помогает осуществлять покупки желаемых товаров из США, Европы, а также ювелирных украшений из Дубая. \n\n" +
                "Почему шопинг с KUPIDON — это лучший выбор? Вот 5 причин: \n\n" +
                "- Адекватная наценка; \n" +
                "- Бесплатные замеры; \n" +
                "- Только оригинальные брендовые вещи с гарантией качества и подлинности; \n" +
                "- Индивидуальный подход к каждому клиенту; \n" +
                "- Выгодные условия доставки. \n\n" +
                "Выберите интересующий раздел ниже 🔻";

        sendMessageWithMainButtons(chatId, textToSend);
    }

    private void sendMessageWithMainButtons(long chatId, String textToSend) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(textToSend);

        InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();

        rowsInline.add(List.of(
                createInlineButton("Отзывы", "https://t.me/feedbackkupidon", true),
                createInlineButton("Оформить заказ", "order", false)
        ));
        rowsInline.add(List.of(
                createInlineButton("Доставка", "delivery", false),
                createInlineButton("Условия", "terms", false)
        ));
        rowsInline.add(List.of(
                createInlineButton("Каталог", "https://t.me/kupidonbuyer", true)
        ));

        markupInline.setKeyboard(rowsInline);
        message.setReplyMarkup(markupInline);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private InlineKeyboardButton createInlineButton(String text, String dataOrUrl, boolean isUrl) {
        InlineKeyboardButton button = new InlineKeyboardButton();
        button.setText(text);
        if (isUrl) {
            button.setUrl(dataOrUrl);
        } else {
            button.setCallbackData(dataOrUrl);
        }
        return button;
    }

    private void sendOrderMessage(long chatId) {
        String messageText = "Мы будем рады Вам помочь ❤️\n" +
                "Скажите, пожалуйста, что Вас интересует.\n\n" +
                "Вы также можете прислать фото, видео или голосовое сообщение ☺️";
        sendMessage(chatId, messageText);
    }

    private void sendMessage(long chatId, String textToSend) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(textToSend);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void forwardToManager(long userChatId, String messageText) {
        SendMessage forwardMessage = new SendMessage();
        forwardMessage.setChatId(String.valueOf(MANAGER_USER_ID));
        forwardMessage.setText("Сообщение от пользователя " + userChatId + ":\n" + messageText);

        try {
            var sentMessage = execute(forwardMessage);
            int messageId = sentMessage.getMessageId();
            messageIdToUserIdMap.put(messageId, userChatId);
            System.out.println("Forwarded message ID: " + messageId + " mapped to user ID: " + userChatId);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void handleManagerResponse(org.telegram.telegrambots.meta.api.objects.Message replyToMessage, String managerMessage) {
        if (replyToMessage == null) {
            sendMessage(MANAGER_USER_ID, "Ошибка: невозможно определить пользователя.");
            System.out.println("No replyToMessage found in manager's response.");
            return;
        }

        int originalMessageId = replyToMessage.getMessageId();
        long userChatId = messageIdToUserIdMap.getOrDefault(originalMessageId, -1L);

        if (userChatId == -1) {
            sendMessage(MANAGER_USER_ID, "Ошибка: не удалось определить ID пользователя.");
            System.out.println("Message ID " + originalMessageId + " not found in map.");
            return;
        }

        sendMessage(userChatId, "Ответ от менеджера:\n" + managerMessage);
        System.out.println("Replied to user ID: " + userChatId + " with message: " + managerMessage);
    }
}