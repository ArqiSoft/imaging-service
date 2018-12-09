package sds.imaging.config;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.npspot.jtransitlight.JTransitLightException;
import com.npspot.jtransitlight.consumer.ReceiverBusControl;
import com.npspot.jtransitlight.consumer.setting.ConsumerSettings;
import com.npspot.jtransitlight.publisher.IBusControl;

import sds.imaging.domain.commands.GenerateImage;
import sds.imaging.processor.GenerateImageCallback;
import sds.messaging.callback.AbstractMessageProcessor;

@Component
public class MessageProcessorConfiguration {
    private static final Logger LOGGER = LoggerFactory.getLogger(MessageProcessorConfiguration.class);

    
    @Autowired
    public MessageProcessorConfiguration(IBusControl busControl, 
            ReceiverBusControl receiver, 
            AbstractMessageProcessor<GenerateImage> processor,
            BlockingQueue<GenerateImage> queue,
            @Value("${generateImage.queueName}") String queueName,
            @Value("${EXECUTOR_THREAD_COUNT:5}") Integer threadCount) 
                    throws JTransitLightException, IOException, InterruptedException {
        
        receiver.subscribe(new GenerateImage().getQueueName(), queueName, 
                ConsumerSettings.newBuilder().withDurable(true).build(), 
                new GenerateImageCallback(GenerateImage.class, queue));
        
        LOGGER.debug("EXECUTOR_THREAD_COUNT is set to {}", threadCount);
        
        
        Executors.newSingleThreadExecutor().submit(() -> {
            final ExecutorService threadPool = 
                    Executors.newFixedThreadPool(threadCount);
            
            while (true) {
                // wait for message
                final GenerateImage message = queue.take();
                
                // submit to processing pool
                threadPool.submit(() -> processor.doProcess(message));
                Thread.sleep(10);
            }
        });
    }
    
    
}
