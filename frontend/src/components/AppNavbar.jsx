import { Container, Nav, Navbar, NavDropdown } from "react-bootstrap";
import { Link, NavLink } from "react-router-dom";
import { useAuth } from "../auth/useAuth";
import { NAV_ITEMS } from "../config/navigation";

export default function AppNavbar() {
  const { email, roles, logout } = useAuth();
  const items = roles.flatMap((role) => NAV_ITEMS[role] ?? []);

  return (
    <Navbar bg="dark" variant="dark" expand="lg" className="mb-4">
      <Container>
        <Navbar.Brand as={Link} to="/">
          SIGA IPT
        </Navbar.Brand>
        <Navbar.Toggle aria-controls="main-nav" />
        <Navbar.Collapse id="main-nav">
          <Nav className="me-auto">
            {items.map((item) => (
              <Nav.Link key={item.path} as={NavLink} to={item.path}>
                {item.label}
              </Nav.Link>
            ))}
          </Nav>
          <NavDropdown title={email} id="user-menu" align="end" menuVariant="dark">
            <NavDropdown.ItemText>Role: {roles.join(", ")}</NavDropdown.ItemText>
            <NavDropdown.Divider />
            <NavDropdown.Item onClick={logout}>Log out</NavDropdown.Item>
          </NavDropdown>
        </Navbar.Collapse>
      </Container>
    </Navbar>
  );
}
