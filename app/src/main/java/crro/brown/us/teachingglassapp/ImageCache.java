package crro.brown.us.teachingglassapp;

import android.graphics.Bitmap;
import android.support.v4.util.LruCache;

/**
 * Created by David on 2/26/15.
 */
public class ImageCache extends LruCache<String, Bitmap> {

    /**
     * @param maxSize for caches that do not override {@link #sizeOf}, this is
     *                the maximum number of entries in the cache. For all other caches,
     *                this is the maximum sum of the sizes of the entries in this cache.
     */
    public ImageCache(int maxSize) {
        super(maxSize);
    }

    @Override
    protected int sizeOf( String key, Bitmap value ) {
        return value.getByteCount();
    }

    @Override
    protected void entryRemoved( boolean evicted, String key, Bitmap oldValue, Bitmap newValue ) {
        oldValue.recycle();
    }
}
