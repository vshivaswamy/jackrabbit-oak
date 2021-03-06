/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.oak.spi.security.authentication.external.impl.principal;

import java.security.Principal;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterators;
import org.apache.jackrabbit.api.security.principal.PrincipalManager;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.oak.spi.security.authentication.external.basic.DefaultSyncConfig;
import org.apache.jackrabbit.oak.spi.security.principal.PrincipalImpl;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Extension of the {@link ExternalGroupPrincipalProviderTest} with 'automembership'
 * configured in the {@link DefaultSyncConfig}.
 */
public class PrincipalProviderAutoMembershipTest extends ExternalGroupPrincipalProviderTest {

    private static final String AUTO_MEMBERSHIP_GROUP_ID = "testGroup" + UUID.randomUUID();
    private static final String AUTO_MEMBERSHIP_GROUP_PRINCIPAL_NAME = "p" + AUTO_MEMBERSHIP_GROUP_ID;
    private static final String NON_EXISTING_GROUP_ID = "nonExistingGroup";

    private Group autoMembershipGroup;

    @Override
    public void before() throws Exception {
        super.before();

        autoMembershipGroup = getUserManager(root).createGroup(AUTO_MEMBERSHIP_GROUP_ID, new PrincipalImpl(AUTO_MEMBERSHIP_GROUP_PRINCIPAL_NAME), null);
        root.commit();
    }

    @Override
    protected DefaultSyncConfig createSyncConfig() {
        DefaultSyncConfig syncConfig = super.createSyncConfig();
        syncConfig.user().setAutoMembership(AUTO_MEMBERSHIP_GROUP_ID, NON_EXISTING_GROUP_ID);

        return syncConfig;
    }

    @Override
    Set<Principal> getExpectedGroupPrincipals(@Nonnull String userId) throws Exception {
        return ImmutableSet.<Principal>builder()
                .addAll(super.getExpectedGroupPrincipals(userId))
                .add(autoMembershipGroup.getPrincipal()).build();
    }

    @Override
    @Test
    public void testFindPrincipalsByTypeAll() throws Exception {
        Set<? extends Principal> res = ImmutableSet.copyOf(principalProvider.findPrincipals(PrincipalManager.SEARCH_TYPE_ALL));
        // not automembership principals expected here
        assertEquals(super.getExpectedGroupPrincipals(USER_ID), res);
    }

    @Test
    public void testGetAutoMembershipPrincipal() throws Exception {
        assertNull(principalProvider.getPrincipal(autoMembershipGroup.getPrincipal().getName()));
        assertNull(principalProvider.getPrincipal(AUTO_MEMBERSHIP_GROUP_PRINCIPAL_NAME));
        assertNull(principalProvider.getPrincipal(AUTO_MEMBERSHIP_GROUP_ID));
        assertNull(principalProvider.getPrincipal(NON_EXISTING_GROUP_ID));
    }

    @Test
    public void testGetGroupPrincipals() throws Exception {
        Set<Principal> expected = getExpectedGroupPrincipals(USER_ID);

        Authorizable user = getUserManager(root).getAuthorizable(USER_ID);

        Set<java.security.acl.Group> result = principalProvider.getGroupMembership(user.getPrincipal());
        assertTrue(result.contains(autoMembershipGroup.getPrincipal()));
        assertEquals(expected, result);
    }

    @Test
    public void testGetPrincipals() throws Exception {
        Set<Principal> expected = getExpectedGroupPrincipals(USER_ID);

        Set<? extends Principal> result = principalProvider.getPrincipals(USER_ID);
        assertTrue(result.contains(autoMembershipGroup.getPrincipal()));
        assertEquals(expected, result);
    }

    @Test
    public void testFindPrincipalsByHint() throws Exception {
        List<String> hints = ImmutableList.of(
                AUTO_MEMBERSHIP_GROUP_PRINCIPAL_NAME,
                AUTO_MEMBERSHIP_GROUP_ID,
                AUTO_MEMBERSHIP_GROUP_PRINCIPAL_NAME.substring(1, 6));

        for (String hint : hints) {
            Iterator<? extends Principal> res = principalProvider.findPrincipals(hint, PrincipalManager.SEARCH_TYPE_GROUP);

            assertFalse(Iterators.contains(res, autoMembershipGroup.getPrincipal()));
            assertFalse(Iterators.contains(res, new PrincipalImpl(NON_EXISTING_GROUP_ID)));
        }
    }

    @Test
    public void testFindPrincipalsByTypeGroup() throws Exception {
        Iterator<? extends Principal> res = principalProvider.findPrincipals(PrincipalManager.SEARCH_TYPE_GROUP);

        assertFalse(Iterators.contains(res, autoMembershipGroup.getPrincipal()));
        assertFalse(Iterators.contains(res, new PrincipalImpl(NON_EXISTING_GROUP_ID)));
    }
}