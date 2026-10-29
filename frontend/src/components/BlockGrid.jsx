import { Form, Table } from "react-bootstrap";
import { CLASS_DAYS, MODULES } from "../constants/enums";

// Weekly grid to pick schedule blocks. value is an array of codes such as ["L2", "W3"].
export default function BlockGrid({ value, onChange, disabled = false }) {
  const toggle = (code) =>
    onChange(value.includes(code) ? value.filter((c) => c !== code) : [...value, code]);

  return (
    <Table bordered size="sm" className="text-center align-middle mb-2">
      <thead>
        <tr>
          <th>Module</th>
          {CLASS_DAYS.map((d) => (
            <th key={d.code}>
              {d.label}
              <div className="small text-muted fw-normal">{d.code}</div>
            </th>
          ))}
        </tr>
      </thead>
      <tbody>
        {MODULES.map((m) => (
          <tr key={m.number}>
            <th className="small text-nowrap text-start">
              {m.number} <span className="text-muted fw-normal">{m.label}</span>
            </th>
            {CLASS_DAYS.map((d) => {
              const code = `${d.code}${m.number}`;
              return (
                <td key={code}>
                  <Form.Check
                    type="checkbox"
                    className="d-inline-block"
                    aria-label={code}
                    checked={value.includes(code)}
                    disabled={disabled}
                    onChange={() => toggle(code)}
                  />
                </td>
              );
            })}
          </tr>
        ))}
      </tbody>
    </Table>
  );
}
