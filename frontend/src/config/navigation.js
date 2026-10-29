import { ROLES } from "../constants/roles";

export const NAV_ITEMS = {
  [ROLES.ADMIN]: [
    { path: "/admin/careers", label: "Careers" },
    { path: "/admin/study-plans", label: "Study plans" },
    { path: "/admin/students", label: "Students" },
    { path: "/admin/teachers", label: "Teachers" },
    { path: "/admin/periods", label: "Academic periods" },
  ],
  [ROLES.TEACHER]: [{ path: "/teacher/sections", label: "My sections" }],
  [ROLES.STUDENT]: [
    { path: "/student/profile", label: "My profile" },
    { path: "/student/offer", label: "Offer" },
    { path: "/student/enrollments", label: "Enrollments" },
    { path: "/student/history", label: "History" },
  ],
};
