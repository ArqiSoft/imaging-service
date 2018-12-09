package sds.imaging.domain.core;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Image {

    private UUID id;
    private int width;
    private int height;
    private String format;
    private String mimeType;
    private String exception;


    public Image() { }

    public Image(UUID id, int width, int height, String format, String mimeType, String exception) {
		super();
		this.id = id;
		this.width = width;
		this.height = height;
		this.format = format;
		this.mimeType = mimeType;
		this.exception = exception;
	}

	/**
     * @return the id
     */
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
     * @return the width
     */
    public int getWidth() {
        return width;
    }

    /**
     * @param width the width to set
     */
    public void setWidth(int width) {
        this.width = width;
    }

    /**
     * @return the height
     */
    public int getHeight() {
        return height;
    }

    /**
     * @param height the height to set
     */
    public void setHeight(int height) {
        this.height = height;
    }

    /**
     * @return the format
     */
    public String getFormat() {
        return format;
    }

    /**
     * @param format the format to set
     */
    public void setFormat(String format) {
        this.format = format;
    }

    /**
     * @return the mimeType
     */
    public String getMimeType() {
        return mimeType;
    }

    /**
     * @param mimeType the mimeType to set
     */
    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    /**
     * @return the exception
     */
    @JsonProperty("Exception") 
    public String getException() {
        return exception;
    }

    /**
     * @param exception the exception to set
     */
    public void setException(String exception) {
        this.exception = exception;
    }


    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return String.format("Image [id=%s, width=%s, height=%s, format=%s, mimeType=%s, exception=%s]", id, width,
                height, format, mimeType, exception);
    }
 
}
