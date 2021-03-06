/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.language.nativeplatform.internal.toolchains;

import org.gradle.api.model.ObjectFactory;
import org.gradle.language.cpp.CppPlatform;
import org.gradle.language.cpp.internal.DefaultCppPlatform;
import org.gradle.language.swift.SwiftPlatform;
import org.gradle.language.swift.internal.DefaultSwiftPlatform;
import org.gradle.model.internal.registry.ModelRegistry;
import org.gradle.nativeplatform.OperatingSystemFamily;
import org.gradle.nativeplatform.platform.NativePlatform;
import org.gradle.nativeplatform.platform.internal.Architectures;
import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform;
import org.gradle.nativeplatform.toolchain.internal.NativeLanguage;
import org.gradle.nativeplatform.toolchain.internal.NativeToolChainInternal;
import org.gradle.nativeplatform.toolchain.internal.NativeToolChainRegistryInternal;
import org.gradle.nativeplatform.toolchain.internal.PlatformToolProvider;

import javax.inject.Inject;

public class DefaultToolChainSelector implements ToolChainSelector {
    private final ModelRegistry modelRegistry;
    private final ObjectFactory objectFactory;
    private DefaultNativePlatform host;

    @Inject
    public DefaultToolChainSelector(ModelRegistry modelRegistry, ObjectFactory objectFactory) {
        this.modelRegistry = modelRegistry;
        this.objectFactory = objectFactory;
        this.host = DefaultNativePlatform.host();
    }

    @Override
    public <T extends NativePlatform> Result<T> select(Class<T> platformType) {
        DefaultNativePlatform targetMachine = host;
        OperatingSystemFamily operatingSystemFamily = objectFactory.named(OperatingSystemFamily.class, targetMachine.getOperatingSystem().toFamilyName());

        // TODO - push all this stuff down to the tool chain and let it create the specific platform and provider

        NativeLanguage sourceLanguage = platformType == SwiftPlatform.class ? NativeLanguage.SWIFT : NativeLanguage.CPP;
        NativeToolChainRegistryInternal registry = modelRegistry.realize("toolChains", NativeToolChainRegistryInternal.class);
        NativeToolChainInternal toolChain = registry.getForPlatform(sourceLanguage, targetMachine);
        // TODO - don't select again here, as the selection is already performed to select the toolchain
        PlatformToolProvider toolProvider = toolChain.select(sourceLanguage, targetMachine);

        if (!toolProvider.isAvailable() && operatingSystemFamily.isWindows() && targetMachine.getArchitecture().isAmd64()) {
            // Try building x86 on Windows. Don't do this for other operating systems (yet)
            DefaultNativePlatform x86platformRequest = targetMachine.withArchitecture(Architectures.of(Architectures.X86));
            NativeToolChainInternal x86ToolChain = registry.getForPlatform(sourceLanguage, x86platformRequest);
            // TODO - don't select again here, as the selection is already performed to select the toolchain
            PlatformToolProvider x86ToolProvider = x86ToolChain.select(sourceLanguage, x86platformRequest);
            if (x86ToolProvider.isAvailable()) {
                targetMachine = x86platformRequest;
                toolChain = x86ToolChain;
                toolProvider = x86ToolProvider;
            }
        }

        // TODO - use a better name for the platforms, rather than "host"

        final T targetPlatform;
        if (CppPlatform.class.isAssignableFrom(platformType)) {
            targetPlatform = platformType.cast(new DefaultCppPlatform("host", operatingSystemFamily, targetMachine));
        } else if (SwiftPlatform.class.isAssignableFrom(platformType)) {
            targetPlatform = platformType.cast(new DefaultSwiftPlatform("host", operatingSystemFamily, targetMachine));
        } else {
            throw new IllegalArgumentException("Unknown type of platform " + platformType);
        }
        return new DefaultResult<T>(toolChain, toolProvider, targetPlatform);
    }

    class DefaultResult<T extends NativePlatform> implements Result<T> {
        private final NativeToolChainInternal toolChain;
        private final PlatformToolProvider platformToolProvider;
        private final T targetPlatform;

        DefaultResult(NativeToolChainInternal toolChain, PlatformToolProvider platformToolProvider, T targetPlatform) {
            this.toolChain = toolChain;
            this.platformToolProvider = platformToolProvider;
            this.targetPlatform = targetPlatform;
        }

        @Override
        public NativeToolChainInternal getToolChain() {
            return toolChain;
        }

        @Override
        public T getTargetPlatform() {
            return targetPlatform;
        }

        @Override
        public PlatformToolProvider getPlatformToolProvider() {
            return platformToolProvider;
        }
    }
}
