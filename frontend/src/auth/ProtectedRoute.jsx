import { Navigate, Outlet } from "react-router-dom";
import { useAuth } from "./useAuth";

// Wraps a group of routes: <Route element={<ProtectedRoute roles={["ADMIN"]} />}> ... </Route>
// This only hides views; the real protection is done by Spring Security in the backend.
export default function ProtectedRoute({ roles }) {
  const { hasAnyRole } = useAuth();
  if (!hasAnyRole(roles)) {
    return <Navigate to="/forbidden" replace />;
  }
  return <Outlet />;
}
