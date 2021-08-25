package com.skyblockplus.utils;

import static com.skyblockplus.Main.jda;
import static com.skyblockplus.utils.Utils.scheduler;

import com.skyblockplus.utils.structs.MessageTimeoutStruct;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MessageTimeout extends ListenerAdapter {

	public static final List<MessageTimeoutStruct> messageList = new ArrayList<>();
	private static final Logger log = LoggerFactory.getLogger(MessageTimeout.class);

	public static void addMessage(Message message, Object eventListener) {
		messageList.add(new MessageTimeoutStruct(message, eventListener));
	}

	public static void removeMessage(Object eventListener) {
		for (Iterator<MessageTimeoutStruct> iteratorCur = messageList.iterator(); iteratorCur.hasNext();) {
			MessageTimeoutStruct currentMessage = iteratorCur.next();
			if (currentMessage.eventListener.equals(eventListener)) {
				iteratorCur.remove();
				break;
			}
		}
	}

	@Override
	public void onReady(@NotNull ReadyEvent event) {
		scheduler.scheduleWithFixedDelay(this::updateMessages, 1, 1, TimeUnit.MINUTES);
	}

	public void updateMessages() {
		try {
			for (Iterator<MessageTimeoutStruct> iteratorCur = messageList.iterator(); iteratorCur.hasNext();) {
				MessageTimeoutStruct currentMessageStruct = iteratorCur.next();
				Message currentMessage = currentMessageStruct.message;
				long secondsSinceLast = Instant.now().getEpochSecond() - currentMessage.getTimeCreated().toInstant().getEpochSecond();
				if (secondsSinceLast > 30) {
					currentMessage.clearReactions().queue();

					iteratorCur.remove();
					jda.removeEventListener(currentMessageStruct.eventListener);
				}
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}
}