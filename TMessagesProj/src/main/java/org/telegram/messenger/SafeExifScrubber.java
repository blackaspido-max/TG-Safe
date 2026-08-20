package org.telegram.messenger;

import android.content.ContentResolver;
import android.net.Uri;
import android.text.TextUtils;
import android.webkit.MimeTypeMap;

import androidx.exifinterface.media.ExifInterface;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Locale;

/**
 * Creates a private cache copy of an outgoing image and strips privacy-sensitive
 * EXIF fields from that copy. The user's original file is never modified.
 *
 * This is intentionally fail-closed for image documents: if a candidate image
 * cannot be copied and scrubbed, callers can block the send rather than fall
 * back to the original file.
 */
public final class SafeExifScrubber {

    public static final class Result {
        public final boolean candidateImage;
        public final boolean success;
        public final String path;
        public final Uri uri;

        private Result(boolean candidateImage, boolean success, String path, Uri uri) {
            this.candidateImage = candidateImage;
            this.success = success;
            this.path = path;
            this.uri = uri;
        }

        public static Result pass(String path, Uri uri) {
            return new Result(false, true, path, uri);
        }

        public static Result ok(String path) {
            return new Result(true, true, path, null);
        }

        public static Result blocked() {
            return new Result(true, false, null, null);
        }
    }

    private SafeExifScrubber() {
    }

    public static Result sanitizeImageDocument(String path, Uri uri, String mime) {
        if (!SafeMode.scrubExif()) {
            return Result.pass(path, uri);
        }

        final String extension = resolveExtension(path, uri, mime);
        final String effectiveMime = resolveMime(path, uri, mime, extension);
        if (!isExifCandidate(extension, effectiveMime)) {
            return Result.pass(path, uri);
        }

        File output = null;
        try {
            final File cacheDir = FileLoader.getDirectory(FileLoader.MEDIA_DIR_CACHE);
            if (cacheDir == null) {
                return Result.blocked();
            }
            final String safeExtension = TextUtils.isEmpty(extension) ? "jpg" : extension.toLowerCase(Locale.US);
            output = new File(cacheDir, "tgsafe_scrub_" + System.nanoTime() + "." + safeExtension);
            copyTo(path, uri, output);
            if (!output.isFile() || output.length() == 0) {
                deleteQuietly(output);
                return Result.blocked();
            }

            ExifInterface exif = new ExifInterface(output.getAbsolutePath());
            clearPrivacyTags(exif);
            exif.saveAttributes();
            return Result.ok(output.getAbsolutePath());
        } catch (Throwable t) {
            FileLog.e(t);
            deleteQuietly(output);
            return Result.blocked();
        }
    }

    private static void copyTo(String path, Uri uri, File output) throws Exception {
        InputStream input = null;
        OutputStream out = null;
        try {
            if (!TextUtils.isEmpty(path)) {
                File source = new File(path);
                if (!source.isFile()) {
                    throw new IllegalStateException("TG Safe EXIF source path is not a file");
                }
                input = new FileInputStream(source);
            } else if (uri != null) {
                input = ApplicationLoader.applicationContext.getContentResolver().openInputStream(uri);
                if (input == null) {
                    throw new IllegalStateException("TG Safe EXIF source URI could not be opened");
                }
            } else {
                throw new IllegalStateException("TG Safe EXIF source is empty");
            }

            out = new FileOutputStream(output);
            byte[] buffer = new byte[128 * 1024];
            int read;
            while ((read = input.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            out.flush();
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (Throwable ignore) {
                }
            }
            if (out != null) {
                try {
                    out.close();
                } catch (Throwable ignore) {
                }
            }
        }
    }

    private static void clearPrivacyTags(ExifInterface exif) {
        String[] tags = new String[] {
                ExifInterface.TAG_GPS_LATITUDE,
                ExifInterface.TAG_GPS_LATITUDE_REF,
                ExifInterface.TAG_GPS_LONGITUDE,
                ExifInterface.TAG_GPS_LONGITUDE_REF,
                ExifInterface.TAG_GPS_ALTITUDE,
                ExifInterface.TAG_GPS_ALTITUDE_REF,
                ExifInterface.TAG_GPS_TIMESTAMP,
                ExifInterface.TAG_GPS_DATESTAMP,
                ExifInterface.TAG_GPS_PROCESSING_METHOD,
                ExifInterface.TAG_DATETIME,
                ExifInterface.TAG_DATETIME_ORIGINAL,
                ExifInterface.TAG_DATETIME_DIGITIZED,
                ExifInterface.TAG_MAKE,
                ExifInterface.TAG_MODEL,
                ExifInterface.TAG_SOFTWARE,
                ExifInterface.TAG_ARTIST,
                ExifInterface.TAG_COPYRIGHT,
                ExifInterface.TAG_IMAGE_DESCRIPTION,
                ExifInterface.TAG_IMAGE_UNIQUE_ID,
                ExifInterface.TAG_USER_COMMENT,
                ExifInterface.TAG_BODY_SERIAL_NUMBER,
                ExifInterface.TAG_CAMERA_OWNER_NAME,
                ExifInterface.TAG_LENS_MAKE,
                ExifInterface.TAG_LENS_MODEL,
                ExifInterface.TAG_LENS_SERIAL_NUMBER,
                ExifInterface.TAG_MAKER_NOTE
        };

        for (String tag : tags) {
            try {
                exif.setAttribute(tag, null);
            } catch (Throwable ignore) {
                // Some containers do not support every EXIF tag. Continue and
                // still clear every supported privacy-sensitive field.
            }
        }

        // AndroidX versions differ in how XMP/offset/subsecond tags are exposed.
        // Use their stable EXIF key strings so the scrubber remains buildable
        // across Telegram's pinned ExifInterface version.
        String[] optionalTags = new String[] {
                "Xmp",
                "OffsetTime",
                "OffsetTimeOriginal",
                "OffsetTimeDigitized",
                "SubSecTime",
                "SubSecTimeOriginal",
                "SubSecTimeDigitized",
                "GPSAreaInformation",
                "GPSDestBearing",
                "GPSDestBearingRef",
                "GPSDestDistance",
                "GPSDestDistanceRef",
                "GPSImgDirection",
                "GPSImgDirectionRef",
                "GPSSpeed",
                "GPSSpeedRef"
        };
        for (String tag : optionalTags) {
            try {
                exif.setAttribute(tag, null);
            } catch (Throwable ignore) {
            }
        }
    }

    private static String resolveMime(String path, Uri uri, String mime, String extension) {
        if (!TextUtils.isEmpty(mime)) {
            return mime.toLowerCase(Locale.US);
        }
        if (uri != null) {
            try {
                ContentResolver resolver = ApplicationLoader.applicationContext.getContentResolver();
                String type = resolver.getType(uri);
                if (!TextUtils.isEmpty(type)) {
                    return type.toLowerCase(Locale.US);
                }
            } catch (Throwable ignore) {
            }
        }
        if (!TextUtils.isEmpty(extension)) {
            String type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.toLowerCase(Locale.US));
            if (!TextUtils.isEmpty(type)) {
                return type.toLowerCase(Locale.US);
            }
        }
        return "";
    }

    private static String resolveExtension(String path, Uri uri, String mime) {
        String extension = extensionFromString(path);
        if (TextUtils.isEmpty(extension) && uri != null) {
            extension = extensionFromString(uri.getLastPathSegment());
        }
        if (TextUtils.isEmpty(extension) && !TextUtils.isEmpty(mime)) {
            extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mime);
        }
        return extension == null ? "" : extension.toLowerCase(Locale.US);
    }

    private static String extensionFromString(String value) {
        if (TextUtils.isEmpty(value)) {
            return "";
        }
        int query = value.indexOf('?');
        if (query >= 0) {
            value = value.substring(0, query);
        }
        int slash = Math.max(value.lastIndexOf('/'), value.lastIndexOf('\\'));
        int dot = value.lastIndexOf('.');
        if (dot <= slash || dot == value.length() - 1) {
            return "";
        }
        return value.substring(dot + 1);
    }

    private static boolean isExifCandidate(String extension, String mime) {
        if (!TextUtils.isEmpty(mime) && mime.startsWith("image/")) {
            if (mime.equals("image/gif") || mime.equals("image/svg+xml")) {
                return false;
            }
            return true;
        }
        return extension.equals("jpg")
                || extension.equals("jpeg")
                || extension.equals("jpe")
                || extension.equals("png")
                || extension.equals("webp")
                || extension.equals("heic")
                || extension.equals("heif");
    }

    private static void deleteQuietly(File file) {
        if (file != null) {
            try {
                file.delete();
            } catch (Throwable ignore) {
            }
        }
    }
}
