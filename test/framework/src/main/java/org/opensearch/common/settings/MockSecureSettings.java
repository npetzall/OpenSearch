/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

/*
 * Modifications Copyright OpenSearch Contributors. See
 * GitHub history for details.
 */

package org.opensearch.common.settings;

import org.opensearch.common.hash.MessageDigests;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A mock implementation of secure settings for tests to use.
 */
public class MockSecureSettings implements SecureSettings {

    private Map<String, String> secureStrings = new HashMap<>();
    private Map<String, byte[]> files = new HashMap<>();
    private Map<String, byte[]> sha256Digests = new HashMap<>();
    private Set<String> settingNames = new HashSet<>();
    private final AtomicBoolean closed = new AtomicBoolean(false);

    public MockSecureSettings() {
    }

    private MockSecureSettings(MockSecureSettings source) {
        secureStrings.putAll(source.secureStrings);
        files.putAll(source.files);
        sha256Digests.putAll(source.sha256Digests);
        settingNames.addAll(source.settingNames);
    }

    @Override
    public boolean isLoaded() {
        return true;
    }

    @Override
    public Set<String> getSettingNames() {
        return settingNames;
    }

    @Override
    public SecureString getString(String setting) {
        ensureOpen();
        final String s = secureStrings.get(setting);
        if (s == null) {
            return null;
        }
        return new SecureString(s.toCharArray());
    }

    @Override
    public InputStream getFile(String setting) {
        ensureOpen();
        return new ByteArrayInputStream(files.get(setting));
    }

    @Override
    public byte[] getSHA256Digest(String setting) {
        return sha256Digests.get(setting);
    }

    public void setString(String setting, String value) {
        ensureOpen();
        secureStrings.put(setting, value);
        sha256Digests.put(setting, MessageDigests.sha256().digest(value.getBytes(StandardCharsets.UTF_8)));
        settingNames.add(setting);
    }

    public void setFile(String setting, byte[] value) {
        ensureOpen();
        files.put(setting, value);
        sha256Digests.put(setting, MessageDigests.sha256().digest(value));
        settingNames.add(setting);
    }

    /** Merge the given secure settings into this one. */
    public void merge(MockSecureSettings secureSettings) {
        for (String setting : secureSettings.getSettingNames()) {
            if (settingNames.contains(setting)) {
                throw new IllegalArgumentException("Cannot overwrite existing secure setting " + setting);
            }
        }
        settingNames.addAll(secureSettings.settingNames);
        secureStrings.putAll(secureSettings.secureStrings);
        sha256Digests.putAll(secureSettings.sha256Digests);
        files.putAll(secureSettings.files);
    }

    @Override
    public void close() throws IOException {
        closed.set(true);
    }

    private void ensureOpen() {
        if (closed.get()) {
            throw new IllegalStateException("secure settings are already closed");
        }
    }

    public SecureSettings clone() {
        ensureOpen();
        return new MockSecureSettings(this);
    }
}
