package sds.imaging.processor;

import java.util.concurrent.BlockingQueue;

import sds.imaging.domain.commands.GenerateImage;
import sds.messaging.callback.AbstractMessageCallback;

public class GenerateImageCallback extends AbstractMessageCallback<GenerateImage> {

    public GenerateImageCallback(Class<GenerateImage> tClass,
            BlockingQueue<GenerateImage> queue) {
        super(tClass, queue);
    }

}