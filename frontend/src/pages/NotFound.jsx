import { Alert } from "react-bootstrap";
import { Link } from "react-router-dom";

export default function NotFound() {
  return (
    <Alert variant="secondary">
      <Alert.Heading>Page not found</Alert.Heading>
      <Link to="/">Back to the start</Link>
    </Alert>
  );
}
