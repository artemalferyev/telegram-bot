package com.example.telegram_bot.service;

import com.example.telegram_bot.config.BotConfig;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.send.SendVideo;
import org.telegram.telegrambots.meta.api.methods.send.SendAudio;
import org.telegram.telegrambots.meta.api.methods.send.SendVoice;
import org.telegram.telegrambots.meta.api.objects.InputFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TelegramBot extends TelegramLongPollingBot {

    private final BotConfig config;
    private static final long MANAGER_USER_ID = 6614865222L;
    private final Map<Integer, Long> messageIdToUserIdMap = new ConcurrentHashMap<>();
    private final Map<Long, Map<Integer, Long>> userMessageIdMap = new ConcurrentHashMap<>();

    public TelegramBot(BotConfig config) {
        this.config = config;

        List<BotCommand> listOfCommands = new ArrayList<>();
        listOfCommands.add(new BotCommand("/start", "получить приветствование"));

        try {
            this.execute(new SetMyCommands(listOfCommands, new BotCommandScopeDefault(), null));
        } catch (TelegramApiException e) {
            System.out.println("Error setting bot commands: " + e.getMessage());
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
        if (update.hasMessage()) {
            var message = update.getMessage();

            if (message.getReplyToMessage() != null && message.getChatId() == MANAGER_USER_ID) {
                handleManagerResponse(message.getReplyToMessage(), message.getText());
                return;
            }

            long chatId = message.getChatId();

            if (message.hasText()) {
                String messageText = message.getText();
                if (messageText.equals("/start")) {
                    String name = message.getChat().getFirstName();
                    sendWelcomeMessage(chatId, name);
                } else {
                    forwardToManager(update, chatId, messageText);
                }
            } else if (message.hasPhoto()) {
                forwardMediaToManager(message.getPhoto().get(0).getFileId(), "photo", chatId, update);
            } else if (message.hasVideo()) {
                forwardMediaToManager(message.getVideo().getFileId(), "video", chatId, update);
            } else if (message.hasAudio()) {
                forwardMediaToManager(message.getAudio().getFileId(), "audio", chatId, update);
            } else if (message.hasVoice()) {
                forwardMediaToManager(message.getVoice().getFileId(), "voice", chatId, update);
            }
        } else if (update.hasCallbackQuery()) {
            String callbackData = update.getCallbackQuery().getData();
            long chatId = update.getCallbackQuery().getMessage().getChatId();

            switch (callbackData) {
                case "order":
                    sendOrderMessage(chatId);
                    break;

                case "delivery":
                    sendDeliveryInfo(chatId);
                    break;

                default:
                    sendMessage(chatId, "Неизвестная команда.");
            }
        }
    }

    private void sendDeliveryInfo(long chatId) {
        String usaDeliveryInfo = "📦 ДОСТАВКА ИЗ США\n" +
                "Мы сотрудничаем с надежными поставщиками, что гарантирует своевременную доставку и страхование вашего заказа.\n\n" +
                "1 кг → 21 $ → 2688 ₽\n" +
                "2 кг → 39 $ → 4992 ₽\n" +
                "3 кг → 54 $ → 6912 ₽\n" +
                "4 кг → 68 $ → 8704 ₽\n" +
                "5 кг → 88 $ → 11264 ₽\n" +
                "6 кг → 103 $ → 13184 ₽\n" +
                "7 кг → 111 $ → 14208 ₽\n" +
                "8 кг → 121 $ → 15488 ₽\n" +
                "9 кг → 129 $ → 16512 ₽\n" +
                "10 кг → 131 $ → 16768 ₽\n" +
                "11 кг → 147 $ → 18816 ₽\n" +
                "12 кг → 155 $ → 19840 ₽\n" +
                "13 кг → 162 $ → 20736 ₽\n" +
                "14 кг → 171 $ → 21888 ₽\n" +
                "15 кг → 173 $ → 22144 ₽\n\n" +
                "Срок: 10 дней-6 недель\n" +
                "Курс валюты постоянно меняется, уточняйте его у нас в день оформления заказа.\n";

        String europeDeliveryInfo = "📦 ДОСТАВКА ИЗ ЕВРОПЫ\n" +
                "Мы сотрудничаем с надежными поставщиками, что гарантирует своевременную доставку и страхование вашего заказа.\n\n" +
                "1 кг → 35 € → 4830 ₽\n" +
                "2 кг → 39 € → 5382 ₽\n" +
                "3 кг → 44 € → 6072 ₽\n" +
                "4 кг → 48 € → 6624 ₽\n" +
                "5 кг → 53 € → 7314 ₽\n" +
                "6 кг → 57 € → 7866 ₽\n" +
                "7 кг → 62 € → 8556 ₽\n" +
                "8 кг → 66 € → 9108 ₽\n" +
                "9 кг → 71 € → 9798 ₽\n" +
                "10 кг → 75 € → 10 350 ₽\n" +
                "11 кг → 79 € → 10 902 ₽\n" +
                "12 кг → 84 € → 11 592 ₽\n" +
                "13 кг → 89 € → 12 282 ₽\n" +
                "14 кг → 93 € → 12 834 ₽\n" +
                "15 кг → 98 € → 13 524 ₽\n\n" +
                "Срок: 10 дней-6 недель\n" +
                "Курс валюты постоянно меняется, уточняйте его у нас в день оформления заказа.\n";

        String additionalInfo = "📌 ВАЖНО:\n\n" +
                "▫️ Бесплатное хранение товаров — 30 дней с момента поступления заказа, далее — 0,15 € за каждый день;\n\n" +
                "▫️ Возврат в магазин — 15 €;\n\n" +
                "▫️ Отправка из Европы осуществляется 2 раза в неделю партиями.\n\n" +
                "▫️ Переупаковка готовых к отправке товаров — 5 €;\n\n" +
                "▫️ Доставка lux-брендов осуществляется в срок 7 дней, обсуждается индивидуально.\n\n" +
                "▫️ Доставка ювелирных украшений осуществляется личным курьером, обсуждается индивидуально.\n";

        sendMessage(chatId, usaDeliveryInfo);
        sendMessage(chatId, europeDeliveryInfo);
        sendMessage(chatId, additionalInfo);
    }

    private void forwardMediaToManager(String fileId, String mediaType, long userChatId, Update update) {
        try {
            String userName = "";
            String firstName = update.getMessage().getChat().getFirstName();
            String lastName = update.getMessage().getChat().getLastName();

            if (firstName != null) userName += firstName;
            if (lastName != null) userName += " " + lastName;

            Integer forwardedMessageId = null;

            switch (mediaType) {
                case "photo":
                    SendPhoto sendPhoto = new SendPhoto();
                    sendPhoto.setChatId(String.valueOf(MANAGER_USER_ID));
                    sendPhoto.setPhoto(new InputFile(fileId));
                    sendPhoto.setCaption("Фото от пользователя " + userName + " (ID: " + userChatId + ")");
                    forwardedMessageId = execute(sendPhoto).getMessageId();
                    break;

                case "video":
                    SendVideo sendVideo = new SendVideo();
                    sendVideo.setChatId(String.valueOf(MANAGER_USER_ID));
                    sendVideo.setVideo(new InputFile(fileId));
                    sendVideo.setCaption("Видео от пользователя " + userName + " (ID: " + userChatId + ")");
                    forwardedMessageId = execute(sendVideo).getMessageId();
                    break;

                case "audio":
                    SendAudio sendAudio = new SendAudio();
                    sendAudio.setChatId(String.valueOf(MANAGER_USER_ID));
                    sendAudio.setAudio(new InputFile(fileId));
                    sendAudio.setCaption("Аудио от пользователя " + userName + " (ID: " + userChatId + ")");
                    forwardedMessageId = execute(sendAudio).getMessageId();
                    break;

                case "voice":
                    SendVoice sendVoice = new SendVoice();
                    sendVoice.setChatId(String.valueOf(MANAGER_USER_ID));
                    sendVoice.setVoice(new InputFile(fileId));
                    sendVoice.setCaption("Голосовое сообщение от пользователя " + userName + " (ID: " + userChatId + ")");
                    forwardedMessageId = execute(sendVoice).getMessageId();
                    break;
            }


            if (forwardedMessageId != null) {
                messageIdToUserIdMap.put(forwardedMessageId, userChatId);
                System.out.println("Mapping forwarded message ID " + forwardedMessageId + " to user ID " + userChatId);
            }

        } catch (TelegramApiException e) {
            System.out.println("Error forwarding " + mediaType + " to manager: " + e.getMessage());
            e.printStackTrace();
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
                createInlineButton("Оформить заказ", "https://t.me/marinakupidon", true)
        ));
        rowsInline.add(List.of(
                createInlineButton("Каталог", "https://t.me/kupidonbuyer", true)
        ));
        rowsInline.add(List.of(
                createInlineButton("Доставка", "delivery", false)
        ));
        rowsInline.add(List.of(
                createInlineButton("Отзывы", "https://t.me/feedbackkupidon", true)
        ));

        markupInline.setKeyboard(rowsInline);
        message.setReplyMarkup(markupInline);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            System.out.println("Error sending message with buttons: " + e.getMessage());
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
                "Вы можете написать ниже текстовое сообщение, а также прислать фото, видео или голосовое сообщение ☺️";

        sendMessage(chatId, messageText);
    }

    private void sendMessage(long chatId, String textToSend) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(textToSend);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            System.out.println("Error sending message: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void forwardToManager(Update update, long chatId, String messageText) {
        try {

            if (chatId == MANAGER_USER_ID) {
                System.out.println("Manager message cannot be forwarded to manager.");
                return;
            }

            String firstName = update.getMessage().getChat().getFirstName();
            String lastName = update.getMessage().getChat().getLastName();
            String userName = (firstName != null ? firstName : "") +
                    (lastName != null && !lastName.isEmpty() ? " " + lastName : "").trim();

            SendMessage forwardMessage = new SendMessage();
            forwardMessage.setChatId(String.valueOf(MANAGER_USER_ID));
            forwardMessage.setText(String.format("Сообщение от пользователя %s (ID: %d):\n\n%s", userName, chatId, messageText));

            System.out.println("Forwarding message to manager: " + messageText);

            var sentMessage = execute(forwardMessage);

            if (sentMessage != null) {
                Integer forwardedMessageId = sentMessage.getMessageId();
                if (forwardedMessageId != null) {
                    messageIdToUserIdMap.put(forwardedMessageId, chatId);
                    System.out.println("Mapping forwarded message ID " + forwardedMessageId + " to user ID " + chatId);
                } else {
                    System.out.println("Error: forwardedMessageId is null.");
                }
            } else {
                System.out.println("Failed to forward the message to the manager.");
            }
        } catch (TelegramApiException e) {
            System.out.println("Error forwarding message to manager: " + e.getMessage());
            e.printStackTrace();
        }
    }
    private void handleManagerResponse(Message replyToMessage, String managerText) {
        try {
            if (replyToMessage == null) {
                System.out.println("ReplyToMessage is null. Cannot process manager response.");
                return;
            }

            Integer repliedMessageId = replyToMessage.getMessageId();
            System.out.println("Processing reply for Message ID: " + repliedMessageId);

            Long userId = messageIdToUserIdMap.get(repliedMessageId);

            if (userId == null) {
                System.out.println("No mapping found for Message ID: " + repliedMessageId);
                return;
            }

            if (userId.equals(MANAGER_USER_ID)) {
                System.out.println("Reply appears to be for the manager. Not forwarding to user.");
                return;
            }
            System.out.println("Found mapping: User ID = " + userId);

            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(String.valueOf(userId));
            sendMessage.setText("Reply from manager:\n\n" + managerText);

            System.out.println("Sending manager's response to user: " + managerText);

            execute(sendMessage);
        } catch (TelegramApiException e) {
            System.out.println("Error sending manager's response to user: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
