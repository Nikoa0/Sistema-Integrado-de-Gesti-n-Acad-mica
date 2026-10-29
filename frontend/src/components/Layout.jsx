import { Container } from "react-bootstrap";
import { Outlet } from "react-router-dom";
import AppNavbar from "./AppNavbar";

export default function Layout() {
  return (
    <>
      <AppNavbar />
      <Container className="pb-5">
        <Outlet />
      </Container>
    </>
  );
}
