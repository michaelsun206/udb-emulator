package com.dss.udb.emulator.dsscommand

enum class StandardRequest(val value: Int) {
    GI(0), // Get ID Request
    ST(1), // Set Field Request
    GT(2), // Get Field Request
    SP(3)  // Set Protected Field
}