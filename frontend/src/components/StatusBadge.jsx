import { Badge } from "react-bootstrap";

const VARIANTS = {
  ACTIVE: "success",
  INACTIVE: "secondary",
  REGULAR: "success",
  POSTPONED: "warning",
  TEMPORARY_WITHDRAWAL: "warning",
  GRADUATED: "primary",
  ELIMINATED: "danger",
  OPEN: "success",
  CLOSED: "secondary",
  CURRENT: "success",
  NOT_CURRENT: "secondary",
  APPROVED: "success",
  FAILED: "danger",
};

export default function StatusBadge({ status }) {
  if (!status) return <span className="text-muted">-</span>;
  const variant = VARIANTS[status] ?? "secondary";
  return (
    <Badge bg={variant} text={variant === "warning" ? "dark" : undefined}>
      {status.replaceAll("_", " ")}
    </Badge>
  );
}
