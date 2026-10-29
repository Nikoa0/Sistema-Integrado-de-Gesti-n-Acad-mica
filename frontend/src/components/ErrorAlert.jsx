import { Alert } from "react-bootstrap";

export default function ErrorAlert({ message, onClose }) {
  if (!message) return null;
  return (
    <Alert variant="danger" dismissible={Boolean(onClose)} onClose={onClose}>
      {message}
    </Alert>
  );
}
