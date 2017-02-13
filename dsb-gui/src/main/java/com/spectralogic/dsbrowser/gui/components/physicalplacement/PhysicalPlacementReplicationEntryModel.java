package com.spectralogic.dsbrowser.gui.components.physicalplacement;

import com.spectralogic.ds3client.models.Ds3TargetAccessControlReplication;
import com.spectralogic.ds3client.models.Quiesced;
import com.spectralogic.ds3client.models.TargetReadPreferenceType;
import com.spectralogic.ds3client.models.TargetState;

import java.util.UUID;

public class PhysicalPlacementReplicationEntryModel {
    private final Ds3TargetAccessControlReplication accessControlReplication;
    private final String adminAuthId;
    private final String adminSecretKey;
    private final String dataPathEndPoint;
    private final boolean dataPathHttps;
    private final int dataPathPort;
    private final String dataPathProxy;
    private final boolean dataPathVerifyCertificate;
    private final TargetReadPreferenceType defaultReadPreference;
    private final UUID id;
    private final String name;
    private final boolean permitGoingOutOfSync;
    private final Quiesced quiesced;
    private final String replicatedUserDefaultDataPolicy;
    private final TargetState state;

    public PhysicalPlacementReplicationEntryModel(final Ds3TargetAccessControlReplication accessControlReplication, final String adminAuthId, final String adminSecretKey, final TargetState state, final String dataPathEndPoint, final boolean dataPathHttps, final int dataPathPort, final String dataPathProxy, final boolean dataPathVerifyCertificate, final TargetReadPreferenceType defaultReadPreference, final UUID id, final String name, final boolean permitGoingOutOfSync, final Quiesced quiesced, final String replicatedUserDefaultDataPolicy) {
        this.accessControlReplication = accessControlReplication;
        this.adminAuthId = adminAuthId;
        this.adminSecretKey = adminSecretKey;
        this.dataPathEndPoint = dataPathEndPoint;
        this.state = state;
        this.dataPathHttps = dataPathHttps;
        this.dataPathPort = dataPathPort;
        this.dataPathProxy = dataPathProxy;
        this.dataPathVerifyCertificate = dataPathVerifyCertificate;
        this.defaultReadPreference = defaultReadPreference;
        this.id = id;
        this.name = name;
        this.permitGoingOutOfSync = permitGoingOutOfSync;
        this.quiesced = quiesced;
        this.replicatedUserDefaultDataPolicy = replicatedUserDefaultDataPolicy;

    }

    public Ds3TargetAccessControlReplication getAccessControlReplication() {
        return accessControlReplication;
    }

    public String getAdminAuthId() {
        return adminAuthId;
    }

    public String getAdminSecretKey() {
        return adminSecretKey;
    }

    public String getDataPathEndPoint() {
        return dataPathEndPoint;
    }

    public TargetState getState() {
        return state;
    }

    public boolean isDataPathHttps() {
        return dataPathHttps;
    }

    public int getDataPathPort() {
        return dataPathPort;
    }

    public String getDataPathProxy() {
        return dataPathProxy;
    }

    public boolean isDataPathVerifyCertificate() {
        return dataPathVerifyCertificate;
    }

    public TargetReadPreferenceType getDefaultReadPreference() {
        return defaultReadPreference;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public boolean isPermitGoingOutOfSync() {
        return permitGoingOutOfSync;
    }

    public Quiesced getQuiesced() {
        return quiesced;
    }

    public String getReplicatedUserDefaultDataPolicy() {
        return replicatedUserDefaultDataPolicy;
    }


}

