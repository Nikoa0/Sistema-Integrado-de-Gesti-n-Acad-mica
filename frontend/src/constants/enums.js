export const STUDENT_STATUSES = ["REGULAR", "POSTPONED", "TEMPORARY_WITHDRAWAL", "GRADUATED", "ELIMINATED"];
// GRADUATED and ELIMINATED are assigned automatically when a period is closed
export const MANUAL_STUDENT_STATUSES = ["REGULAR", "POSTPONED", "TEMPORARY_WITHDRAWAL"];
export const TEACHER_STATUSES = ["ACTIVE", "INACTIVE"];
export const CAREER_STATUSES = ["ACTIVE", "INACTIVE"];
export const PERIOD_STATUSES = ["OPEN", "CLOSED"];

export const CLASS_DAYS = [
  { code: "L", label: "Monday" },
  { code: "M", label: "Tuesday" },
  { code: "W", label: "Wednesday" },
  { code: "J", label: "Thursday" },
  { code: "V", label: "Friday" },
];

export const MODULES = [
  { number: 1, label: "08:15 - 09:35" },
  { number: 2, label: "09:50 - 11:10" },
  { number: 3, label: "11:25 - 12:45" },
  { number: 4, label: "13:45 - 15:05" },
  { number: 5, label: "15:20 - 16:40" },
  { number: 6, label: "16:55 - 18:15" },
];

export const PASSING_GRADE = 4.0;
