/*
 * ******************************************************************************
 *    Copyright 2016-2017 Spectra Logic Corporation. All Rights Reserved.
 *    Licensed under the Apache License, Version 2.0 (the "License"). You may not use
 *    this file except in compliance with the License. A copy of the License is located at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *    or in the "license" file accompanying this file.
 *    This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 *    CONDITIONS OF ANY KIND, either express or implied. See the License for the
 *    specific language governing permissions and limitations under the License.
 * ******************************************************************************
 */

package com.spectralogic.dsbrowser.gui.components.metadata;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.spectralogic.ds3client.networking.Metadata;
import org.apache.http.Header;
import org.apache.http.message.BasicHeader;
import org.junit.Test;

import java.util.List;
import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;

public class MetadataPresenterTest {

    @Test
    public void testMetadataFromDsb() {
        final Metadata metadata = genMetadata(new BasicHeader("ds3-creation-time", "2017-01-01 14:44:2T"),
                new BasicHeader("ds3-last-modified-time", "2017-01-03 14:44:2T"),
                new BasicHeader("ds3-last-access-time", "2017-01-05 14:44:2T"),
                new BasicHeader("ds3-owner", "OWNER"),
                new BasicHeader("ds3-group", "GROUP"),
                new BasicHeader("ds3-uid", "UID"),
                new BasicHeader("ds3-gid", "GID"),
                new BasicHeader("ds3-flags", "FLAG"),
                new BasicHeader("ds3-dacl", "11bwuyhdsubsjxxsakdnewufhe"),
                new BasicHeader("ds3-mode", "MODE"));
        final MetadataPresenter metadataPresenter = new MetadataPresenter();
        final ImmutableList.Builder<MetadataEntry> builder = ImmutableList.builder();
        metadataPresenter.createMetadataBuilder(metadata, builder);
        assertThat(builder, is(notNullValue()));
        assertFalse(builder.build().isEmpty());
        assertThat(builder.build().size(), is(10));
        assertThat(builder.build().get(0).getValue(), is("2017-01-01 14:44:2 "));
        assertThat(builder.build().get(1).getValue(), is("2017-01-05 14:44:2 "));
        assertThat(builder.build().get(2).getValue(), is("2017-01-03 14:44:2 "));
        assertThat(builder.build().get(3).getValue(), is("OWNER"));
        assertThat(builder.build().get(4).getValue(), is("GROUP"));
        assertThat(builder.build().get(5).getValue(), is("UID"));
        assertThat(builder.build().get(6).getValue(), is("GID"));
        assertThat(builder.build().get(7).getValue(), is("FLAG"));
        assertThat(builder.build().get(8).getValue(), is("11bwuyhdsubsjxxsakdnewufhe"));
        assertThat(builder.build().get(9).getValue(), is("MODE"));
    }

    @Test
    public void testMetadataFromServer() {
        final Metadata metadata = genMetadata(new BasicHeader("creation-time", "2017-01-01 14:44:2T"),
                new BasicHeader("last-modified-time", "2017-01-03 14:44:2T"),
                new BasicHeader("last-access-time", "2017-01-05 14:44:2T"),
                new BasicHeader("owner", "OWNER"),
                new BasicHeader("group", "GROUP"),
                new BasicHeader("uid", "UID"),
                new BasicHeader("gid", "GID"),
                new BasicHeader("flags", "FLAG"),
                new BasicHeader("dacl", "11bwuyhdsubsjxxsakdnewufhe"),
                new BasicHeader("mode", "MODE"));
        final MetadataPresenter metadataPresenter = new MetadataPresenter();
        final ImmutableList.Builder<MetadataEntry> builder = ImmutableList.builder();
        metadataPresenter.createMetadataBuilder(metadata, builder);
        assertThat(builder, is(notNullValue()));
        assertFalse(builder.build().isEmpty());
        assertThat(builder.build().size(), is(10));
    }

    private Metadata genMetadata(final Header... headers) {
        final ImmutableMultimap.Builder<String, String> mapBuilder = ImmutableMultimap.builder();
        for (final Header header : headers) {
            mapBuilder.put(header.getName(), header.getValue());
        }
        final ImmutableMultimap<String, String> map = mapBuilder.build();
        return new Metadata() {
            @Override
            public List<String> get(final String key) {
                return Lists.newArrayList(map.get(key));
            }

            @Override
            public Set<String> keys() {
                return Sets.newHashSet(map.keySet());
            }
        };
    }
}
