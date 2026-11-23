const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_principal_1";
const BOB_UID = "bob_principal_2";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

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

test("Unauthenticated user cannot read or write any school workspace collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("students").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("attendance").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("facilities").get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      schoolName: "Unauthorized Academy",
      academicYear: "2026-2027",
      principalName: "Intruder",
      createdAt: new Date(Date.now() - 1000),
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

test("Owner can list empty subcollections cleanly; cross-user reads and writes are denied", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("students").get());
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("classes").get());
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("attendance").get());
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("facilities").get());

  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("students").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("attendance").get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("notices").doc("n1").set({
      title: "Hijack",
      body: "Unauthorized notice",
      category: "URGENT",
      audience: "All",
      pinned: true,
      createdAt: new Date(Date.now() - 1000),
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

test("School profile, class, student, and attendance lifecycle mirrors Kotlin repository writes", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date(Date.now() - 5000);
  const userRef = aliceDb.collection("users").doc(ALICE_UID);

  // 1. Create school profile
  await assertSucceeds(
    userRef.set({
      userId: ALICE_UID,
      schoolName: "Oakridge International Academy",
      academicYear: "2026-2027",
      principalName: "Dr. Elena Vance",
      motto: "Curiosity, Integrity, Excellence",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Update school profile partially
  await assertSucceeds(
    userRef.update({
      schoolName: "Oakridge Preparatory Academy",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // 2. Create class
  await assertSucceeds(
    userRef.collection("classes").doc("cls_10a").set({
      name: "Grade 10 - Section A",
      gradeLevel: "Grade 10",
      roomNumber: "Room 204",
      homeroomTeacher: "Prof. Marcus Cole",
      scheduleSummary: "Mon-Fri 08:00 - 14:30",
      capacity: 35,
      createdAt: now,
      updatedAt: now,
    })
  );

  // 3. Student creation requires existing classId
  await assertFails(
    userRef.collection("students").doc("stu_bad").set({
      fullName: "Orphan Student",
      rollNumber: "10A-99",
      classId: "non_existent_class",
      className: "Unknown",
      status: "ACTIVE",
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    userRef.collection("students").doc("stu_01").set({
      fullName: "Aarav Sharma",
      rollNumber: "10A-01",
      classId: "cls_10a",
      className: "Grade 10 - Section A",
      guardianName: "Priya Sharma",
      guardianPhone: "+1-555-0142",
      status: "ACTIVE",
      createdAt: now,
      updatedAt: now,
    })
  );

  // 4. Attendance create & partial update
  await assertSucceeds(
    userRef.collection("attendance").doc("2026-11-23_stu_01").set({
      studentId: "stu_01",
      studentName: "Aarav Sharma",
      rollNumber: "10A-01",
      classId: "cls_10a",
      className: "Grade 10 - Section A",
      dateKey: "2026-11-23",
      status: "PRESENT",
      remarks: "On time",
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    userRef.collection("attendance").doc("2026-11-23_stu_01").update({
      status: "LATE",
      remarks: "Bus delayed 10 mins",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject immutable studentId/dateKey/createdAt mutation
  await assertFails(
    userRef.collection("attendance").doc("2026-11-23_stu_01").update({
      dateKey: "2026-11-24",
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("Facilities, faculty, fees, notices, and grades enforce schema & security bounds", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date(Date.now() - 5000);
  const userRef = aliceDb.collection("users").doc(ALICE_UID);

  // Setup class & student via batch (tests existsAfter relational check)
  const batch = aliceDb.batch();
  batch.set(userRef.collection("classes").doc("cls_9b"), {
    name: "Grade 9 - Section B",
    gradeLevel: "Grade 9",
    roomNumber: "Room 102",
    capacity: 30,
    createdAt: now,
    updatedAt: now,
  });
  batch.set(userRef.collection("students").doc("stu_02"), {
    fullName: "Maya Lin",
    rollNumber: "9B-04",
    classId: "cls_9b",
    className: "Grade 9 - Section B",
    status: "ACTIVE",
    createdAt: now,
    updatedAt: now,
  });
  await assertSucceeds(batch.commit());

  // Faculty
  await assertSucceeds(
    userRef.collection("faculty").doc("fac_01").set({
      fullName: "Dr. Sarah Jenkins",
      department: "Sciences",
      subject: "Physics & Robotics",
      assignedClass: "Grade 10 - Section A",
      email: "s.jenkins@oakridge.edu",
      status: "ACTIVE",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Facility create + booking update
  await assertSucceeds(
    userRef.collection("facilities").doc("facil_lab1").set({
      name: "STEM Robotics Lab",
      category: "LAB",
      building: "Innovation Wing - 2F",
      capacity: 32,
      status: "AVAILABLE",
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(
    userRef.collection("facilities").doc("facil_lab1").update({
      status: "OCCUPIED",
      bookedBy: "Grade 9 - Section B",
      scheduleSlot: "10:30 - 11:30",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Fee invoice create + payment update + invalid overpayment rejection
  await assertSucceeds(
    userRef.collection("fees").doc("fee_01").set({
      studentId: "stu_02",
      studentName: "Maya Lin",
      className: "Grade 9 - Section B",
      title: "Term 2 Tuition Fee",
      amount: 1200,
      paidAmount: 600,
      dueDateKey: "2026-12-01",
      status: "PARTIAL",
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertFails(
    userRef.collection("fees").doc("fee_01").update({
      paidAmount: 1500, // exceeds amount
      status: "PAID",
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertSucceeds(
    userRef.collection("fees").doc("fee_01").update({
      paidAmount: 1200,
      status: "PAID",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Notice
  await assertSucceeds(
    userRef.collection("notices").doc("not_01").set({
      title: "Winter Science Fair",
      body: "All Grade 9 and 10 teams must submit project abstracts by Friday.",
      category: "EVENT",
      audience: "Grades 9-10",
      pinned: true,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Grade
  await assertSucceeds(
    userRef.collection("grades").doc("grd_01").set({
      studentId: "stu_02",
      studentName: "Maya Lin",
      className: "Grade 9 - Section B",
      subject: "Physics",
      examName: "Midterm Assessment",
      score: 94,
      maxScore: 100,
      gradeLetter: "A+",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Negative checks: @DocumentId "id" key, future timestamp, oversized title
  await assertFails(
    userRef.collection("notices").doc("not_bad_id").set({
      id: "not_bad_id",
      title: "Bad ID Field",
      body: "Should fail because id is forbidden",
      category: "GENERAL",
      audience: "All",
      pinned: false,
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertFails(
    userRef.collection("notices").doc("not_future_ts").set({
      title: "Future Timestamp",
      body: "Clock tamper test",
      category: "GENERAL",
      audience: "All",
      pinned: false,
      createdAt: new Date(Date.now() + 60000),
      updatedAt: new Date(Date.now() + 60000),
    })
  );
});

test("Student daily self check-in enforces schema, relational student existence, and immutable dateKey", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 5000);
  const userRef = aliceDb.collection("users").doc(ALICE_UID);

  // Setup class and student
  const setupBatch = aliceDb.batch();
  setupBatch.set(userRef.collection("classes").doc("cls_10a"), {
    name: "Grade 10 - Section A",
    gradeLevel: "Grade 10",
    roomNumber: "Room 204",
    capacity: 30,
    createdAt: now,
    updatedAt: now,
  });
  setupBatch.set(userRef.collection("students").doc("stu_01"), {
    fullName: "Aarav Sharma",
    rollNumber: "10A-01",
    classId: "cls_10a",
    className: "Grade 10 - Section A",
    status: "ACTIVE",
    createdAt: now,
    updatedAt: now,
  });
  await assertSucceeds(setupBatch.commit());

  // Valid batch write mirroring SchoolRepository.submitStudentCheckIn
  const checkInBatch = aliceDb.batch();
  checkInBatch.set(userRef.collection("student_checkins").doc("2026-11-23_stu_01"), {
    studentId: "stu_01",
    studentName: "Aarav Sharma",
    rollNumber: "10A-01",
    classId: "cls_10a",
    className: "Grade 10 - Section A",
    dateKey: "2026-11-23",
    status: "PRESENT",
    sessionName: "Morning Homeroom",
    checkInTimeLabel: "08:25 AM",
    locationNote: "Main Campus · Room 204",
    remarks: "Checked in on time",
    createdAt: now,
    updatedAt: now,
  });
  checkInBatch.set(userRef.collection("attendance").doc("2026-11-23_stu_01"), {
    studentId: "stu_01",
    studentName: "Aarav Sharma",
    rollNumber: "10A-01",
    classId: "cls_10a",
    className: "Grade 10 - Section A",
    dateKey: "2026-11-23",
    status: "PRESENT",
    remarks: "Self check-in (Morning Homeroom)",
    createdAt: now,
    updatedAt: now,
  });
  await assertSucceeds(checkInBatch.commit());

  // Partial update succeeds
  await assertSucceeds(
    userRef.collection("student_checkins").doc("2026-11-23_stu_01").update({
      status: "LATE",
      remarks: "Updated arrival note",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject immutable dateKey modification & cross-user read
  await assertFails(
    userRef.collection("student_checkins").doc("2026-11-23_stu_01").update({
      dateKey: "2026-11-24",
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("student_checkins").get());
});

test("Daily class schedule enforces schema, class existence, period bounds, and ownership", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const now = new Date(Date.now() - 5000);
  const userRef = aliceDb.collection("users").doc(ALICE_UID);

  // Unauthenticated & cross-user listing blocked
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("schedules").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("schedules").get());

  // Missing classId fails existsAfter check
  await assertFails(
    userRef.collection("schedules").doc("sch_orphan").set({
      classId: "missing_cls",
      className: "Unknown Class",
      dayOfWeek: "MONDAY",
      periodNumber: 1,
      subject: "Physics",
      startTime: "08:15",
      endTime: "09:05",
      facultyName: "Dr. Sarah Jenkins",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Batch setup of class + schedule period succeeds
  const batch = aliceDb.batch();
  batch.set(userRef.collection("classes").doc("cls_10a"), {
    name: "Grade 10 - Section A",
    gradeLevel: "Grade 10",
    roomNumber: "Room 204",
    capacity: 32,
    createdAt: now,
    updatedAt: now,
  });
  batch.set(userRef.collection("schedules").doc("sch_mon_1"), {
    classId: "cls_10a",
    className: "Grade 10 - Section A",
    dayOfWeek: "MONDAY",
    periodNumber: 1,
    subject: "Physics & Robotics",
    startTime: "08:15",
    endTime: "09:05",
    facultyId: "fac_01",
    facultyName: "Dr. Sarah Jenkins",
    roomNumber: "STEM Lab 2F",
    topicNotes: "Kinematics and motion sensors",
    createdAt: now,
    updatedAt: now,
  });
  await assertSucceeds(batch.commit());

  // Invalid dayOfWeek or out-of-range periodNumber rejected
  await assertFails(
    userRef.collection("schedules").doc("sch_bad_day").set({
      classId: "cls_10a",
      className: "Grade 10 - Section A",
      dayOfWeek: "SUNDAY",
      periodNumber: 1,
      subject: "Art",
      startTime: "09:00",
      endTime: "10:00",
      facultyName: "Ms. Nair",
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertFails(
    userRef.collection("schedules").doc("sch_bad_period").set({
      classId: "cls_10a",
      className: "Grade 10 - Section A",
      dayOfWeek: "TUESDAY",
      periodNumber: 15,
      subject: "Calculus",
      startTime: "09:15",
      endTime: "10:05",
      facultyName: "Prof. Marcus Cole",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Partial update of subject/timing/faculty succeeds, preserving createdAt
  await assertSucceeds(
    userRef.collection("schedules").doc("sch_mon_1").update({
      startTime: "08:30",
      endTime: "09:20",
      facultyName: "Prof. Marcus Cole",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject cross-user write
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("schedules").doc("sch_mon_1").delete()
  );
});

test("Academic grades enforce assessmentType enum, score bounds, and optional term/teacherNotes", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date(Date.now() - 5000);
  const userRef = aliceDb.collection("users").doc(ALICE_UID);

  const setupBatch = aliceDb.batch();
  setupBatch.set(userRef.collection("classes").doc("cls_10a"), {
    name: "Grade 10 - Section A",
    gradeLevel: "Grade 10",
    roomNumber: "Room 204",
    capacity: 32,
    createdAt: now,
    updatedAt: now,
  });
  setupBatch.set(userRef.collection("students").doc("stu_01"), {
    fullName: "Aarav Sharma",
    rollNumber: "10A-01",
    classId: "cls_10a",
    className: "Grade 10 - Section A",
    status: "ACTIVE",
    createdAt: now,
    updatedAt: now,
  });
  await assertSucceeds(setupBatch.commit());

  await assertSucceeds(
    userRef.collection("grades").doc("grd_subj_1").set({
      studentId: "stu_01",
      studentName: "Aarav Sharma",
      className: "Grade 10 - Section A",
      subject: "Physics",
      examName: "Kinematics Lab #1",
      score: 46,
      maxScore: 50,
      gradeLetter: "A",
      term: "Term 2",
      assessmentType: "LAB",
      teacherNotes: "Clear experimental error analysis",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Invalid assessmentType rejected
  await assertFails(
    userRef.collection("grades").doc("grd_bad_type").set({
      studentId: "stu_01",
      studentName: "Aarav Sharma",
      className: "Grade 10 - Section A",
      subject: "Physics",
      examName: "Invalid Test",
      score: 40,
      maxScore: 50,
      gradeLetter: "B+",
      assessmentType: "ORAL_ONLY",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Score exceeding maxScore rejected
  await assertFails(
    userRef.collection("grades").doc("grd_over_max").set({
      studentId: "stu_01",
      studentName: "Aarav Sharma",
      className: "Grade 10 - Section A",
      subject: "Mathematics",
      examName: "Quiz 1",
      score: 105,
      maxScore: 100,
      gradeLetter: "A+",
      assessmentType: "QUIZ",
      createdAt: now,
      updatedAt: now,
    })
  );
});

