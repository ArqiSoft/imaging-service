package sds.imaging.rasterizer.exception;

public class RasterizationException extends RuntimeException {
    private static final long serialVersionUID = 6481809394016884692L;
    
    public RasterizationException() {
        super();
    }

    public RasterizationException(String message) {
        super(message);
    }

    public RasterizationException(String message, Throwable cause) {
        super(message, cause);
    }

    public RasterizationException(Throwable cause) {
        super(cause);
    }    

}
