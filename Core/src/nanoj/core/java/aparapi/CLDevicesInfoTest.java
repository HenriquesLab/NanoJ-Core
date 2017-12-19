package nanoj.core.java.aparapi;

import org.junit.Test;

/**
 * Author: Ricardo Henriques <paxcalpt@gmail.com>
 * Date: 4/4/13
 * Time: 6:31 PM
 */
public class CLDevicesInfoTest {
    @Test
    public void testGetInfo() throws Exception {
        System.out.println(CLDevicesInfo.getInfo());
    }

    @Test
    public void testGetMemory() throws Exception {
        System.out.println("globalMemSize="+ CLDevicesInfo.getGlobalMemSizeBest());
    }
}
