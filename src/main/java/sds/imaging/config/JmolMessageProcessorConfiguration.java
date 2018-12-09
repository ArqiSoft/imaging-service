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

import sds.imaging.domain.commands.GenerateImageJmol;
import sds.imaging.processor.GenerateImageJmolCallback;
import sds.messaging.callback.AbstractMessageProcessor;

@Component
public class JmolMessageProcessorConfiguration {
    private static final Logger LOGGER = LoggerFactory.getLogger(JmolMessageProcessorConfiguration.class);
    
    @Autowired
    public JmolMessageProcessorConfiguration(IBusControl busControl, 
            ReceiverBusControl receiver,
            AbstractMessageProcessor<GenerateImageJmol> processor,
            BlockingQueue<GenerateImageJmol> queue,
            @Value("${generateImage.queueName.Jmol}") String queueName,
            @Value("${EXECUTOR_THREAD_COUNT:5}") Integer threadCount)
                    throws JTransitLightException, IOException, InterruptedException {
        
        receiver.subscribe(new GenerateImageJmol().getQueueName(), queueName, 
                ConsumerSettings.newBuilder().withDurable(true).build(), 
                new GenerateImageJmolCallback(GenerateImageJmol.class, queue));
        
        LOGGER.debug("EXECUTOR_THREAD_COUNT is set to {}", threadCount);
        
        
        Executors.newSingleThreadExecutor().submit(() -> {
            final ExecutorService threadPool = 
                    Executors.newFixedThreadPool(threadCount);
            
            while (true) {
                // wait for message
                final GenerateImageJmol message = queue.take();
                
                // submit to processing pool
                threadPool.submit(() -> processor.doProcess(message));
                Thread.sleep(10);
            }
        });
    }
    

}
