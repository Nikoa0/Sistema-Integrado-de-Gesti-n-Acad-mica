import { useState } from "react";
import { Alert, Button, Form, Table } from "react-bootstrap";
import { Link, useParams } from "react-router-dom";
import { getPeriod } from "../../api/academicPeriods";
import { getEnrollmentsBySection } from "../../api/enrollments";
import { registerGrade } from "../../api/grades";
import { getSection } from "../../api/sections";
import ErrorAlert from "../../components/ErrorAlert";
import PageHeader from "../../components/PageHeader";
import QueryState from "../../components/QueryState";
import StatusBadge from "../../components/StatusBadge";
import { PASSING_GRADE } from "../../constants/enums";
import useAction from "../../hooks/useAction";
import useFetch from "../../hooks/useFetch";

async function loadAll(sectionId) {
  const section = await getSection(sectionId);
  const [period, enrollments] = await Promise.all([getPeriod(section.periodId), getEnrollmentsBySection(sectionId)]);
  return { section, period, enrollments };
}

export default function SectionStudentsPage() {
  const { sectionId } = useParams();
  const { data, loading, error, reload } = useFetch(() => loadAll(sectionId), [sectionId]);
  const section = data?.section;
  const enrollments = data?.enrollments ?? [];
  const isOpen = data?.period?.status === "OPEN";

  const action = useAction();
  const [drafts, setDrafts] = useState({});

  const save = async (enrollment) => {
    const result = await action.run(() => registerGrade(enrollment.id, Number(drafts[enrollment.id])));
    if (result) {
      setDrafts((d) => {
        const next = { ...d };
        delete next[enrollment.id];
        return next;
      });
      reload();
    }
  };

  return (
    <>
      <PageHeader
        title={section ? `${section.subjectCode} - ${section.subjectName}` : "Students of the section"}
        subtitle={section ? `Period ${section.periodName}. Enrolled students and final grades` : undefined}
        actions={
          <Button as={Link} to="/teacher/sections" variant="outline-secondary">
            Back
          </Button>
        }
      />
      <ErrorAlert message={action.error} onClose={action.clearError} />

      <QueryState loading={loading} error={error}>
        {!isOpen && (
          <Alert variant="secondary">The period is CLOSED, so grades can no longer be registered or modified.</Alert>
        )}
        {enrollments.length === 0 ? (
          <p className="text-muted">No students are enrolled in this section yet.</p>
        ) : (
          <Table striped hover responsive size="sm" className="align-middle">
            <thead>
              <tr>
                <th>Student</th>
                <th style={{ width: 160 }}>Final grade (1.0 - 7.0)</th>
                <th>Result</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {enrollments.map((e) => {
                const draft = drafts[e.id];
                const value = draft ?? e.grade ?? "";
                const number = Number(value);
                const valid = value !== "" && number >= 1 && number <= 7;
                const dirty = draft !== undefined && number !== e.grade;
                return (
                  <tr key={e.id}>
                    <td>{e.studentName}</td>
                    <td>
                      <Form.Control
                        size="sm"
                        type="number"
                        step="0.1"
                        min={1}
                        max={7}
                        disabled={!isOpen}
                        value={value}
                        isInvalid={draft !== undefined && !valid}
                        onChange={(ev) => setDrafts((d) => ({ ...d, [e.id]: ev.target.value }))}
                        aria-label={`Final grade of ${e.studentName}`}
                      />
                    </td>
                    <td>
                      {e.result ? <StatusBadge status={e.result} /> : <span className="text-muted">Pending</span>}
                      {e.grade !== null && e.grade !== undefined && (
                        <span className="text-muted small ms-2">(passing: {PASSING_GRADE.toFixed(1)})</span>
                      )}
                    </td>
                    <td className="text-end">
                      <Button size="sm" disabled={!isOpen || !valid || !dirty || action.busy} onClick={() => save(e)}>
                        Save grade
                      </Button>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </Table>
        )}
      </QueryState>
    </>
  );
}
