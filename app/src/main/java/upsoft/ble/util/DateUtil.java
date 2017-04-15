package upsoft.ble.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Date Util
 */
public class DateUtil {
    private static final ThreadLocal<SimpleDateFormat> mFormater = new ThreadLocal<SimpleDateFormat>() {
        private static final String DATE_PATTERN = "yyyy-MM-dd HH:mm:ss.SSS";
        @Override
        protected SimpleDateFormat initialValue() {
            return new SimpleDateFormat(DATE_PATTERN, Locale.ENGLISH);
        }
    };

    private static final ThreadLocal<SimpleDateFormat> mFilenameFormater = new ThreadLocal<SimpleDateFormat>() {
        private static final String FILENAME_PATTERN = "yyyy-MMdd-HH-mm-ss";
        @Override
        protected SimpleDateFormat initialValue() {
            return new SimpleDateFormat(FILENAME_PATTERN, Locale.ENGLISH);
        }
    };

    /** get "yyyy-MM-dd HH:mm:ss.SSS" String */
    public static String get_yyyyMMddHHmmssSSS(long ms) {
        return mFormater.get().format(new Date(ms));
    }

    /** get "yyyy-MMdd-HH-mm-ss.csv" String */
    public static String get_nowCsvFilename() {
        return mFilenameFormater.get().format(new Date()) + ".csv";
    }

    private DateUtil() {
        // Util class
    }
}
