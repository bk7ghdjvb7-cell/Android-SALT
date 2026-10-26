package org.salt.savannah.security;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.PointF;
import android.graphics.Rect;
import android.media.FaceDetector;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

/** Blurs faces before a photo can be posted. The original bytes are not written. */
public final class FaceBlur {
    public static final class Prepared {
        public final byte[] publicJpeg;
        public final boolean facesFound;
        public final String reviewId;

        Prepared(byte[] publicJpeg, boolean facesFound, String reviewId) {
            this.publicJpeg = publicJpeg;
            this.facesFound = facesFound;
            this.reviewId = reviewId;
        }
    }

    private FaceBlur() {}

    public static Prepared prepare(Context context, byte[] imageBytes, boolean consentReview) {
        Bitmap decoded = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
        if (decoded == null) {
            throw new SecurityException("image rejected");
        }
        Bitmap working = toEven565(decoded);
        if (working != decoded) {
            decoded.recycle();
        }
        FaceDetector.Face[] faces = new FaceDetector.Face[8];
        int found;
        try {
            found = new FaceDetector(working.getWidth(), working.getHeight(), faces.length).findFaces(working, faces);
        } catch (RuntimeException ex) {
            working.recycle();
            throw new SecurityException("faces could not be scanned");
        }
        Bitmap output = found > 0 ? pixelate(working, faces, found) : working;
        if (output != working) {
            working.recycle();
        }
        ByteArrayOutputStream jpeg = new ByteArrayOutputStream();
        if (!output.compress(Bitmap.CompressFormat.JPEG, 85, jpeg)) {
            output.recycle();
            throw new SecurityException("image rejected");
        }
        String reviewId = null;
        if (consentReview && found > 0) {
            reviewId = storeEncryptedReview(context, imageBytes);
        }
        output.recycle();
        return new Prepared(jpeg.toByteArray(), found > 0, reviewId);
    }

    private static Bitmap toEven565(Bitmap source) {
        int width = source.getWidth() % 2 == 0 ? source.getWidth() : source.getWidth() - 1;
        int height = source.getHeight();
        if (width < 2 || height < 2) {
            throw new SecurityException("image rejected");
        }
        Bitmap copy = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        Canvas canvas = new Canvas(copy);
        canvas.drawBitmap(source, new Rect(0, 0, width, height), new Rect(0, 0, width, height), null);
        return copy;
    }

    private static Bitmap pixelate(Bitmap source, FaceDetector.Face[] faces, int count) {
        Bitmap output = source.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(output);
        PointF point = new PointF();
        for (int i = 0; i < count; i++) {
            faces[i].getMidPoint(point);
            float eyes = faces[i].eyesDistance();
            int size = Math.max(8, (int) (eyes * 2.4f));
            int left = clamp((int) point.x - size, 0, output.getWidth() - 1);
            int top = clamp((int) point.y - size, 0, output.getHeight() - 1);
            int right = clamp((int) point.x + size, left + 1, output.getWidth());
            int bottom = clamp((int) point.y + size, top + 1, output.getHeight());
            Bitmap patch = Bitmap.createBitmap(output, left, top, right - left, bottom - top);
            Bitmap small = Bitmap.createScaledBitmap(patch, Math.max(1, patch.getWidth() / 14), Math.max(1, patch.getHeight() / 14), true);
            Bitmap pixelated = Bitmap.createScaledBitmap(small, patch.getWidth(), patch.getHeight(), false);
            canvas.drawBitmap(pixelated, left, top, null);
            patch.recycle();
            small.recycle();
            pixelated.recycle();
        }
        return output;
    }

    private static String storeEncryptedReview(Context context, byte[] original) {
        try {
            byte[] key = new byte[32];
            new SecureRandom().nextBytes(key);
            String token = SaltCrypto.seal(original, key);
            String id = SaltCrypto.sha512Hex(token.getBytes(StandardCharsets.UTF_8)).substring(0, 32);
            writePrivate(context, "consent-" + id + ".txt", token.getBytes(StandardCharsets.UTF_8));
            writePrivate(context, "consent-" + id + ".key", key);
            return id;
        } catch (Exception ex) {
            throw new SecurityException("consent review could not be sealed");
        }
    }

    private static void writePrivate(Context context, String name, byte[] value) throws Exception {
        try (FileOutputStream out = context.openFileOutput(name, Context.MODE_PRIVATE)) {
            out.write(value);
        }
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
