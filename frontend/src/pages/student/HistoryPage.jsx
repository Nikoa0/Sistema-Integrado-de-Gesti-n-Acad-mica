import { Alert, Table } from "react-bootstrap";
import { getEnrollmentsByStudent } from "../../api/enrollments";
import { useAuth } from "../../auth/useAuth";
import PageHeader from "../../components/PageHeader";
import QueryState from "../../components/QueryState";
import StatusBadge from "../../components/StatusBadge";
import useFetch from "../../hooks/useFetch";

export default function HistoryPage() {
  const { studentId } = useAuth();
  const { data, loading, error } = useFetch(
    () => (studentId ? getEnrollmentsByStudent(studentId) : Promise.resolve([])),
    [studentId]
  );
  const enrollments = data ?? [];

  const periodNames = [...new Set(enrollments.map((e) => e.periodName))].sort().reverse();
  const approved = enrollments.filter((e) => e.result === "APPROVED").length;
  const failed = enrollments.filter((e) => e.result === "FAILED").length;

  return (
    <>
      <PageHeader title="Academic history" subtitle="Subjects taken, grades and results" />
      {!studentId && <Alert variant="warning">No student record is linked to your user.</Alert>}
      <QueryState loading={loading} error={error}>
        {enrollments.length === 0 ? (
          <p className="text-muted">You have no enrollments yet.</p>
        ) : (
          <>
            <p>
              Approved: <strong>{approved}</strong> - Failed: <strong>{failed}</strong>
            </p>
            {periodNames.map((name) => (
              <div key={name} className="mb-4">
                <h5>Period {name}</h5>
                <Table striped responsive size="sm" className="align-middle">
                  <thead>
                    <tr>
                      <th>Subject</th>
                      <th>Grade</th>
                      <th>Result</th>
                    </tr>
                  </thead>
                  <tbody>
                    {enrollments
                      .filter((e) => e.periodName === name)
                      .map((e) => (
                        <tr key={e.id}>
                          <td>
                            {e.subjectCode} - {e.subjectName}
                          </td>
                          <td>{e.grade ?? "-"}</td>
                          <td>{e.result ? <StatusBadge status={e.result} /> : <span className="text-muted">Pending</span>}</td>
                        </tr>
                      ))}
                  </tbody>
                </Table>
              </div>
            ))}
          </>
        )}
      </QueryState>
    </>
  );
}
