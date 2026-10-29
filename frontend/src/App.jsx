import { Route, Routes } from "react-router-dom";
import ProtectedRoute from "./auth/ProtectedRoute";
import Layout from "./components/Layout";
import { ROLES } from "./constants/roles";

import Home from "./pages/Home";
import Forbidden from "./pages/Forbidden";
import NotFound from "./pages/NotFound";

import CareersPage from "./pages/admin/CareersPage";
import StudyPlansPage from "./pages/admin/StudyPlansPage";
import StudyPlanSubjectsPage from "./pages/admin/StudyPlanSubjectsPage";
import StudentsPage from "./pages/admin/StudentsPage";
import TeachersPage from "./pages/admin/TeachersPage";
import PeriodsPage from "./pages/admin/PeriodsPage";
import SectionsPage from "./pages/admin/SectionsPage";
import MatriculationsPage from "./pages/admin/MatriculationsPage";
import PeriodEnrollmentsPage from "./pages/admin/PeriodEnrollmentsPage";

import MySectionsPage from "./pages/teacher/MySectionsPage";
import SectionStudentsPage from "./pages/teacher/SectionStudentsPage";

import ProfilePage from "./pages/student/ProfilePage";
import OfferPage from "./pages/student/OfferPage";
import MyEnrollmentsPage from "./pages/student/MyEnrollmentsPage";
import HistoryPage from "./pages/student/HistoryPage";

export default function App() {
  return (
    <Routes>
      <Route element={<Layout />}>
        <Route index element={<Home />} />

        {/* ADMIN */}
        <Route element={<ProtectedRoute roles={[ROLES.ADMIN]} />}>
          <Route path="admin/careers" element={<CareersPage />} />
          <Route path="admin/study-plans" element={<StudyPlansPage />} />
          <Route path="admin/study-plans/:planId/subjects" element={<StudyPlanSubjectsPage />} />
          <Route path="admin/students" element={<StudentsPage />} />
          <Route path="admin/teachers" element={<TeachersPage />} />
          <Route path="admin/periods" element={<PeriodsPage />} />
          <Route path="admin/periods/:periodId/sections" element={<SectionsPage />} />
          <Route path="admin/periods/:periodId/matriculations" element={<MatriculationsPage />} />
          <Route path="admin/periods/:periodId/enrollments" element={<PeriodEnrollmentsPage />} />
        </Route>

        {/* TEACHER */}
        <Route element={<ProtectedRoute roles={[ROLES.TEACHER]} />}>
          <Route path="teacher/sections" element={<MySectionsPage />} />
          <Route path="teacher/sections/:sectionId/students" element={<SectionStudentsPage />} />
        </Route>

        {/* STUDENT */}
        <Route element={<ProtectedRoute roles={[ROLES.STUDENT]} />}>
          <Route path="student/profile" element={<ProfilePage />} />
          <Route path="student/offer" element={<OfferPage />} />
          <Route path="student/enrollments" element={<MyEnrollmentsPage />} />
          <Route path="student/history" element={<HistoryPage />} />
        </Route>

        <Route path="forbidden" element={<Forbidden />} />
        <Route path="*" element={<NotFound />} />
      </Route>
    </Routes>
  );
}
