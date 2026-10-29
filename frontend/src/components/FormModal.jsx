import { Button, Form, Modal, Spinner } from "react-bootstrap";
import ErrorAlert from "./ErrorAlert";

export default function FormModal({
  show,
  title,
  onHide,
  onSubmit,
  saving = false,
  error,
  submitLabel = "Save",
  size,
  children,
}) {
  return (
    <Modal show={show} onHide={saving ? undefined : onHide} centered size={size}>
      <Form onSubmit={onSubmit}>
        <Modal.Header closeButton>
          <Modal.Title>{title}</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          <ErrorAlert message={error} />
          {children}
        </Modal.Body>
        <Modal.Footer>
          <Button variant="secondary" onClick={onHide} disabled={saving}>
            Cancel
          </Button>
          <Button type="submit" disabled={saving}>
            {saving ? <Spinner size="sm" animation="border" /> : submitLabel}
          </Button>
        </Modal.Footer>
      </Form>
    </Modal>
  );
}
