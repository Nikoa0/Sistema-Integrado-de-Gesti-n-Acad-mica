import { useState } from "react";
import { Alert, Button, Form, Table } from "react-bootstrap";
import { cancelEnrollment, enroll, getEnrollmentsByStudent, getOffer } from "../../api/enrollments";
import { useAuth } from "../../auth/useAuth";
import ConfirmModal from "../../components/ConfirmModal";
import ErrorAlert from "../../components/ErrorAlert";
import PageHeader from "../../components/PageHeader";
import PeriodSelect from "../../components/PeriodSelect";
import QueryState from "../../components/QueryState";
import StatusBadge from "../../components/StatusBadge";
import useAction from "../../hooks/useAction";
import useFetch from "../../hooks/useFetch";
import usePeriods from "../../hooks/usePeriods";

export default function MyEnrollmentsPage() {
  const { studentId } = useAuth();
  const periodsState = usePeriods();
  const { periods, selected, selectedId, setSelectedId } = periodsState;
  const isOpen = selected?.status === "OPEN";

  const offerQuery = useFetch(
    () => (studentId && selectedId ? getOffer(studentId, selectedId) : Promise.resolve([])),
    [studentId, selectedId]
  );
  const mineQuery = useFetch(
    () => (studentId && selectedId ? getEnrollmentsByStudent(studentId, selectedId) : Promise.resolve([])),
    [studentId, selectedId]
  );

  const action = useAction();
  const [picked, setPicked] = useState([]);
  const [toCancel, setToCancel] = useState(null);
  const [success, setSuccess] = useState(null);

  const mine = mineQuery.data ?? [];
  const enrolledSubjectIds = new Set(mine.map((e) => e.subjectId));
  const available = (offerQuery.data ?? [])
    .filter((s) => !enrolledSubjectIds.has(s.subjectId))
    .sort((a, b) => a.subjectCode.localeCompare(b.subjectCode));

  const changePeriod = (id) => {
    setSelectedId(id);
    setPicked([]);
    setSuccess(null);
    action.clearError();
  };

  const toggle = (subjectId) =>
    setPicked((list) => (list.includes(subjectId) ? list.filter((id) => id !== subjectId) : [...list, subjectId]));

  const submit = async () => {
    setSuccess(null);
    const created = await action.run(() => enroll(studentId, selectedId, picked));
    if (created) {
      setPicked([]);
      setSuccess(`${created.length} subject(s) enrolled.`);
      mineQuery.reload();
    }
  };

  const confirmCancel = async () => {
    setSuccess(null);
    await action.run(() => cancelEnrollment(toCancel.id));
    setToCancel(null);
    mineQuery.reload();
  };

  if (!studentId) {
    return (
      <>
        <PageHeader title="Enrollments" subtitle="Enroll subjects and manage your enrollments" />
        <Alert variant="warning">No student record is linked to your user.</Alert>
      </>
    );
  }

  return (
    <>
      <PageHeader title="Enrollments" subtitle="Enroll subjects and manage your enrollments" />
      <ErrorAlert message={action.error} onClose={action.clearError} />
      {success && (
        <Alert variant="success" dismissible onClose={() => setSuccess(null)}>
          {success}
        </Alert>
      )}

      <QueryState loading={periodsState.loading} error={periodsState.error}>
        {periods.length === 0 ? (
          <p className="text-muted">There are no academic periods yet.</p>
        ) : (
          <>
            <div className="d-flex align-items-center gap-3 mb-3">
              <PeriodSelect periods={periods} value={selectedId} onChange={changePeriod} />
              {selected && <StatusBadge status={selected.status} />}
            </div>
            {!isOpen && (
              <Alert variant="secondary">Enrollments and cancellations are only allowed while the period is OPEN.</Alert>
            )}

            <h5>My enrollments in this period</h5>
            <QueryState loading={mineQuery.loading} error={mineQuery.error}>
              {mine.length === 0 ? (
                <p className="text-muted">You have not enrolled any subject in this period.</p>
              ) : (
                <Table striped responsive size="sm" className="align-middle">
                  <thead>
                    <tr>
                      <th>Subject</th>
                      <th>Enrolled on</th>
                      <th>Grade</th>
                      <th />
                    </tr>
                  </thead>
                  <tbody>
                    {mine.map((e) => (
                      <tr key={e.id}>
                        <td>
                          {e.subjectCode} - {e.subjectName}
                        </td>
                        <td>{e.enrolledAt}</td>
                        <td>{e.grade ?? "-"}</td>
                        <td className="text-end">
                          <Button size="sm" variant="outline-danger" disabled={!isOpen} onClick={() => setToCancel(e)}>
                            Cancel enrollment
                          </Button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </Table>
              )}
            </QueryState>

            <h5 className="mt-4">Subjects available to enroll</h5>
            <p className="text-muted small">
              You must have between 3 and 6 subjects per period, approved prerequisites, no schedule conflicts and a seat
              in the section. The server checks every rule and explains any rejection.
            </p>
            <QueryState loading={offerQuery.loading} error={offerQuery.error}>
              {available.length === 0 ? (
                <p className="text-muted">There are no more subjects to enroll in this period.</p>
              ) : (
                <>
                  <Table striped hover responsive size="sm" className="align-middle">
                    <thead>
                      <tr>
                        <th />
                        <th>Subject</th>
                        <th>Teacher</th>
                        <th>Schedule</th>
                        <th>Capacity</th>
                      </tr>
                    </thead>
                    <tbody>
                      {available.map((s) => (
                        <tr key={s.id}>
                          <td>
                            <Form.Check
                              type="checkbox"
                              aria-label={`Select ${s.subjectCode}`}
                              disabled={!isOpen}
                              checked={picked.includes(s.subjectId)}
                              onChange={() => toggle(s.subjectId)}
                            />
                          </td>
                          <td>
                            {s.subjectCode} - {s.subjectName}
                          </td>
                          <td>{s.teacherName}</td>
                          <td>{s.blocks.join(", ")}</td>
                          <td>{s.capacity}</td>
                        </tr>
                      ))}
                    </tbody>
                  </Table>
                  <Button disabled={!isOpen || picked.length === 0 || action.busy} onClick={submit}>
                    Enroll selected ({picked.length})
                  </Button>
                </>
              )}
            </QueryState>
          </>
        )}
      </QueryState>

      <ConfirmModal
        show={Boolean(toCancel)}
        title="Cancel enrollment"
        message={`Cancel your enrollment in ${toCancel?.subjectCode} - ${toCancel?.subjectName}? Your seat will be released.`}
        confirmLabel="Cancel enrollment"
        onConfirm={confirmCancel}
        onCancel={() => setToCancel(null)}
      />
    </>
  );
}
