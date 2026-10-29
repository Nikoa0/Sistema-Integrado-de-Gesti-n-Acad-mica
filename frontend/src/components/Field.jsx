import { Form } from "react-bootstrap";

export default function Field({ label, hint, children }) {
  return (
    <Form.Group className="mb-3">
      <Form.Label>{label}</Form.Label>
      {children}
      {hint && <Form.Text muted>{hint}</Form.Text>}
    </Form.Group>
  );
}
