import { useEffect, useMemo, useState } from "react";
import { Alert, Button, Container } from "react-bootstrap";
import { getMe } from "../api/me";
import LoadingSpinner from "../components/LoadingSpinner";
import { getErrorMessage } from "../utils/errors";
import { AuthContext } from "./AuthContext";
import keycloak from "./keycloak";

// keycloak.init() can only run once, and React StrictMode runs effects twice in dev,
// so the promise is kept at module level.
let initPromise = null;
function initKeycloak() {
  if (!initPromise) {
    initPromise = keycloak.init({
      onLoad: "login-required",
      pkceMethod: "S256",
      checkLoginIframe: false,
    });
  }
  return initPromise;
}

export function AuthProvider({ children }) {
  const [me, setMe] = useState(null);
  const [error, setError] = useState(null);

  useEffect(() => {
    let cancelled = false;
    initKeycloak()
      .then(() => getMe())
      .then((data) => {
        if (!cancelled) setMe(data);
      })
      .catch((err) => {
        if (!cancelled) setError(getErrorMessage(err));
      });
    return () => {
      cancelled = true;
    };
  }, []);

  const value = useMemo(() => {
    if (!me) return null;
    const roles = me.roles ?? [];
    return {
      me,
      roles,
      email: me.email,
      studentId: me.studentId,
      teacherId: me.teacherId,
      hasRole: (role) => roles.includes(role),
      hasAnyRole: (allowed) => !allowed || allowed.some((role) => roles.includes(role)),
      logout: () => keycloak.logout({ redirectUri: window.location.origin }),
    };
  }, [me]);

  if (error) {
    return (
      <Container className="py-5">
        <Alert variant="danger">
          <Alert.Heading>Could not start the application</Alert.Heading>
          <p className="mb-3">{error}</p>
          <Button variant="outline-danger" onClick={() => keycloak.logout({ redirectUri: window.location.origin })}>
            Log out
          </Button>
        </Alert>
      </Container>
    );
  }

  if (!value) {
    return <LoadingSpinner />;
  }

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
