package sds.imaging.domain.events;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;

import sds.imaging.domain.core.Image;
import sds.messaging.contracts.AbstractContract;

public class ImageGenerated extends AbstractContract {

    private UUID id;
    private UUID blobId;
    private String timeStamp;
    private UUID userId;
    private Image image;
    private String bucket;

    public ImageGenerated() {
        namespace = "Sds.Imaging.Domain.Events";
        contractName = ImageGenerated.class.getSimpleName();
    }

    /**
     * @return the id
     */
    @Override
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

    /**
     * @return the bucket
     */
    public String getBucket() {
        return bucket;
    }

    /**
     * @param bucket the bucket to set
     */
    public void setBucket(String bucket) {
        this.bucket = bucket;
    }

    
    /**
     * @return the blobId
     */
    public UUID getBlobId() {
        return blobId;
    }

    /**
     * @param blobId the blobId to set
     */
    public void setBlobId(UUID blobId) {
        this.blobId = blobId;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return String.format(
                "ImageGenerated [id=%s, timeStamp=%s, userId=%s, image=%s, namespace=%s, contractName=%s, correlationId=%s]",
                id, timeStamp, userId, image, namespace, contractName, this.getCorrelationId());
    }


    
}
