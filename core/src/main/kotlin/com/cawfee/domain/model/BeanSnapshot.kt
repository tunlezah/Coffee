package com.cawfee.domain.model

/** Immutable bean info passed to the rules engine. Ported from BeanSnapshot.swift. */
data class BeanSnapshot(
    val id: String,                 // stable slug
    val name: String,
    val roaster: String,
    val roastLevel: RoastLevel,
    val milkFriendly: Boolean,
    val flavourNotes: List<String>,
    val recommendedSettings: MachineSettings,
)
