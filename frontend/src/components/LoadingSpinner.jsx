import { Spinner } from "react-bootstrap";

export default function LoadingSpinner({ label = "Loading..." }) {
  return (
    <div className="d-flex justify-content-center align-items-center py-5">
      <Spinner animation="border" role="status" aria-label={label} />
    </div>
  );
}
