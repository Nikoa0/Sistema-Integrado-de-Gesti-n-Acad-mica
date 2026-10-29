import { Navigate } from "react-router-dom";
import { useAuth } from "../auth/useAuth";
import { ROLES } from "../constants/roles";

// Sends each user to the first view of their role
export default function Home() {
  const { hasRole } = useAuth();
  if (hasRole(ROLES.ADMIN)) return <Navigate to="/admin/careers" replace />;
  if (hasRole(ROLES.TEACHER)) return <Navigate to="/teacher/sections" replace />;
  if (hasRole(ROLES.STUDENT)) return <Navigate to="/student/offer" replace />;
  return <Navigate to="/forbidden" replace />;
}
