package java.com.github.pwittchen.infinitescroll.app;

import ohos.aafwk.ability.delegation.AbilityDelegatorRegistry;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

/**
 * ExampleOhosTest.
 */
public class ExampleOhosTest {
    @Test
    public void testBundleName() {
        final String actualBundleName = AbilityDelegatorRegistry.getArguments().getTestBundleName();
        assertEquals("java.com.github.pwittchen.infinitescroll.app", actualBundleName);
    }
}