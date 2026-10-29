import { Button, Modal } from "react-bootstrap";

export default function ConfirmModal({
  show,
  title = "Confirm",
  message,
  confirmLabel = "Confirm",
  variant = "danger",
  onConfirm,
  onCancel,
}) {
  return (
    <Modal show={show} onHide={onCancel} centered>
      <Modal.Header closeButton>
        <Modal.Title>{title}</Modal.Title>
      </Modal.Header>
      <Modal.Body>{message}</Modal.Body>
      <Modal.Footer>
        <Button variant="secondary" onClick={onCancel}>
          Cancel
        </Button>
        <Button variant={variant} onClick={onConfirm}>
          {confirmLabel}
        </Button>
      </Modal.Footer>
    </Modal>
  );
}
