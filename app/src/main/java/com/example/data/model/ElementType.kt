package com.example.data.model

enum class ElementType(
  val id: String,
  val displayName: String,
  val unitName: String,
  val leaderName: String,
  val labName: String,
  val specialtySummary: String,
) {
  PYRO(
    id = "pyro",
    displayName = "Pyro",
    unitName = "Pyronaut",
    leaderName = "Ignis Prime",
    labName = "Pyronaut Forge Lab",
    specialtySummary = "Melts cryo-crystal barriers, burns spore webs, and powers thermal cores.",
  ),
  HYDRO(
    id = "hydro",
    displayName = "Hydro",
    unitName = "Hydronaut",
    leaderName = "Nautilus Prime",
    labName = "Hydronaut Bio-Tank Lab",
    specialtySummary = "Extinguishes plasma jets, cools reactor locks, and bridges brine channels.",
  ),
  VOLT(
    id = "volt",
    displayName = "Volt",
    unitName = "Voltnaut",
    leaderName = "Tesla Prime",
    labName = "Voltnaut Arc Lab",
    specialtySummary = "Energizes circuit relays, overloads shield pylons, and stuns armored guardians.",
  );

  companion object {
    fun fromId(id: String): ElementType = entries.firstOrNull { it.id == id } ?: PYRO
  }
}

enum class ResourceType(
  val key: String,
  val displayName: String,
  val shortLabel: String,
  val element: ElementType,
  val description: String,
) {
  FUEL_CELLS(
    key = "fuelCells",
    displayName = "Thermal Fuel Cells",
    shortLabel = "Fuel",
    element = ElementType.PYRO,
    description = "Pressurized subterranean fuel canisters used for Pyro soldier thrusters.",
  ),
  SPARK_FLINT(
    key = "sparkFlint",
    displayName = "Spark-Flint Shards",
    shortLabel = "Flint",
    element = ElementType.PYRO,
    description = "High-friction mineral shards struck to ignite Pyronaut eggs.",
  ),
  MAGMA_MATCHES(
    key = "magmaMatches",
    displayName = "Magma-Core Matches",
    shortLabel = "Matches",
    element = ElementType.PYRO,
    description = "Self-oxidizing ignition rods for high-tier Pyro forge upgrades.",
  ),
  GLASS_JARS(
    key = "glassJars",
    displayName = "Distilled Glass Jars",
    shortLabel = "Jars",
    element = ElementType.HYDRO,
    description = "Sealed crystal vessels for collecting pure cave condensate.",
  ),
  DEW_VIALS(
    key = "dewVials",
    displayName = "Abyssal Dew Vials",
    shortLabel = "Dew",
    element = ElementType.HYDRO,
    description = "Mineral-rich bioluminescent water used to incubate Hydronaut eggs.",
  ),
  CRYO_CONDENSERS(
    key = "cryoCondensers",
    displayName = "Cryo Condensers",
    shortLabel = "Condensers",
    element = ElementType.HYDRO,
    description = "Pressure cooling cores for advanced Hydro filtration upgrades.",
  ),
  CAPACITOR_CHIPS(
    key = "capacitorChips",
    displayName = "Micro-Capacitor Chips",
    shortLabel = "Chips",
    element = ElementType.VOLT,
    description = "Salvaged silicon logic wafers from sunken dynamo vaults.",
  ),
  CAVE_BATTERIES(
    key = "caveBatteries",
    displayName = "Cave-Cell Batteries",
    shortLabel = "Batteries",
    element = ElementType.VOLT,
    description = "Compact alkaline cells that jumpstart dormant Voltnaut eggs.",
  ),
  COPPER_COILS(
    key = "copperCoils",
    displayName = "Copper Relay Coils",
    shortLabel = "Coils",
    element = ElementType.VOLT,
    description = "Conductive windings for arc-chain and overdrive circuitry.",
  );
}

enum class BiomeType(
  val id: String,
  val displayName: String,
  val description: String,
  val allowedElements: Set<ElementType>,
  val restrictionNote: String,
) {
  BASALT_RIFT(
    id = "BASALT_RIFT",
    displayName = "Volcanic Basalt Rift",
    description = "Magma fissures and cryo-choked basalt vaults.",
    allowedElements = setOf(ElementType.PYRO, ElementType.VOLT),
    restrictionNote = "Hydro Restricted: Extreme fissure heat evaporates Hydronauts.",
  ),
  CRYSTAL_CISTERN(
    id = "CRYSTAL_CISTERN",
    displayName = "Submerged Crystal Cistern",
    description = "Flooded subterranean aqueducts and brine trenches.",
    allowedElements = setOf(ElementType.HYDRO, ElementType.PYRO),
    restrictionNote = "Volt Restricted: High humidity shorts unshielded Voltnaut coils.",
  ),
  DYNAMO_RUINS(
    id = "DYNAMO_RUINS",
    displayName = "Sunken Dynamo Ruins",
    description = "Ancient powered conduits and magnetic relay mazes.",
    allowedElements = setOf(ElementType.VOLT, ElementType.HYDRO),
    restrictionNote = "Pyro Restricted: Oxygen-starved vaults suppress Pyronaut ignition.",
  ),
  FUNGAL_GROTTO(
    id = "FUNGAL_GROTTO",
    displayName = "Luminous Fungal Grotto",
    description = "Bioluminescent mycelium tunnels with mixed elemental hazards.",
    allowedElements = setOf(ElementType.PYRO, ElementType.HYDRO, ElementType.VOLT),
    restrictionNote = "All Elements Permitted: Balanced atmospheric chamber.",
  ),
  STORM_CHASM(
    id = "STORM_CHASM",
    displayName = "Abyssal Storm Chasm",
    description = "Volatile deep-core labyrinth with multi-element puzzle locks.",
    allowedElements = setOf(ElementType.PYRO, ElementType.HYDRO, ElementType.VOLT),
    restrictionNote = "All Elements Permitted: High-threat multi-element expedition.",
  );

  companion object {
    fun fromId(id: String): BiomeType = entries.firstOrNull { it.id == id } ?: FUNGAL_GROTTO
  }
}

enum class WeatherCondition(
  val id: String,
  val displayName: String,
  val effectSummary: String,
  val restrictedElement: ElementType? = null,
) {
  CLEAR(
    id = "CLEAR",
    displayName = "Calm Subterranean",
    effectSummary = "Stable air currents. No weather penalties.",
  ),
  SPORE_MIST(
    id = "SPORE_MIST",
    displayName = "Bioluminescent Spore Mist",
    effectSummary = "Dense glowing fog narrows visibility radius around units.",
  ),
  BRINE_RAIN(
    id = "BRINE_RAIN",
    displayName = "Corrosive Brine Rain",
    effectSummary = "Periodic acidic droplets chip non-Hydro squads unless upgraded.",
  ),
  ION_SURGE(
    id = "ION_SURGE",
    displayName = "Ion Surge Storm",
    effectSummary = "Empowers Volt units (+25% speed), periodically shocks non-Volt units.",
  ),
  THERMAL_UPDRAFT(
    id = "THERMAL_UPDRAFT",
    displayName = "Thermal Magma Updraft",
    effectSummary = "Boosts Pyro melt speed (+30%), slows non-Pyro regeneration.",
  );

  companion object {
    fun fromId(id: String): WeatherCondition = entries.firstOrNull { it.id == id } ?: CLEAR
  }
}

enum class UpgradeBranch(val title: String) {
  COMBAT("Combat"),
  UTILITY("Engineering"),
  SURVIVAL("Survival"),
}

data class UpgradeNodeDefinition(
  val id: String,
  val element: ElementType,
  val branch: UpgradeBranch,
  val tier: Int,
  val title: String,
  val description: String,
  val statBadge: String,
  val prerequisiteId: String? = null,
  val costs: Map<ResourceType, Int>,
)

data class LeaderSkillDefinition(
  val id: String,
  val element: ElementType,
  val name: String,
  val description: String,
  val unlockLevel: Int,
  val cooldownSeconds: Float,
  val radiusTiles: Float,
  val damage: Int,
)

object UpgradeCatalog {
  val soldierUpgrades: List<UpgradeNodeDefinition> = listOf(
    // PYRO TREE (6 deep nodes across 3 branches)
    UpgradeNodeDefinition(
      id = "pyro_combat_1",
      element = ElementType.PYRO,
      branch = UpgradeBranch.COMBAT,
      tier = 1,
      title = "Flare Mandibles",
      description = "Pyronauts deal +35% bite & ignite damage to cavern guardians.",
      statBadge = "+35% ATK",
      costs = mapOf(ResourceType.SPARK_FLINT to 3, ResourceType.FUEL_CELLS to 2),
    ),
    UpgradeNodeDefinition(
      id = "pyro_combat_2",
      element = ElementType.PYRO,
      branch = UpgradeBranch.COMBAT,
      tier = 2,
      title = "Ember Nova Burst",
      description = "Pyronauts unleash a heat shockwave on strike, dealing splash damage.",
      statBadge = "+60% ATK & Splash",
      prerequisiteId = "pyro_combat_1",
      costs = mapOf(ResourceType.MAGMA_MATCHES to 4, ResourceType.FUEL_CELLS to 5),
    ),
    UpgradeNodeDefinition(
      id = "pyro_util_1",
      element = ElementType.PYRO,
      branch = UpgradeBranch.UTILITY,
      tier = 1,
      title = "Blowtorch Core",
      description = "Melts cryo-crystal barriers and burns spore webs 45% faster.",
      statBadge = "+45% Clear Speed",
      costs = mapOf(ResourceType.FUEL_CELLS to 3, ResourceType.MAGMA_MATCHES to 2),
    ),
    UpgradeNodeDefinition(
      id = "pyro_util_2",
      element = ElementType.PYRO,
      branch = UpgradeBranch.UTILITY,
      tier = 2,
      title = "Thermal Thrusters",
      description = "Increases Pyronaut movement and resource haul speed across caverns.",
      statBadge = "+25% Move & Carry",
      prerequisiteId = "pyro_util_1",
      costs = mapOf(ResourceType.FUEL_CELLS to 6, ResourceType.SPARK_FLINT to 5),
    ),
    UpgradeNodeDefinition(
      id = "pyro_surv_1",
      element = ElementType.PYRO,
      branch = UpgradeBranch.SURVIVAL,
      tier = 1,
      title = "Basalt Carapace",
      description = "Hardened obsidian plating boosts Pyronaut max health.",
      statBadge = "+40% HP",
      costs = mapOf(ResourceType.SPARK_FLINT to 4, ResourceType.MAGMA_MATCHES to 2),
    ),
    UpgradeNodeDefinition(
      id = "pyro_surv_2",
      element = ElementType.PYRO,
      branch = UpgradeBranch.SURVIVAL,
      tier = 2,
      title = "Storm-Sealed Mantle",
      description = "Reduces Ion Surge & Brine Rain weather damage taken by 60%.",
      statBadge = "-60% Weather DMG",
      prerequisiteId = "pyro_surv_1",
      costs = mapOf(ResourceType.MAGMA_MATCHES to 5, ResourceType.FUEL_CELLS to 4),
    ),

    // HYDRO TREE
    UpgradeNodeDefinition(
      id = "hydro_combat_1",
      element = ElementType.HYDRO,
      branch = UpgradeBranch.COMBAT,
      tier = 1,
      title = "Pressure Jet",
      description = "Hydronauts fire concentrated water bursts that stagger enemies.",
      statBadge = "+35% ATK",
      costs = mapOf(ResourceType.GLASS_JARS to 3, ResourceType.DEW_VIALS to 2),
    ),
    UpgradeNodeDefinition(
      id = "hydro_combat_2",
      element = ElementType.HYDRO,
      branch = UpgradeBranch.COMBAT,
      tier = 2,
      title = "Tidal Surge Cannon",
      description = "High-pressure hydro blasts deal bonus damage to shielded bosses.",
      statBadge = "+60% ATK & Stagger",
      prerequisiteId = "hydro_combat_1",
      costs = mapOf(ResourceType.CRYO_CONDENSERS to 4, ResourceType.DEW_VIALS to 5),
    ),
    UpgradeNodeDefinition(
      id = "hydro_util_1",
      element = ElementType.HYDRO,
      branch = UpgradeBranch.UTILITY,
      tier = 1,
      title = "Rapid Siphon",
      description = "Extinguishes plasma fire jets and bridges brine channels 45% faster.",
      statBadge = "+45% Clear Speed",
      costs = mapOf(ResourceType.DEW_VIALS to 3, ResourceType.CRYO_CONDENSERS to 2),
    ),
    UpgradeNodeDefinition(
      id = "hydro_util_2",
      element = ElementType.HYDRO,
      branch = UpgradeBranch.UTILITY,
      tier = 2,
      title = "Buoyancy Sacs",
      description = "Glides effortlessly over wet stone with boosted haul speed.",
      statBadge = "+25% Move & Carry",
      prerequisiteId = "hydro_util_1",
      costs = mapOf(ResourceType.GLASS_JARS to 6, ResourceType.CRYO_CONDENSERS to 4),
    ),
    UpgradeNodeDefinition(
      id = "hydro_surv_1",
      element = ElementType.HYDRO,
      branch = UpgradeBranch.SURVIVAL,
      tier = 1,
      title = "Gelatinous Membrane",
      description = "Cushions impacts and boosts Hydronaut health reserves.",
      statBadge = "+40% HP",
      costs = mapOf(ResourceType.GLASS_JARS to 4, ResourceType.DEW_VIALS to 2),
    ),
    UpgradeNodeDefinition(
      id = "hydro_surv_2",
      element = ElementType.HYDRO,
      branch = UpgradeBranch.SURVIVAL,
      tier = 2,
      title = "Aegis Mist",
      description = "Regenerates squad HP passively in damp biomes & resists Thermal Updrafts.",
      statBadge = "HP Regen & Resist",
      prerequisiteId = "hydro_surv_1",
      costs = mapOf(ResourceType.CRYO_CONDENSERS to 5, ResourceType.DEW_VIALS to 5),
    ),

    // VOLT TREE
    UpgradeNodeDefinition(
      id = "volt_combat_1",
      element = ElementType.VOLT,
      branch = UpgradeBranch.COMBAT,
      tier = 1,
      title = "Arc Stingers",
      description = "Voltnauts discharge high-voltage bolts on contact.",
      statBadge = "+35% ATK",
      costs = mapOf(ResourceType.CAPACITOR_CHIPS to 3, ResourceType.CAVE_BATTERIES to 2),
    ),
    UpgradeNodeDefinition(
      id = "volt_combat_2",
      element = ElementType.VOLT,
      branch = UpgradeBranch.COMBAT,
      tier = 2,
      title = "Chain Lightning Lattice",
      description = "Strikes arc between nearby enemies and briefly stun armored targets.",
      statBadge = "+60% ATK & Stun",
      prerequisiteId = "volt_combat_1",
      costs = mapOf(ResourceType.COPPER_COILS to 4, ResourceType.CAPACITOR_CHIPS to 5),
    ),
    UpgradeNodeDefinition(
      id = "volt_util_1",
      element = ElementType.VOLT,
      branch = UpgradeBranch.UTILITY,
      tier = 1,
      title = "Overclocked Relays",
      description = "Powers circuit locks and disables shield pylons 45% faster.",
      statBadge = "+45% Clear Speed",
      costs = mapOf(ResourceType.CAVE_BATTERIES to 3, ResourceType.COPPER_COILS to 2),
    ),
    UpgradeNodeDefinition(
      id = "volt_util_2",
      element = ElementType.VOLT,
      branch = UpgradeBranch.UTILITY,
      tier = 2,
      title = "Superconductor Feet",
      description = "Turbocharges Voltnaut sprint velocity and whistle response.",
      statBadge = "+30% Move Speed",
      prerequisiteId = "volt_util_1",
      costs = mapOf(ResourceType.CAPACITOR_CHIPS to 6, ResourceType.COPPER_COILS to 4),
    ),
    UpgradeNodeDefinition(
      id = "volt_surv_1",
      element = ElementType.VOLT,
      branch = UpgradeBranch.SURVIVAL,
      tier = 1,
      title = "Faraday Casing",
      description = "Insulated alloy shell reinforces Voltnaut durability.",
      statBadge = "+40% HP",
      costs = mapOf(ResourceType.CAVE_BATTERIES to 4, ResourceType.CAPACITOR_CHIPS to 2),
    ),
    UpgradeNodeDefinition(
      id = "volt_surv_2",
      element = ElementType.VOLT,
      branch = UpgradeBranch.SURVIVAL,
      tier = 2,
      title = "Grounding Stabilizer",
      description = "Absorbs weather hazards to restore shield integrity.",
      statBadge = "-60% Weather DMG",
      prerequisiteId = "volt_surv_1",
      costs = mapOf(ResourceType.COPPER_COILS to 5, ResourceType.CAVE_BATTERIES to 5),
    ),
  )

  val leaderSkills: List<LeaderSkillDefinition> = listOf(
    LeaderSkillDefinition(
      id = "solar_dash",
      element = ElementType.PYRO,
      name = "Solar Dash",
      description = "Ignis Prime surges forward in a blazing arc, scorching enemies and melting nearby frost.",
      unlockLevel = 1,
      cooldownSeconds = 5f,
      radiusTiles = 2.4f,
      damage = 42,
    ),
    LeaderSkillDefinition(
      id = "magma_mortar",
      element = ElementType.PYRO,
      name = "Magma Mortar",
      description = "Launches a heavy thermal burst around Ignis Prime that shatters boss armor.",
      unlockLevel = 2,
      cooldownSeconds = 8f,
      radiusTiles = 3.2f,
      damage = 78,
    ),
    LeaderSkillDefinition(
      id = "tidal_barrier",
      element = ElementType.HYDRO,
      name = "Tidal Barrier",
      description = "Nautilus Prime emits a restorative aqua pulse that heals nearby squads and knocks foes back.",
      unlockLevel = 1,
      cooldownSeconds = 6f,
      radiusTiles = 2.8f,
      damage = 34,
    ),
    LeaderSkillDefinition(
      id = "pressure_lance",
      element = ElementType.HYDRO,
      name = "Pressure Lance",
      description = "Projects a high-pressure hydro vortex that douses flames and pierces guardians.",
      unlockLevel = 2,
      cooldownSeconds = 7.5f,
      radiusTiles = 3.1f,
      damage = 72,
    ),
    LeaderSkillDefinition(
      id = "chain_lightning",
      element = ElementType.VOLT,
      name = "Chain Lightning",
      description = "Tesla Prime discharges a high-voltage arc that stuns nearby enemies and charges relays.",
      unlockLevel = 1,
      cooldownSeconds = 5.5f,
      radiusTiles = 3.0f,
      damage = 48,
    ),
    LeaderSkillDefinition(
      id = "emp_overdrive",
      element = ElementType.VOLT,
      name = "EMP Overdrive",
      description = "Triggers a massive electromagnetic blast, stunning bosses and supercharging Voltnauts.",
      unlockLevel = 2,
      cooldownSeconds = 8.5f,
      radiusTiles = 3.6f,
      damage = 84,
    ),
  )

  fun upgradesFor(element: ElementType): List<UpgradeNodeDefinition> =
    soldierUpgrades.filter { it.element == element }

  fun skillsFor(element: ElementType): List<LeaderSkillDefinition> =
    leaderSkills.filter { it.element == element }
}
