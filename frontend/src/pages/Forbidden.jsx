import { Alert } from "react-bootstrap";
import { Link } from "react-router-dom";

export default function Forbidden() {
  return (
    <Alert variant="warning">
      <Alert.Heading>Access denied</Alert.Heading>
      <p className="mb-2">You do not have permission to see this page.</p>
      <Link to="/">Back to the start</Link>
    </Alert>
  );
}
