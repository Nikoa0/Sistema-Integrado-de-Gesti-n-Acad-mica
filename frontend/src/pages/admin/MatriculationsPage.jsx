import { useState } from "react";
import { Button, Col, Form, Row, Table } from "react-bootstrap";
import { Link, useParams } from "react-router-dom";
import { getPeriod } from "../../api/academicPeriods";
import { createMatriculation, deleteMatriculation, getMatriculationsByPeriod } from "../../api/matriculations";
import { getStudents } from "../../api/students";
import ConfirmModal from "../../components/ConfirmModal";
import ErrorAlert from "../../components/ErrorAlert";
import PageHeader from "../../components/PageHeader";
import QueryState from "../../components/QueryState";
import useAction from "../../hooks/useAction";
import useFetch from "../../hooks/useFetch";

async function loadAll(periodId) {
  const [period, matriculations, students] = await Promise.all([
    getPeriod(periodId),
    getMatriculationsByPeriod(periodId),
    getStudents(),
  ]);
  return { period, matriculations, students };
}

export default function MatriculationsPage() {
  const { periodId } = useParams();
  const { data, loading, error, reload } = useFetch(() => loadAll(periodId), [periodId]);
  const matriculations = data?.matriculations ?? [];
  const students = data?.students ?? [];
  const period = data?.period;

  const action = useAction();
  const [studentId, setStudentId] = useState("");
  const [toDelete, setToDelete] = useState(null);

  const available = students.filter(
    (s) => s.status === "REGULAR" && !matriculations.some((m) => m.studentId === s.id)
  );

  const add = async (e) => {
    e.preventDefault();
    const created = await action.run(() => createMatriculation(periodId, Number(studentId)));
    if (created) {
      setStudentId("");
      reload();
    }
  };

  const confirmDelete = async () => {
    await action.run(() => deleteMatriculation(toDelete.id));
    setToDelete(null);
    reload();
  };

  return (
    <>
      <PageHeader
        title={period ? `Matriculations - ${period.name}` : "Matriculations"}
        subtitle="Students matriculated in a period"
        actions={
          <Button as={Link} to="/admin/periods" variant="outline-secondary">
            Back
          </Button>
        }
      />
      <ErrorAlert message={action.error} onClose={action.clearError} />

      <QueryState loading={loading} error={error}>
        <Form onSubmit={add} className="mb-4">
          <Row className="g-2" style={{ maxWidth: 700 }}>
            <Col sm={8}>
              <Form.Select
                required
                aria-label="Student"
                value={studentId}
                onChange={(e) => setStudentId(e.target.value)}
              >
                <option value="">Select a REGULAR student to matriculate</option>
                {available.map((s) => (
                  <option key={s.id} value={s.id}>
                    {s.run} - {s.fullName}
                  </option>
                ))}
              </Form.Select>
            </Col>
            <Col sm={4}>
              <Button type="submit" disabled={!studentId || action.busy}>
                Add matriculation
              </Button>
            </Col>
          </Row>
        </Form>

        {matriculations.length === 0 ? (
          <p className="text-muted">No students are matriculated in this period yet.</p>
        ) : (
          <Table striped hover responsive size="sm" className="align-middle">
            <thead>
              <tr>
                <th>Student</th>
                <th>Period</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {matriculations.map((m) => (
                <tr key={m.id}>
                  <td>{m.studentName}</td>
                  <td>{m.periodName}</td>
                  <td className="text-end">
                    <Button size="sm" variant="outline-danger" onClick={() => setToDelete(m)}>
                      Remove
                    </Button>
                  </td>
                </tr>
              ))}
            </tbody>
          </Table>
        )}
      </QueryState>

      <ConfirmModal
        show={Boolean(toDelete)}
        title="Remove matriculation"
        message={`Remove the matriculation of ${toDelete?.studentName}? It cannot be removed if the student already has enrollments in this period.`}
        confirmLabel="Remove"
        onConfirm={confirmDelete}
        onCancel={() => setToDelete(null)}
      />
    </>
  );
}
