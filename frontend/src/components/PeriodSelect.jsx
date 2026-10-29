import { Form } from "react-bootstrap";

export default function PeriodSelect({ periods, value, onChange }) {
  return (
    <Form.Select
      aria-label="Academic period"
      style={{ maxWidth: 280 }}
      value={value ?? ""}
      onChange={(e) => onChange(Number(e.target.value))}
    >
      {periods.map((p) => (
        <option key={p.id} value={p.id}>
          {p.name} ({p.status})
        </option>
      ))}
    </Form.Select>
  );
}
