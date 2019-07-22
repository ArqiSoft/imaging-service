package sds.imaging.processor;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.npspot.jtransitlight.JTransitLightException;
import com.npspot.jtransitlight.consumer.ReceiverBusControl;
import com.npspot.jtransitlight.publisher.IBusControl;
import com.sds.storage.BlobInfo;
import com.sds.storage.BlobStorage;
import com.sds.storage.Guid;

import sds.imaging.domain.commands.GenerateImage;
import sds.imaging.domain.core.Image;
import sds.imaging.domain.events.ImageGenerated;
import sds.imaging.domain.events.ImageGenerationFailed;
import sds.imaging.rasterizer.RasterizationFactory;
import sds.messaging.callback.AbstractMessageProcessor;

@Component
public class ThumbnailGenerator extends AbstractMessageProcessor<GenerateImage> {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(ThumbnailGenerator.class);

    ReceiverBusControl receiver;
    IBusControl bus;
    BlobStorage storage;

    
    @Autowired
    public ThumbnailGenerator(ReceiverBusControl receiver, IBusControl bus,
            BlobStorage storage) throws JTransitLightException, IOException {
        this.bus = bus;
        this.receiver = receiver;
        this.storage = storage;
    }

    public void process(GenerateImage message) {
        
        try {
            byte[] data = storage.downloadFile(new Guid(message.getBlobId()), message.getBucket());
            final BlobInfo fileInfo = storage.getFileInfo(new Guid(message.getBlobId()), message.getBucket());
            String[] fileNameParts =  fileInfo.getFileName().split("\\.");
            
            byte[] thumbnail = RasterizationFactory
                    .getInstance(fileNameParts[fileNameParts.length - 1])
                    .rasterize(message.getImage(), data, fileNameParts[fileNameParts.length - 1]);;

                    Image img = message.getImage();
                    Map<String, Object> metadata = new HashMap<>();
                    metadata.put("SourceId", message.getBlobId());
                    storage.addFile(new Guid(img.getId()), img.getId().toString(),
                            thumbnail, img.getMimeType(), message.getBucket(), metadata);
                        
                    publishImageGeneratedEvent(message);
        } catch (Error e) {
            LOGGER.error("Image generation failed for {}: {}", message, e.getMessage());
            publishImageGenerationFailedEvent(message, e.getMessage(), true);
        }
         catch (Throwable e) {
            LOGGER.error("Image generation failed for {}: {}", message, e.getMessage());
            publishImageGenerationFailedEvent(message, e.getMessage(), true);
        }
    }

    private void publishImageGeneratedEvent(GenerateImage message) {
        ImageGenerated event = new ImageGenerated();
        event.setId(message.getId());
        event.setUserId(message.getUserId());
        event.setTimeStamp(getTimestamp());
        event.setImage(message.getImage());
        event.setBlobId(message.getImage().getId());
        event.setBucket(message.getBucket());
        event.setCorrelationId(message.getCorrelationId());
        
        LOGGER.debug("Publishing event {}", event);
        
        bus.publish(event);

    }

    private void publishImageGenerationFailedEvent(GenerateImage message, 
            String exception, boolean republishMessage) {
        ImageGenerationFailed event = new ImageGenerationFailed();
        event.setId(message.getId());
        event.setUserId(message.getUserId());
        event.setTimeStamp(getTimestamp());
        event.setImage(message.getImage());
        event.getImage().setException(exception);
        event.setCorrelationId(message.getCorrelationId());
        
        LOGGER.debug("Publishing ImageGenerationFailed event {}", event);
        
        bus.publish(event);
    }

    private String getTimestamp() {
        //("yyyy-MM-dd'T'HH:mm:ss'Z'")
        return LocalDateTime.now().toString();
    }
    
}
