const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validProfile(uid, serverTimestamp) {
  return {
    userId: uid,
    callsign: "Commander Alpha",
    highestTutorialCleared: 1,
    totalRescues: 3,
    totalExpeditions: 1,
    bossesDefeated: 0,
    fuelCells: 10,
    sparkFlint: 8,
    magmaMatches: 5,
    glassJars: 0,
    dewVials: 0,
    cryoCondensers: 0,
    capacitorChips: 0,
    caveBatteries: 0,
    copperCoils: 0,
    pyroEggs: 2,
    hydroEggs: 0,
    voltEggs: 0,
    createdAt: serverTimestamp,
    updatedAt: serverTimestamp,
  };
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user cannot read or write profiles, labs, leaders, or expeditions", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("labs").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("leaders").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("expeditions").get());
});

test("Authenticated owner can create and update their own CommanderProfile", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date();
  const docRef = aliceDb.collection("users").doc(ALICE_UID);

  await assertSucceeds(docRef.set(validProfile(ALICE_UID, now)));
  await assertSucceeds(docRef.get());
  await assertSucceeds(
    docRef.update({
      fuelCells: 25,
      updatedAt: new Date(),
    })
  );
});

test("Shadow update with ghost field or spoofed userId fails on CommanderProfile", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date();
  const docRef = aliceDb.collection("users").doc(ALICE_UID);

  await assertFails(
    docRef.set({
      ...validProfile(ALICE_UID, now),
      isAdmin: true,
    })
  );
  await assertFails(docRef.set(validProfile(BOB_UID, now)));
});

test("Cross-user isolation: Bob cannot read or write Alice's labs, leaders, or expeditions", async () => {
  const now = new Date();
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection("users").doc(ALICE_UID).set(validProfile(ALICE_UID, now));
    await db.collection("users").doc(ALICE_UID).collection("labs").doc("pyro").set({
      userId: ALICE_UID,
      elementId: "pyro",
      unlocked: true,
      labTier: 1,
      unlockedUpgrades: ["thermal_shell"],
      createdAt: now,
      updatedAt: now,
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("labs").doc("pyro").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("labs").get());
});

test("Owner can manage labs, leaders, and append-only expeditions with strict schema validation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date();

  const labRef = aliceDb.collection("users").doc(ALICE_UID).collection("labs").doc("pyro");
  await assertSucceeds(
    labRef.set({
      userId: ALICE_UID,
      elementId: "pyro",
      unlocked: true,
      labTier: 1,
      unlockedUpgrades: [],
      createdAt: now,
      updatedAt: now,
    })
  );
  // Mismatched doc id vs elementId must fail
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("labs").doc("hydro").set({
      userId: ALICE_UID,
      elementId: "pyro",
      unlocked: true,
      labTier: 1,
      unlockedUpgrades: [],
      createdAt: now,
      updatedAt: now,
    })
  );

  const leaderRef = aliceDb.collection("users").doc(ALICE_UID).collection("leaders").doc("pyro");
  await assertSucceeds(
    leaderRef.set({
      userId: ALICE_UID,
      elementId: "pyro",
      name: "Ignis Prime",
      unlocked: true,
      level: 1,
      xp: 0,
      maxHp: 180,
      attackPower: 22,
      unlockedSkills: ["solar_dash"],
      createdAt: now,
      updatedAt: now,
    })
  );

  const expRef = aliceDb.collection("users").doc(ALICE_UID).collection("expeditions").doc("exp_1");
  await assertSucceeds(
    expRef.set({
      userId: ALICE_UID,
      sectorId: "tut_01",
      sectorName: "Ember Awakening",
      seed: 101,
      biome: "BASALT_RIFT",
      weather: "CLEAR",
      rescuedCount: 2,
      totalPods: 2,
      bossDefeated: false,
      victory: true,
      rank: "S",
      durationSeconds: 145,
      createdAt: now,
    })
  );
  // Expeditions are append-only (no update)
  await assertFails(expRef.update({ rank: "A" }));
});
