import { useState } from "react";
import { Button, Form, Table } from "react-bootstrap";
import { Link, useParams } from "react-router-dom";
import { getPeriod } from "../../api/academicPeriods";
import { getEnrollmentsByPeriod } from "../../api/enrollments";
import PageHeader from "../../components/PageHeader";
import QueryState from "../../components/QueryState";
import StatusBadge from "../../components/StatusBadge";
import useFetch from "../../hooks/useFetch";

async function loadAll(periodId) {
  const [period, enrollments] = await Promise.all([getPeriod(periodId), getEnrollmentsByPeriod(periodId)]);
  return { period, enrollments };
}

export default function PeriodEnrollmentsPage() {
  const { periodId } = useParams();
  const { data, loading, error } = useFetch(() => loadAll(periodId), [periodId]);
  const [filter, setFilter] = useState("");

  const text = filter.trim().toLowerCase();
  const rows = (data?.enrollments ?? []).filter(
    (e) =>
      !text ||
      e.studentName.toLowerCase().includes(text) ||
      e.subjectCode.toLowerCase().includes(text) ||
      e.subjectName.toLowerCase().includes(text)
  );

  return (
    <>
      <PageHeader
        title={data ? `Enrollments - ${data.period.name}` : "Enrollments"}
        subtitle="Enrollments of a period"
        actions={
          <Button as={Link} to="/admin/periods" variant="outline-secondary">
            Back
          </Button>
        }
      />
      <Form.Control
        className="mb-3"
        style={{ maxWidth: 320 }}
        placeholder="Filter by student or subject"
        value={filter}
        onChange={(e) => setFilter(e.target.value)}
      />
      <QueryState loading={loading} error={error}>
        {rows.length === 0 ? (
          <p className="text-muted">No enrollments found.</p>
        ) : (
          <Table striped hover responsive size="sm" className="align-middle">
            <thead>
              <tr>
                <th>Student</th>
                <th>Subject</th>
                <th>Enrolled on</th>
                <th>Grade</th>
                <th>Result</th>
              </tr>
            </thead>
            <tbody>
              {rows.map((e) => (
                <tr key={e.id}>
                  <td>{e.studentName}</td>
                  <td>
                    {e.subjectCode} - {e.subjectName}
                  </td>
                  <td>{e.enrolledAt}</td>
                  <td>{e.grade ?? "-"}</td>
                  <td>
                    <StatusBadge status={e.result} />
                  </td>
                </tr>
              ))}
            </tbody>
          </Table>
        )}
      </QueryState>
    </>
  );
}
