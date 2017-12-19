package nanoj.core.java.tools;

/**
 * Author: Ricardo Henriques <paxcalpt@gmail.com>
 * Date: 4/10/13
 * Time: 1:20 PM
 */
public class Exceptions {

    public static class ZeroOrSaturationFoundException extends Exception {
        public ZeroOrSaturationFoundException() { super(); }
        public ZeroOrSaturationFoundException(String message) { super(message); }
        public ZeroOrSaturationFoundException(String message, Throwable cause) { super(message, cause); }
        public ZeroOrSaturationFoundException(Throwable cause) { super(cause); }
    }

    public static class WrongArraySize extends Exception {
        public WrongArraySize() { super(); }
        public WrongArraySize(String message) { super(message); }
        public WrongArraySize(String message, Throwable cause) { super(message, cause); }
        public WrongArraySize(Throwable cause) { super(cause); }
    }
}
