package io.virtualapp.addon.arm64;

import com.lody.virtual.DelegateApplication64Bit;

/**
 * @author Lody
 */
public class App extends DelegateApplication64Bit {

    @Override
    protected String get32BitPackageName() {
        return BuildConfig.PACKAGE_NAME_32BIT;
    }
}
