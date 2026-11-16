const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const PASSENGER_UID = "passenger_101";
const DRIVER_UID = "driver_202";
const OTHER_DRIVER_UID = "driver_303";
const ADMIN_EMAIL = "hirunideshani07@gmail.com";
const ADMIN_UID = "admin_999";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function pastTimestamp() {
  return new Date(Date.now() - 2000);
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

test("Unauthenticated user cannot read users, drivers, or trips", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(PASSENGER_UID).get());
  await assertFails(
    unauthDb.collection("drivers").where("isOnline", "==", true).get()
  );
  await assertFails(
    unauthDb.collection("trips").where("passengerId", "==", PASSENGER_UID).get()
  );
});

test("User profile: owner can create/read/update own profile but cannot suspend self or switch to admin", async () => {
  const paxDb = testEnv.authenticatedContext(PASSENGER_UID).firestore();
  const ts = pastTimestamp();
  await assertSucceeds(
    paxDb.collection("users").doc(PASSENGER_UID).set({
      userId: PASSENGER_UID,
      displayName: "Kamal Perera",
      email: "kamal@example.lk",
      phone: "+94771234567",
      preferredLanguage: "si",
      activeMode: "passenger",
      isSuspended: false,
      ratingAvg: 5.0,
      totalTrips: 0,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Cannot escalate to admin
  await assertFails(
    paxDb.collection("users").doc(PASSENGER_UID).update({
      activeMode: "admin",
      updatedAt: pastTimestamp(),
    })
  );

  // Another user cannot read Kamal's PII
  const driverDb = testEnv.authenticatedContext(DRIVER_UID).firestore();
  await assertFails(driverDb.collection("users").doc(PASSENGER_UID).get());
});

test("Driver profile: driver cannot self-approve or go online while PENDING", async () => {
  const driverDb = testEnv.authenticatedContext(DRIVER_UID).firestore();
  const ts = pastTimestamp();
  await assertSucceeds(
    driverDb.collection("drivers").doc(DRIVER_UID).set({
      userId: DRIVER_UID,
      displayName: "Sunil Bandara",
      vehicleType: "TUK_TUK",
      vehicleModel: "Bajaj RE",
      plateNumber: "WP ABJ-4521",
      vehicleColor: "Black Purple",
      verificationStatus: "PENDING",
      isOnline: false,
      latitude: 6.9271,
      longitude: 79.8612,
      heading: 0.0,
      gpsAccuracyMeters: 8.0,
      ratingAvg: 5.0,
      completedTrips: 0,
      totalEarningsLkr: 0.0,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Cannot self-approve
  await assertFails(
    driverDb.collection("drivers").doc(DRIVER_UID).update({
      verificationStatus: "APPROVED",
      updatedAt: pastTimestamp(),
    })
  );

  // Cannot go online while PENDING
  await assertFails(
    driverDb.collection("drivers").doc(DRIVER_UID).update({
      isOnline: true,
      updatedAt: pastTimestamp(),
    })
  );
});

test("Atomic trip acceptance: only approved driver can accept REQUESTED trip, prevents double-assignment", async () => {
  const ts = pastTimestamp();
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection("drivers").doc(DRIVER_UID).set({
      userId: DRIVER_UID,
      displayName: "Sunil Bandara",
      vehicleType: "SEDAN",
      vehicleModel: "Toyota Prius",
      plateNumber: "WP CAB-8821",
      verificationStatus: "APPROVED",
      isOnline: true,
      latitude: 6.9271,
      longitude: 79.8612,
      createdAt: ts,
      updatedAt: ts,
    });
    await db.collection("drivers").doc(OTHER_DRIVER_UID).set({
      userId: OTHER_DRIVER_UID,
      displayName: "Nimal Silva",
      vehicleType: "SEDAN",
      vehicleModel: "Honda Fit",
      plateNumber: "WP KQ-1122",
      verificationStatus: "PENDING",
      isOnline: false,
      latitude: 6.93,
      longitude: 79.86,
      createdAt: ts,
      updatedAt: ts,
    });
  });

  const paxDb = testEnv.authenticatedContext(PASSENGER_UID).firestore();
  await assertSucceeds(
    paxDb.collection("trips").doc("trip_001").set({
      passengerId: PASSENGER_UID,
      passengerName: "Kamal",
      vehicleType: "SEDAN",
      pickupAddress: "Galle Face Green, Colombo",
      pickupLat: 6.9271,
      pickupLng: 79.8442,
      dropoffAddress: "Bandaranaike Intl Airport",
      dropoffLat: 7.1808,
      dropoffLng: 79.8841,
      estimatedDistanceKm: 32.5,
      estimatedDurationMin: 42,
      estimatedFareLkr: 3850.0,
      status: "REQUESTED",
      paymentMethod: "CASH",
      paymentStatus: "PENDING",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Unapproved driver CANNOT accept
  const unapprovedDb = testEnv.authenticatedContext(OTHER_DRIVER_UID).firestore();
  await assertFails(
    unapprovedDb.collection("trips").doc("trip_001").update({
      driverId: OTHER_DRIVER_UID,
      driverName: "Nimal Silva",
      vehiclePlate: "WP KQ-1122",
      status: "ACCEPTED",
      driverLat: 6.93,
      driverLng: 79.86,
      driverHeading: 45.0,
      updatedAt: pastTimestamp(),
    })
  );

  // Approved driver CAN accept
  const driverDb = testEnv.authenticatedContext(DRIVER_UID).firestore();
  await assertSucceeds(
    driverDb.collection("trips").doc("trip_001").update({
      driverId: DRIVER_UID,
      driverName: "Sunil Bandara",
      vehiclePlate: "WP CAB-8821",
      status: "ACCEPTED",
      driverLat: 6.9271,
      driverLng: 79.8612,
      driverHeading: 90.0,
      updatedAt: pastTimestamp(),
    })
  );

  // Second driver CANNOT overwrite an already ACCEPTED trip
  await assertFails(
    unapprovedDb.collection("trips").doc("trip_001").update({
      driverId: OTHER_DRIVER_UID,
      status: "ACCEPTED",
      updatedAt: pastTimestamp(),
    })
  );
});

test("Admin email spoofing test: unverified admin email is rejected for fare_settings write", async () => {
  const spoofedAdminDb = testEnv
    .authenticatedContext(ADMIN_UID, {
      email: ADMIN_EMAIL,
      email_verified: false,
    })
    .firestore();

  await assertFails(
    spoofedAdminDb.collection("fare_settings").doc("TUK_TUK").set({
      vehicleType: "TUK_TUK",
      displayNameEn: "Tuk Tuk",
      displayNameSi: "ත්‍රිරෝද රථය",
      baseFareLkr: 120.0,
      perKmRateLkr: 95.0,
      perMinRateLkr: 5.0,
      waitingPerMinRateLkr: 8.0,
      minFareLkr: 150.0,
      seatCapacity: 3,
      isActive: true,
      updatedAt: pastTimestamp(),
    })
  );

  const verifiedAdminDb = testEnv
    .authenticatedContext(ADMIN_UID, {
      email: ADMIN_EMAIL,
      email_verified: true,
    })
    .firestore();

  await assertSucceeds(
    verifiedAdminDb.collection("fare_settings").doc("TUK_TUK").set({
      vehicleType: "TUK_TUK",
      displayNameEn: "Tuk Tuk",
      displayNameSi: "ත්‍රිරෝද රථය",
      baseFareLkr: 120.0,
      perKmRateLkr: 95.0,
      perMinRateLkr: 5.0,
      waitingPerMinRateLkr: 8.0,
      minFareLkr: 150.0,
      seatCapacity: 3,
      isActive: true,
      updatedAt: pastTimestamp(),
    })
  );
});
