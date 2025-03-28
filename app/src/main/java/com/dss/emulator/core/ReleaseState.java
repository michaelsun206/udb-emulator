package com.dss.emulator.core;


public enum ReleaseState {
    IDLE_REQ,
    IDLE_ACK,
    INIT_REQ,
    INIT_PENDING,
    INIT_OK,
    INIT_FAIL,
    CON_REQ,
    CON_ID1,
    CON_ID2,
    CON_OK,
    RNG_SINGLE_REQ,
    RNG_SINGLE_PENDING,
    RNG_SINGLE_OK,
    RNG_SINGLE_FAIL,
    RNG_CONT_REQ,
    RNG_CONT_PENDING,
    RNG_CONT_OK,
    RNG_CONT_FAIL,
    AT_REQ,
    AT_ARM_PENDING,
    AT_ARM_OK,
    AT_ARM_FAIL,
    AT_TRG_PENDING,
    AT_TRG_OK,
    AT_TRG_FAIL,
    BCR_REQ,
    BCR_PENDING,
    BCR_OK,
    PI_QID_REQ,
    PI_QID_PENDING,
    PI_QID_DETECT,
    PI_QID_NODETECT,
    PI_ID_REQ,
    PI_ID_PENDING,
    PI_ID_DETECT,
    PI_ID_NODETECT,
    NT_REQ,
    NT_PENDING,
    NT_OK,
    RB_OK,
    RB_ACK
}
