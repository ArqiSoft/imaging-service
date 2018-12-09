package sds.imaging.domain.events;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;

import sds.imaging.domain.core.Image;
import sds.messaging.contracts.AbstractContract;

public class ImageGenerationFailed extends AbstractContract {

    private UUID id;
    private String timeStamp;
    private UUID userId;
    private Image image;

    public ImageGenerationFailed() {
        namespace = "Sds.Imaging.Domain.Events";
        contractName = ImageGenerationFailed.class.getSimpleName();
    }

    /**
     * @return the id
     */
    @JsonProperty("Id")
    public UUID getId() {
        return id;
    }

    /**
     * @param id the id to set
     */
    public void setId(UUID id) {
        this.id = id;
    }

    /**
     * @return the timeStamp
     */
    @JsonProperty("TimeStamp")
    public String getTimeStamp() {
        return timeStamp;
    }

    /**
     * @param timeStamp the timeStamp to set
     */
    public void setTimeStamp(String timeStamp) {
        this.timeStamp = timeStamp;
    }

    /**
     * @return the userId
     */
    @JsonProperty("UserId")
    public UUID getUserId() {
        return userId;
    }

    /**
     * @param userId the userId to set
     */
    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    /**
     * @return the image
     */
    @JsonProperty("Image")
    public Image getImage() {
        return image;
    }

    /**
     * @param image the image to set
     */
    public void setImage(Image image) {
        this.image = image;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return String.format(
                "ImageGenerationFailed [id=%s, timeStamp=%s, userId=%s, image=%s, namespace=%s, contractName=%s, correlationId=%s]",
                id, timeStamp, userId, image, namespace, contractName, this.getCorrelationId());
    }
    
    

}
