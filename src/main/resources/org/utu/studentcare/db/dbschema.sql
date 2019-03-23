BEGIN TRANSACTION;
DROP TABLE IF EXISTS "courses";
CREATE TABLE IF NOT EXISTS "courses" (
	"name"	TEXT NOT NULL UNIQUE,
	"shortName"	TEXT NOT NULL,
	"description"	TEXT,
	"credits"	INTEGER NOT NULL,
	PRIMARY KEY("shortName")
);
DROP TABLE IF EXISTS "programs";
CREATE TABLE IF NOT EXISTS "programs" (
	"id"	TEXT NOT NULL,
	"name"	TEXT NOT NULL,
	PRIMARY KEY("id")
);
DROP TABLE IF EXISTS "courseinstances";
CREATE TABLE IF NOT EXISTS "courseinstances" (
	"instanceid"	TEXT NOT NULL,
	"courseId"	TEXT NOT NULL,
	"gradingRule"	TEXT,
	PRIMARY KEY("instanceid"),
	FOREIGN KEY("courseId") REFERENCES "courses"("shortName") on delete cascade
);
DROP TABLE IF EXISTS "personnel";
CREATE TABLE IF NOT EXISTS "personnel" (
	"firstNames"	TEXT NOT NULL,
	"familyName"	TEXT NOT NULL,
	"program"	TEXT NOT NULL,
	"id"	INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
	"idString"	TEXT NOT NULL UNIQUE,
	"password"	TEXT,
	"username"	TEXT NOT NULL UNIQUE,
	"isTeacher"	INTEGER NOT NULL,
	"isAdmin"	INTEGER NOT NULL,
	"isStudent"	INTEGER NOT NULL,
	FOREIGN KEY("program") REFERENCES "programs"("id") on delete cascade
);
DROP TABLE IF EXISTS "exercises";
CREATE TABLE IF NOT EXISTS "exercises" (
	"instanceId"	TEXT NOT NULL,
	"studentId"	INTEGER NOT NULL,
	"exerciseId"	TEXT NOT NULL,
	"uploadResource"	TEXT NOT NULL,
	"uploadDate"	TEXT NOT NULL,
	"teacherId"	INTEGER,
	"comment"	TEXT,
	"teacherComment"	TEXT,
	"grade"	REAL,
	"gradeDate"	TEXT,
	FOREIGN KEY("instanceId") REFERENCES "courseinstances"("instanceid"),
	FOREIGN KEY("studentId") REFERENCES "personnel"("id"),
	PRIMARY KEY("exerciseId","instanceId","studentId")
);
DROP TABLE IF EXISTS "coursegrades";
CREATE TABLE IF NOT EXISTS "coursegrades" (
	"studentId"	INTEGER NOT NULL,
	"instanceId"	TEXT NOT NULL,
	"gradedate"	TEXT NOT NULL,
	"grade"	INTEGER NOT NULL,
	"teacherId"	INTEGER NOT NULL,
	"adminId"	INTEGER,
	"adminDate"	TEXT,
	FOREIGN KEY("studentId") REFERENCES "personnel"("id") on delete cascade,
	FOREIGN KEY("instanceId") REFERENCES "courseinstances"("instanceid") on delete cascade,
	PRIMARY KEY("studentId","instanceId","gradedate")
);
DROP TABLE IF EXISTS "courseteachers";
CREATE TABLE IF NOT EXISTS "courseteachers" (
	"instanceId"	TEXT NOT NULL,
	"teacherId"	INTEGER NOT NULL,
	PRIMARY KEY("teacherId","instanceId"),
	FOREIGN KEY("instanceId") REFERENCES "courseinstances"("instanceid")
);
DROP TABLE IF EXISTS "coursestudents";
CREATE TABLE IF NOT EXISTS "coursestudents" (
	"studentId"	INTEGER NOT NULL,
	"instanceId"	TEXT NOT NULL,
	FOREIGN KEY("instanceId") REFERENCES "courseinstances"("instanceid") on delete cascade,
	PRIMARY KEY("studentId","instanceId"),
	FOREIGN KEY("studentId") REFERENCES "personnel"("id") on delete cascade
);
COMMIT;
