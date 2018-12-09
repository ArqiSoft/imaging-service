package sds.imaging.processor;

import java.util.concurrent.BlockingQueue;

import sds.imaging.domain.commands.GenerateImageJmol;
import sds.messaging.callback.AbstractMessageCallback;

public class GenerateImageJmolCallback extends AbstractMessageCallback<GenerateImageJmol> {

    public GenerateImageJmolCallback(Class<GenerateImageJmol> tClass,
                BlockingQueue<GenerateImageJmol> queue) {
            super(tClass, queue);
        }
}
